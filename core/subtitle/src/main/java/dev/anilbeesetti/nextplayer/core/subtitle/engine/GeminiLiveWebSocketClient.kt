package dev.anilbeesetti.nextplayer.core.subtitle.engine

import dev.anilbeesetti.nextplayer.core.common.Logger
import dev.anilbeesetti.nextplayer.core.subtitle.di.SubtitleScope
import dev.anilbeesetti.nextplayer.core.subtitle.model.GeminiLiveError
import dev.anilbeesetti.nextplayer.core.subtitle.model.GeminiLiveTranscriptEvent
import dev.anilbeesetti.nextplayer.core.subtitle.model.GeminiLiveTranscriptEventType
import dev.anilbeesetti.nextplayer.core.subtitle.model.GeminiSessionConfig
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
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class GeminiLiveWebSocketClient @Inject constructor(
    @param:SubtitleScope private val okHttpClient: OkHttpClient,
    @param:SubtitleScope private val scope: CoroutineScope,
    private val transcriptParser: GeminiLiveTranscriptParser,
) {
    companion object {
        private const val TAG = "GeminiLiveWebSocketClient"
        private const val WS_URL =
            "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent"
        private const val VALIDATION_TIMEOUT_MS = 2_000L
    }

    interface Listener {
        fun onTranscriptEvents(events: List<GeminiLiveTranscriptEvent>)
        fun onStatusChange(status: SubtitleEngineStatus)
        fun onError(error: GeminiLiveError)
    }

    private var listener: Listener? = null
    private var webSocket: WebSocket? = null
    private var isManualStop = false
    private var validationJob: Job? = null

    private val _status = MutableStateFlow(SubtitleEngineStatus.IDLE)
    val status: StateFlow<SubtitleEngineStatus> = _status.asStateFlow()

    fun setListener(listener: Listener) {
        this.listener = listener
    }

    fun connect(config: GeminiSessionConfig) {
        disconnect()
        isManualStop = false
        _status.value = SubtitleEngineStatus.CONNECTING
        listener?.onStatusChange(SubtitleEngineStatus.CONNECTING)

        val request = Request.Builder().url(webSocketUrl(config.apiKey)).build()
        webSocket = okHttpClient.newWebSocket(request, createWebSocketListener(config))
    }

    fun disconnect() {
        isManualStop = true
        validationJob?.cancel()
        validationJob = null
        webSocket?.close(1000, "Client stopped")
        webSocket = null
        _status.value = SubtitleEngineStatus.STOPPED
        listener?.onStatusChange(SubtitleEngineStatus.STOPPED)
    }

    fun sendAudio(pcmData: ByteArray) {
        webSocket?.send(GeminiLiveMessageBuilder.buildRealtimeAudioMessage(pcmData))
    }

    suspend fun validateApiKey(apiKey: String): String? = suspendCancellableCoroutine { cont ->
        var validationSocket: WebSocket? = null
        var settled = false

        fun settle(result: String?) {
            if (!settled) {
                settled = true
                validationSocket?.close(1000, null)
                cont.resume(result)
            }
        }

        val config = GeminiSessionConfig(apiKey = apiKey)
        val request = Request.Builder().url(webSocketUrl(apiKey)).build()
        validationSocket = okHttpClient.newWebSocket(
            request,
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    webSocket.send(GeminiLiveMessageBuilder.buildSetupMessage(config))
                    validationJob = scope.launch {
                        delay(VALIDATION_TIMEOUT_MS)
                        settle(null)
                    }
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    val error = parseError(text)
                    if (error != null) {
                        settle(error.message)
                    } else {
                        val events = runCatching { transcriptParser.parse(text) }.getOrDefault(emptyList())
                        if (events.any { it.eventType == GeminiLiveTranscriptEventType.STATUS }) {
                            settle(null)
                        }
                    }
                }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    settle(GeminiLiveErrorMapper.fromThrowable(t).message)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    if (code == 1008) {
                        settle("Invalid Google API key or missing Live API permission")
                    }
                }
            },
        )

        cont.invokeOnCancellation {
            validationSocket.close(1000, "Cancelled")
        }
    }

    private fun createWebSocketListener(config: GeminiSessionConfig): WebSocketListener {
        return object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Logger.logDebug(
                    TAG,
                    SubtitleDiagnosticLogger.metadata(
                        "event" to "opened",
                        "provider" to "Gemini Live",
                        "targetLanguage" to config.targetLanguageCode,
                        "model" to config.model,
                    ),
                )
                webSocket.send(GeminiLiveMessageBuilder.buildSetupMessage(config))
                _status.value = SubtitleEngineStatus.ACTIVE
                listener?.onStatusChange(SubtitleEngineStatus.ACTIVE)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val events = transcriptParser.parse(text)
                    if (events.isNotEmpty()) {
                        listener?.onTranscriptEvents(events)
                    }
                } catch (e: Exception) {
                    listener?.onError(GeminiLiveErrorMapper.fromProviderError(null, "Unable to parse Gemini Live response"))
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Logger.logDebug(TAG, "WebSocket closing code=$code")
                webSocket.close(code, reason)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Logger.logDebug(TAG, "WebSocket closed code=$code")
                this@GeminiLiveWebSocketClient.webSocket = null
                _status.value = if (isManualStop) SubtitleEngineStatus.STOPPED else SubtitleEngineStatus.GEMINI_ERROR
                listener?.onStatusChange(_status.value)
                if (!isManualStop && code == 1008) {
                    listener?.onError(GeminiLiveErrorMapper.fromProviderError("401", "API key not valid"))
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Logger.logError(TAG, "WebSocket failure", t)
                this@GeminiLiveWebSocketClient.webSocket = null
                _status.value = SubtitleEngineStatus.GEMINI_ERROR
                listener?.onStatusChange(SubtitleEngineStatus.GEMINI_ERROR)
                listener?.onError(GeminiLiveErrorMapper.fromThrowable(t))
            }
        }
    }

    private fun parseError(text: String): GeminiLiveError? {
        return runCatching {
            val events = transcriptParser.parse(text)
            val error = events.firstOrNull { it.eventType == GeminiLiveTranscriptEventType.ERROR }
            error?.let { GeminiLiveErrorMapper.fromProviderError(it.errorCode, it.text) }
        }.getOrNull()
    }

    private fun webSocketUrl(apiKey: String): String {
        val encodedKey = URLEncoder.encode(apiKey, Charsets.UTF_8.name())
        return "$WS_URL?key=$encodedKey"
    }
}
