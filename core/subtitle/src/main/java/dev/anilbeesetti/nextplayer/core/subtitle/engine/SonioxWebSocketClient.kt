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

@Singleton
class SonioxWebSocketClient @Inject constructor(
    @SubtitleScope private val okHttpClient: OkHttpClient,
    @SubtitleScope private val scope: CoroutineScope,
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

    private var webSocket: WebSocket? = null
    private var config: SonioxSessionConfig? = null
    private var listener: Listener? = null
    private var keepaliveJob: Job? = null
    private var reconnectAttempt = 0
    private var isManualStop = false

    private val _status = MutableStateFlow(SubtitleEngineStatus.IDLE)
    val status: StateFlow<SubtitleEngineStatus> = _status.asStateFlow()

    fun setListener(listener: Listener) {
        this.listener = listener
    }

    fun connect(config: SonioxSessionConfig) {
        this.config = config
        isManualStop = false
        reconnectAttempt = 0
        doConnect()
    }

    fun disconnect() {
        isManualStop = true
        keepaliveJob?.cancel()
        keepaliveJob = null
        webSocket?.close(1000, "User stopped")
        webSocket = null
        _status.value = SubtitleEngineStatus.STOPPED
        listener?.onStatusChange(SubtitleEngineStatus.STOPPED)
    }

    fun sendAudio(pcmData: ByteArray) {
        if (_status.value != SubtitleEngineStatus.ACTIVE) return
        webSocket?.send(pcmData.toByteString(0, pcmData.size))
    }

    fun resetConnection() {
        Logger.logDebug(TAG, "Resetting connection for session reset")
        keepaliveJob?.cancel()
        webSocket?.close(1000, "Session reset")
        webSocket = null
        reconnectAttempt = 0
        doConnect()
    }

    /**
     * Quick validation: connect and immediately disconnect.
     * Returns null on success, error message on failure.
     */
    suspend fun validateApiKey(apiKey: String): String? {
        val testConfig = SonioxSessionConfig(apiKey = apiKey)
        var result: String? = null
        val latch = java.util.concurrent.CountDownLatch(1)

        val request = Request.Builder().url(WS_URL).build()
        val ws = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                val configJson = buildConfigJson(testConfig)
                webSocket.send(configJson)
                // If we can open and send config, key is likely valid
                // Wait briefly for potential error response
                scope.launch {
                    delay(2000)
                    result = null
                    webSocket.close(1000, "Validation complete")
                    latch.countDown()
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                result = "Connection failed: ${t.message}"
                latch.countDown()
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                when (code) {
                    CLOSE_INVALID_KEY, CLOSE_INVALID_KEY_ALT -> result = "Invalid API key"
                    CLOSE_SUBSCRIPTION -> result = "Account/subscription issue"
                    CLOSE_RATE_LIMIT -> result = "Rate limited"
                }
                if (result != null) latch.countDown()
            }
        })

        try {
            latch.await(5, java.util.concurrent.TimeUnit.SECONDS)
        } catch (_: InterruptedException) {
            result = "Validation timeout"
        } finally {
            ws.close(1000, null)
        }
        return result
    }

    private fun doConnect() {
        _status.value = SubtitleEngineStatus.CONNECTING
        listener?.onStatusChange(SubtitleEngineStatus.CONNECTING)

        val request = Request.Builder().url(WS_URL).build()
        webSocket = okHttpClient.newWebSocket(request, createWebSocketListener())
    }

    private fun createWebSocketListener(): WebSocketListener {
        return object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Logger.logDebug(TAG, "WebSocket opened")
                val cfg = config ?: return
                val configJson = buildConfigJson(cfg)
                webSocket.send(configJson)

                _status.value = SubtitleEngineStatus.ACTIVE
                listener?.onStatusChange(SubtitleEngineStatus.ACTIVE)
                reconnectAttempt = 0
                startKeepalive()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
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
                Logger.logDebug(TAG, "WebSocket closed: code=$code reason=$reason")
                keepaliveJob?.cancel()

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
                Logger.logError(TAG, "WebSocket failure", t)
                keepaliveJob?.cancel()

                if (!isManualStop) {
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
        }
        return json.toString()
    }

    private fun startKeepalive() {
        keepaliveJob?.cancel()
        keepaliveJob = scope.launch {
            while (true) {
                delay(KEEPALIVE_INTERVAL_MS)
                val sent = webSocket?.send("{\"type\":\"keepalive\"}") ?: false
                if (!sent) {
                    Logger.logError(TAG, "Failed to send keepalive")
                    break
                }
            }
        }
    }

    private fun scheduleReconnect() {
        _status.value = SubtitleEngineStatus.RECONNECTING
        listener?.onStatusChange(SubtitleEngineStatus.RECONNECTING)

        val backoff = (INITIAL_BACKOFF_MS * (1L shl reconnectAttempt.coerceAtMost(5)))
            .coerceAtMost(MAX_BACKOFF_MS)
        reconnectAttempt++

        Logger.logDebug(TAG, "Reconnecting in ${backoff}ms (attempt $reconnectAttempt)")
        scope.launch {
            delay(backoff)
            if (!isManualStop) {
                doConnect()
            }
        }
    }
}
