package dev.anilbeesetti.nextplayer.core.subtitle.engine

import dev.anilbeesetti.nextplayer.core.common.Logger
import dev.anilbeesetti.nextplayer.core.subtitle.di.SubtitleScope
import dev.anilbeesetti.nextplayer.core.subtitle.model.SonioxSessionConfig
import dev.anilbeesetti.nextplayer.core.subtitle.model.SubtitleEngineStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.toByteString
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class SonioxWebSocketClient @Inject constructor(
    @param:SubtitleScope private val okHttpClient: OkHttpClient,
    @param:SubtitleScope private val scope: CoroutineScope,
) {
    companion object {
        private const val TAG = "SonioxWebSocketClient"
        private const val WS_URL = "wss://stt-rt.soniox.com/transcribe-websocket"
        private const val KEEPALIVE_INTERVAL_MS = 15_000L
        private const val INITIAL_BACKOFF_MS = 1_000L
        private const val MAX_BACKOFF_MS = 30_000L

        // Error close codes
        private const val CLOSE_INVALID_KEY = 4001
        private const val CLOSE_INVALID_KEY_ALT = 4003
        private const val CLOSE_SUBSCRIPTION = 4002
        private const val CLOSE_RATE_LIMIT = 4029
    }

    interface Listener {
        fun onTokensReceived(tokens: JSONArray)
        fun onStatusChange(status: SubtitleEngineStatus)
        fun onError(message: String, isRecoverable: Boolean)
    }

    private enum class ConnectMode {
        INITIAL,
        ROTATE,
        RECONNECT,
    }

    private data class ManagedSocket(
        val id: Long,
        val config: SonioxSessionConfig,
        val webSocket: WebSocket,
    )

    private var config: SonioxSessionConfig? = null
    private var listener: Listener? = null
    private var keepaliveJob: Job? = null
    private var reconnectJob: Job? = null
    private var reconnectAttempt = 0
    private var isManualStop = false
    private var nextSocketId = 0L
    private var activeSocket: ManagedSocket? = null
    private val sockets = mutableMapOf<Long, WebSocket>()
    private val drainingSocketIds = mutableSetOf<Long>()
    private val manualCloseSocketIds = mutableSetOf<Long>()

    private val _status = MutableStateFlow(SubtitleEngineStatus.IDLE)
    val status: StateFlow<SubtitleEngineStatus> = _status.asStateFlow()

    fun setListener(listener: Listener) {
        this.listener = listener
    }

    fun connect(config: SonioxSessionConfig) {
        this.config = config
        isManualStop = false
        reconnectAttempt = 0
        reconnectJob?.cancel()
        closeAllSockets(reason = "Replacing connection")
        openSocket(config = config, mode = ConnectMode.INITIAL)
    }

    fun disconnect() {
        isManualStop = true
        reconnectJob?.cancel()
        reconnectJob = null
        keepaliveJob?.cancel()
        keepaliveJob = null
        closeAllSockets(reason = "User stopped")
        _status.value = SubtitleEngineStatus.STOPPED
        listener?.onStatusChange(SubtitleEngineStatus.STOPPED)
    }

    fun sendAudio(pcmData: ByteArray) {
        if (_status.value != SubtitleEngineStatus.ACTIVE) return
        activeSocket?.webSocket?.send(pcmData.toByteString(0, pcmData.size))
    }

    fun rotateSession(config: SonioxSessionConfig) {
        this.config = config
        isManualStop = false
        reconnectAttempt = 0
        reconnectJob?.cancel()

        if (activeSocket == null) {
            openSocket(config = config, mode = ConnectMode.INITIAL)
            return
        }

        Logger.logDebug(TAG, "Rotating session with make-before-break reset")
        openSocket(config = config, mode = ConnectMode.ROTATE)
    }

    fun reconnectImmediately(config: SonioxSessionConfig) {
        this.config = config
        isManualStop = false
        reconnectAttempt = 0
        reconnectJob?.cancel()
        keepaliveJob?.cancel()
        keepaliveJob = null
        closeAllSockets(reason = "Playback reset")
        openSocket(config = config, mode = ConnectMode.INITIAL)
    }

    /**
     * Quick validation: connect and immediately disconnect.
     * Returns null nếu API key hợp lệ, trả về message lỗi nếu không.
     */
    suspend fun validateApiKey(apiKey: String): String? = suspendCancellableCoroutine { cont ->
        val testConfig = SonioxSessionConfig(apiKey = apiKey)
        var ws: WebSocket? = null
        var settled = false

        fun settle(result: String?) {
            if (!settled) {
                settled = true
                ws?.close(1000, null)
                cont.resume(result)
            }
        }

        val request = Request.Builder().url(WS_URL).build()
        ws = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                webSocket.send(buildConfigJson(testConfig))
                // Chờ phản hồi lỗi trong 2 giây; nếu không có lỗi = key hợp lệ
                scope.launch {
                    delay(2000)
                    settle(null)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                settle("Connection failed: ${t.message}")
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                val error = when (code) {
                    CLOSE_INVALID_KEY, CLOSE_INVALID_KEY_ALT -> "Invalid API key"
                    CLOSE_SUBSCRIPTION -> "Account/subscription issue"
                    CLOSE_RATE_LIMIT -> "Rate limited"
                    else -> null
                }
                if (error != null) settle(error)
            }
        })

        cont.invokeOnCancellation {
            ws?.close(1000, "Cancelled")
        }
    }

    private fun openSocket(config: SonioxSessionConfig, mode: ConnectMode) {
        val status = when (mode) {
            ConnectMode.INITIAL -> SubtitleEngineStatus.CONNECTING
            ConnectMode.RECONNECT -> SubtitleEngineStatus.RECONNECTING
            ConnectMode.ROTATE -> null
        }

        status?.let {
            _status.value = it
            listener?.onStatusChange(it)
        }

        val socketId = ++nextSocketId
        val request = Request.Builder().url(WS_URL).build()
        val webSocket = okHttpClient.newWebSocket(
            request,
            createWebSocketListener(socketId = socketId, socketConfig = config, mode = mode),
        )
        sockets[socketId] = webSocket
    }

    private fun createWebSocketListener(
        socketId: Long,
        socketConfig: SonioxSessionConfig,
        mode: ConnectMode,
    ): WebSocketListener {
        return object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Logger.logDebug(TAG, "WebSocket opened (id=$socketId, mode=$mode)")
                val configJson = buildConfigJson(socketConfig)
                webSocket.send(configJson)

                reconnectJob?.cancel()
                reconnectJob = null
                _status.value = SubtitleEngineStatus.ACTIVE
                listener?.onStatusChange(SubtitleEngineStatus.ACTIVE)
                reconnectAttempt = 0

                if (mode == ConnectMode.ROTATE) {
                    val previousSocket = activeSocket
                    previousSocket?.let {
                        drainingSocketIds.add(it.id)
                        manualCloseSocketIds.add(it.id)
                    }
                    activeSocket = ManagedSocket(socketId, socketConfig, webSocket)
                    previousSocket?.let {
                        it.webSocket.close(1000, "Session reset")
                    }
                } else {
                    activeSocket = ManagedSocket(socketId, socketConfig, webSocket)
                }

                startKeepalive()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (!shouldProcessMessages(socketId)) {
                    return
                }

                try {
                    val json = JSONObject(text)
                    val tokens = json.optJSONArray("tokens")
                    if (tokens != null && tokens.length() > 0) {
                        listener?.onTokensReceived(tokens)
                    }
                } catch (e: Exception) {
                    Logger.logError(TAG, "Failed to parse message: $text", e)
                }
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                // Binary messages not expected from Soniox
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Logger.logDebug(TAG, "WebSocket closing: code=$code reason=$reason")
                webSocket.close(code, reason)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Logger.logDebug(TAG, "WebSocket closed: id=$socketId code=$code reason=$reason")
                val wasActive = activeSocket?.id == socketId
                val wasManualClose = manualCloseSocketIds.remove(socketId)

                sockets.remove(socketId)
                drainingSocketIds.remove(socketId)
                if (wasActive) {
                    keepaliveJob?.cancel()
                    keepaliveJob = null
                    activeSocket = null
                }

                if (wasManualClose || !wasActive) {
                    return
                }

                when (code) {
                    CLOSE_INVALID_KEY, CLOSE_INVALID_KEY_ALT -> {
                        _status.value = SubtitleEngineStatus.ERROR
                        listener?.onError("Invalid API key", false)
                        listener?.onStatusChange(SubtitleEngineStatus.ERROR)
                    }
                    CLOSE_SUBSCRIPTION -> {
                        _status.value = SubtitleEngineStatus.ERROR
                        listener?.onError("Account/subscription issue", false)
                        listener?.onStatusChange(SubtitleEngineStatus.ERROR)
                    }
                    CLOSE_RATE_LIMIT -> {
                        _status.value = SubtitleEngineStatus.ERROR
                        listener?.onError("Rate limited, try again later", true)
                        listener?.onStatusChange(SubtitleEngineStatus.ERROR)
                    }
                    else -> {
                        if (!isManualStop) {
                            scheduleReconnect()
                        }
                    }
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Logger.logError(TAG, "WebSocket failure (id=$socketId)", t)
                val wasActive = activeSocket?.id == socketId
                val wasManualClose = manualCloseSocketIds.remove(socketId)

                sockets.remove(socketId)
                drainingSocketIds.remove(socketId)
                if (wasActive) {
                    keepaliveJob?.cancel()
                    keepaliveJob = null
                    activeSocket = null
                }

                if (!wasManualClose && wasActive && !isManualStop) {
                    listener?.onError("Connection lost: ${t.message}", true)
                    scheduleReconnect()
                }
            }
        }
    }

    private fun buildConfigJson(config: SonioxSessionConfig): String {
        val json = JSONObject().apply {
            put("api_key", config.apiKey)
            put("model", config.model)
            put("audio_format", config.audioFormat)
            put("sample_rate", config.sampleRate)
            put("num_channels", config.numChannels)
            put("enable_endpoint_detection", true)
            put("max_endpoint_delay_ms", config.endpointDelayMs)
            put("enable_speaker_diarization", true)
            put("enable_language_identification", true)

            val translation = JSONObject().apply {
                put("type", "one_way")
                put("target_language", config.targetLanguage)
            }
            put("translation", translation)

            if (config.sourceLanguage != null) {
                put("language_hints", JSONArray().apply { put(config.sourceLanguage) })
            }

            // Carryover context từ session trước để cải thiện độ chính xác
            if (!config.carryoverContext.isNullOrBlank()) {
                put("context", JSONObject().apply {
                    put("text", "Recent conversation: ${config.carryoverContext}")
                })
            }
        }
        return json.toString()
    }

    private fun startKeepalive() {
        keepaliveJob?.cancel()
        keepaliveJob = scope.launch {
            while (true) {
                delay(KEEPALIVE_INTERVAL_MS)
                val sent = activeSocket?.webSocket?.send("{\"type\":\"keepalive\"}") ?: false
                if (!sent) {
                    Logger.logError(TAG, "Failed to send keepalive")
                    break
                }
            }
        }
    }

    private fun scheduleReconnect() {
        val reconnectConfig = config ?: return

        reconnectJob?.cancel()
        _status.value = SubtitleEngineStatus.RECONNECTING
        listener?.onStatusChange(SubtitleEngineStatus.RECONNECTING)

        val backoff = (INITIAL_BACKOFF_MS * (1L shl reconnectAttempt.coerceAtMost(5)))
            .coerceAtMost(MAX_BACKOFF_MS)
        reconnectAttempt++

        Logger.logDebug(TAG, "Reconnecting in ${backoff}ms (attempt $reconnectAttempt)")
        reconnectJob = scope.launch {
            delay(backoff)
            if (!isManualStop && activeSocket == null) {
                openSocket(config = reconnectConfig, mode = ConnectMode.RECONNECT)
            }
        }
    }

    private fun shouldProcessMessages(socketId: Long): Boolean {
        return activeSocket?.id == socketId || socketId in drainingSocketIds
    }

    private fun closeAllSockets(reason: String) {
        if (sockets.isEmpty()) {
            activeSocket = null
            drainingSocketIds.clear()
            return
        }

        manualCloseSocketIds.addAll(sockets.keys)
        sockets.values.forEach { it.close(1000, reason) }
        activeSocket = null
        drainingSocketIds.clear()
    }
}
