package dev.anilbeesetti.nextplayer.core.subtitle.engine

import dev.anilbeesetti.nextplayer.core.common.Logger
import dev.anilbeesetti.nextplayer.core.data.repository.PreferencesRepository
import dev.anilbeesetti.nextplayer.core.model.PlayerPreferences
import dev.anilbeesetti.nextplayer.core.subtitle.audio.AudioBatcher
import dev.anilbeesetti.nextplayer.core.subtitle.audio.SubtitleAudioProcessor
import dev.anilbeesetti.nextplayer.core.subtitle.di.SubtitleScope
import dev.anilbeesetti.nextplayer.core.subtitle.model.SubtitleEngineStatus
import dev.anilbeesetti.nextplayer.core.subtitle.model.SubtitleSegment
import dev.anilbeesetti.nextplayer.core.subtitle.session.SubtitleSessionManager
import dev.anilbeesetti.nextplayer.core.subtitle.storage.SecureApiKeyStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    private val audioBatcher: AudioBatcher,
    private val subtitleAudioProcessor: SubtitleAudioProcessor,
    private val preferencesRepository: PreferencesRepository,
    private val secureApiKeyStorage: SecureApiKeyStorage,
    @SubtitleScope private val scope: CoroutineScope,
) : SubtitleEngine {

    companion object {
        private const val TAG = "SubtitleEngine"
        private const val SESSION_DURATION_MS = 3 * 60 * 1000L // 3 phút
        private const val CONFIGURE_API_KEY_MESSAGE = "Configure Soniox API key in Settings > Subtitle"
    }

    private var currentConfig: dev.anilbeesetti.nextplayer.core.subtitle.model.SonioxSessionConfig? = null
    private var sessionResetJob: Job? = null

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
        val config = dev.anilbeesetti.nextplayer.core.subtitle.model.SonioxSessionConfig(
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
        startSessionResetTimer()
        Logger.logDebug(TAG, "Engine started with target=${config.targetLanguage}")
        return SubtitleStartResult.Started
    }

    override fun stop() {
        sessionResetJob?.cancel()
        sessionResetJob = null
        subtitleAudioProcessor.setEnabled(false)
        webSocketClient.disconnect()
        audioBatcher.reset()
        sessionManager.clearDisplay()
        currentConfig = null
        Logger.logDebug(TAG, "Engine stopped")
    }

    override fun resetSession() {
        Logger.logDebug(TAG, "Resetting session (make-before-break với carryover context)")
        val carryover = sessionManager.getCarryoverContext()
        val newConfig = currentConfig?.copy(carryoverContext = carryover.ifBlank { null })
        sessionManager.clearDisplay()
        if (newConfig != null) {
            currentConfig = newConfig
            webSocketClient.connect(newConfig) // make-before-break: connect trước khi đóng cũ
        } else {
            webSocketClient.resetConnection()
        }
        startSessionResetTimer() // Khởi động lại timer
    }

    private fun startSessionResetTimer() {
        sessionResetJob?.cancel()
        sessionResetJob = scope.launch {
            delay(SESSION_DURATION_MS)
            Logger.logDebug(TAG, "Session timeout — auto-resetting")
            resetSession()
        }
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
