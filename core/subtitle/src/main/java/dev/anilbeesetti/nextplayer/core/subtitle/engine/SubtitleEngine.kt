package dev.anilbeesetti.nextplayer.core.subtitle.engine

import dev.anilbeesetti.nextplayer.core.common.Logger
import dev.anilbeesetti.nextplayer.core.data.repository.PreferencesRepository
import dev.anilbeesetti.nextplayer.core.model.PlayerPreferences
import dev.anilbeesetti.nextplayer.core.subtitle.audio.AudioBatcher
import dev.anilbeesetti.nextplayer.core.subtitle.audio.SubtitleAudioProcessor
import dev.anilbeesetti.nextplayer.core.subtitle.di.SubtitleScope
import dev.anilbeesetti.nextplayer.core.subtitle.model.SonioxSessionConfig
import dev.anilbeesetti.nextplayer.core.subtitle.model.SubtitleEngineStatus
import dev.anilbeesetti.nextplayer.core.subtitle.model.SubtitleSegment
import dev.anilbeesetti.nextplayer.core.subtitle.session.SessionResetScheduler
import dev.anilbeesetti.nextplayer.core.subtitle.session.SubtitleSessionManager
import dev.anilbeesetti.nextplayer.core.subtitle.storage.SecureApiKeyStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton

sealed interface SubtitleStartResult {
    data object Started : SubtitleStartResult
    data class Failed(val message: String) : SubtitleStartResult
}

interface SubtitleEngine {
    suspend fun start(): SubtitleStartResult
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
    private val sessionResetScheduler: SessionResetScheduler,
    private val audioBatcher: AudioBatcher,
    private val subtitleAudioProcessor: SubtitleAudioProcessor,
    private val preferencesRepository: PreferencesRepository,
    private val secureApiKeyStorage: SecureApiKeyStorage,
    @param:SubtitleScope private val scope: CoroutineScope,
) : SubtitleEngine {

    companion object {
        private const val TAG = "SubtitleEngine"
        private const val CONFIGURE_API_KEY_MESSAGE = "Configure Soniox API key in Settings > Subtitle"
    }

    private var currentConfig: SonioxSessionConfig? = null

    override val status: StateFlow<SubtitleEngineStatus> = webSocketClient.status
    override val displaySegments: StateFlow<List<SubtitleSegment>> = sessionManager.displaySegments
    override val provisionalText: StateFlow<String> = sessionManager.provisionalText
    override val provisionalSpeaker: StateFlow<String?> = sessionManager.provisionalSpeaker

    init {
        setupCallbacks()
    }

    override suspend fun start(): SubtitleStartResult {
        if (status.value == SubtitleEngineStatus.ACTIVE || status.value == SubtitleEngineStatus.CONNECTING) {
            return SubtitleStartResult.Started
        }

        val apiKey = secureApiKeyStorage.getApiKey()?.trim().orEmpty()
        if (apiKey.isBlank()) {
            return failStart(CONFIGURE_API_KEY_MESSAGE)
        }

        val validationError = webSocketClient.validateApiKey(apiKey)
        if (validationError != null) {
            return failStart(validationError)
        }

        val preferences = preferencesRepository.playerPreferences.value
        val config = SonioxSessionConfig(
            apiKey = apiKey,
            targetLanguage = preferences.targetLanguage.ifBlank {
                PlayerPreferences.DEFAULT_LIVE_SUBTITLE_TARGET_LANGUAGE
            },
            sourceLanguage = preferences.sourceLanguage.takeUnless {
                it.isBlank() || it == PlayerPreferences.DEFAULT_LIVE_SUBTITLE_SOURCE_LANGUAGE
            },
            endpointDelayMs = preferences.endpointDelayMs,
        )

        currentConfig = config
        sessionManager.reset()
        audioBatcher.setListener(object : AudioBatcher.Listener {
            override fun onAudioBatchReady(pcmData: ByteArray) {
                webSocketClient.sendAudio(pcmData)
            }
        })
        subtitleAudioProcessor.setEnabled(true)
        webSocketClient.connect(config)
        sessionResetScheduler.start(::resetSession)
        Logger.logDebug(TAG, "Engine started with target=${config.targetLanguage}")
        return SubtitleStartResult.Started
    }

    override fun stop() {
        sessionResetScheduler.stop()
        subtitleAudioProcessor.setEnabled(false)
        webSocketClient.disconnect()
        audioBatcher.reset()
        sessionManager.clearDisplay()
        currentConfig = null
        Logger.logDebug(TAG, "Engine stopped")
    }

    override fun resetSession() {
        val baseConfig = currentConfig ?: return
        Logger.logDebug(TAG, "Resetting session (make-before-break với carryover context)")
        val carryover = sessionManager.getCarryoverContext().ifBlank { null }
        val newConfig = baseConfig.copy(carryoverContext = carryover)

        currentConfig = newConfig
        webSocketClient.rotateSession(newConfig)
        sessionResetScheduler.start(::resetSession)
    }

    private fun failStart(message: String): SubtitleStartResult {
        Logger.logError(TAG, message)
        return SubtitleStartResult.Failed(message)
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
