package dev.anilbeesetti.nextplayer.core.subtitle.engine

import dev.anilbeesetti.nextplayer.core.common.Logger
import dev.anilbeesetti.nextplayer.core.subtitle.audio.AudioBatcher
import dev.anilbeesetti.nextplayer.core.subtitle.model.SonioxSessionConfig
import dev.anilbeesetti.nextplayer.core.subtitle.model.SubtitleEngineStatus
import dev.anilbeesetti.nextplayer.core.subtitle.model.SubtitleSegment
import dev.anilbeesetti.nextplayer.core.subtitle.session.SubtitleSessionManager
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton

interface SubtitleEngine {
    fun start(config: SonioxSessionConfig)
    fun stop()
    fun resetSession()
    val status: StateFlow<SubtitleEngineStatus>
    val displaySegments: StateFlow<List<SubtitleSegment>>
    val provisionalText: StateFlow<String>
    val provisionalSpeaker: StateFlow<String?>
}

@Singleton
class SubtitleEngineImpl @Inject constructor(
    private val webSocketClient: SonioxWebSocketClient,
    private val tokenParser: SonioxTokenParser,
    private val sessionManager: SubtitleSessionManager,
    private val audioBatcher: AudioBatcher,
) : SubtitleEngine {

    companion object {
        private const val TAG = "SubtitleEngine"
    }

    private var currentConfig: SonioxSessionConfig? = null

    override val status: StateFlow<SubtitleEngineStatus> = webSocketClient.status
    override val displaySegments: StateFlow<List<SubtitleSegment>> = sessionManager.displaySegments
    override val provisionalText: StateFlow<String> = sessionManager.provisionalText
    override val provisionalSpeaker: StateFlow<String?> = sessionManager.provisionalSpeaker

    init {
        setupCallbacks()
    }

    override fun start(config: SonioxSessionConfig) {
        if (config.apiKey.isBlank()) {
            Logger.logError(TAG, "Cannot start: API key is empty")
            return
        }

        currentConfig = config
        sessionManager.reset()
        audioBatcher.setListener(object : AudioBatcher.Listener {
            override fun onAudioBatchReady(pcmData: ByteArray) {
                webSocketClient.sendAudio(pcmData)
            }
        })
        webSocketClient.connect(config)
        Logger.logDebug(TAG, "Engine started with target=${config.targetLanguage}")
    }

    override fun stop() {
        webSocketClient.disconnect()
        audioBatcher.reset()
        sessionManager.clearDisplay()
        currentConfig = null
        Logger.logDebug(TAG, "Engine stopped")
    }

    override fun resetSession() {
        Logger.logDebug(TAG, "Resetting session")
        sessionManager.clearDisplay()
        webSocketClient.resetConnection()
    }

    private fun setupCallbacks() {
        tokenParser.setCallback(object : SonioxTokenParser.Callback {
            override fun onOriginal(
                text: String,
                speaker: String?,
                language: String?,
                confidence: Float?,
            ) {
                sessionManager.onOriginal(text, speaker, language, confidence)
            }

            override fun onTranslation(text: String) {
                sessionManager.onTranslation(text)
            }

            override fun onProvisional(text: String, speaker: String?, language: String?) {
                sessionManager.onProvisional(text, speaker, language)
            }

            override fun onEndpointReached() {
                sessionManager.onEndpointReached()
            }
        })

        webSocketClient.setListener(object : SonioxWebSocketClient.Listener {
            override fun onTokensReceived(tokens: JSONArray) {
                tokenParser.parseTokens(tokens)
            }

            override fun onStatusChange(status: SubtitleEngineStatus) {
                Logger.logDebug(TAG, "Status changed: $status")
            }

            override fun onError(message: String, isRecoverable: Boolean) {
                Logger.logError(TAG, "Error: $message (recoverable=$isRecoverable)")
            }
        })
    }
}
