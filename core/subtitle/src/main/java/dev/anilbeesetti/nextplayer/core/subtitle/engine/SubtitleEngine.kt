package dev.anilbeesetti.nextplayer.core.subtitle.engine

import dev.anilbeesetti.nextplayer.core.common.Logger
import dev.anilbeesetti.nextplayer.core.data.repository.PreferencesRepository
import dev.anilbeesetti.nextplayer.core.model.PlayerPreferences
import dev.anilbeesetti.nextplayer.core.subtitle.audio.AudioBatcher
import dev.anilbeesetti.nextplayer.core.subtitle.audio.SubtitleAudioProcessor
import dev.anilbeesetti.nextplayer.core.subtitle.di.SubtitleScope
import dev.anilbeesetti.nextplayer.core.subtitle.model.GeminiSessionConfig
import dev.anilbeesetti.nextplayer.core.subtitle.model.SonioxSessionConfig
import dev.anilbeesetti.nextplayer.core.subtitle.model.SubtitleEngineStatus
import dev.anilbeesetti.nextplayer.core.subtitle.model.SubtitleProvider
import dev.anilbeesetti.nextplayer.core.subtitle.model.SubtitleSegment
import dev.anilbeesetti.nextplayer.core.subtitle.session.SessionResetScheduler
import dev.anilbeesetti.nextplayer.core.subtitle.session.SubtitleSessionManager
import dev.anilbeesetti.nextplayer.core.subtitle.storage.SecureApiKeyStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton

sealed interface SubtitleStartResult {
    data object Started : SubtitleStartResult
    data class Failed(val message: String) : SubtitleStartResult
}

interface SubtitleEngine {
    suspend fun start(provider: SubtitleProvider? = null): SubtitleStartResult
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
    private val geminiProvider: GeminiSubtitleProvider,
    @param:SubtitleScope private val scope: CoroutineScope,
) : SubtitleEngine {

    companion object {
        private const val TAG = "SubtitleEngine"
        private const val CONFIGURE_API_KEY_MESSAGE = "Configure Soniox API key in Settings > Subtitle"
    }

    private var activeProvider: SubtitleProvider = SubtitleProvider.SONIOX
    private var currentConfig: SonioxSessionConfig? = null

    private val _status = MutableStateFlow(SubtitleEngineStatus.IDLE)
    override val status: StateFlow<SubtitleEngineStatus> = _status.asStateFlow()

    private val _displaySegments = MutableStateFlow<List<SubtitleSegment>>(emptyList())
    override val displaySegments: StateFlow<List<SubtitleSegment>> = _displaySegments.asStateFlow()

    private val _provisionalText = MutableStateFlow("")
    override val provisionalText: StateFlow<String> = _provisionalText.asStateFlow()

    private val _provisionalSpeaker = MutableStateFlow<String?>(null)
    override val provisionalSpeaker: StateFlow<String?> = _provisionalSpeaker.asStateFlow()

    init {
        setupCallbacks()
        observeProviderFlows()
    }

    override suspend fun start(provider: SubtitleProvider?): SubtitleStartResult {
        val preferences = preferencesRepository.playerPreferences.value
        val selectedProvider = provider ?: SubtitleProvider.fromPreference(preferences.liveSubtitleProvider)
        if (
            activeProvider == selectedProvider &&
            (_status.value == SubtitleEngineStatus.ACTIVE || _status.value == SubtitleEngineStatus.CONNECTING)
        ) {
            return SubtitleStartResult.Started
        }

        if (_status.value == SubtitleEngineStatus.ACTIVE || _status.value == SubtitleEngineStatus.CONNECTING) {
            stop()
        }

        return when (selectedProvider) {
            SubtitleProvider.SONIOX -> startSoniox(preferences)
            SubtitleProvider.GEMINI_LIVE -> startGemini()
        }
    }

    override fun stop() {
        sessionResetScheduler.stop()
        subtitleAudioProcessor.setEnabled(false)
        when (activeProvider) {
            SubtitleProvider.SONIOX -> {
                webSocketClient.disconnect()
                sessionManager.clearDisplay()
            }
            SubtitleProvider.GEMINI_LIVE -> geminiProvider.stop()
        }
        audioBatcher.reset()
        currentConfig = null
        _status.value = SubtitleEngineStatus.STOPPED
        Logger.logDebug(TAG, "Engine stopped provider=$activeProvider")
    }

    override fun resetSession() {
        when (activeProvider) {
            SubtitleProvider.SONIOX -> resetSonioxSession()
            SubtitleProvider.GEMINI_LIVE -> {
                Logger.logDebug(TAG, "Resetting Gemini Live session")
                geminiProvider.resetSession()
                sessionResetScheduler.start(::resetSession)
            }
        }
    }

    private suspend fun startSoniox(preferences: PlayerPreferences): SubtitleStartResult {
        val apiKey = secureApiKeyStorage.getApiKey(SubtitleProvider.SONIOX)?.trim().orEmpty()
        if (apiKey.isBlank()) {
            return failStart(CONFIGURE_API_KEY_MESSAGE)
        }

        val validationError = webSocketClient.validateApiKey(apiKey)
        if (validationError != null) {
            return failStart(validationError)
        }

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

        activeProvider = SubtitleProvider.SONIOX
        currentConfig = config
        sessionManager.reset()
        audioBatcher.reset()
        audioBatcher.setBatchDurationMs(AudioBatcher.DEFAULT_BATCH_DURATION_MS)
        audioBatcher.setListener(object : AudioBatcher.Listener {
            override fun onAudioBatchReady(pcmData: ByteArray) {
                webSocketClient.sendAudio(pcmData)
            }
        })
        subtitleAudioProcessor.setEnabled(true)
        webSocketClient.connect(config)
        sessionResetScheduler.start(::resetSession)
        Logger.logDebug(TAG, "Engine started provider=SONIOX target=${config.targetLanguage}")
        return SubtitleStartResult.Started
    }

    private suspend fun startGemini(): SubtitleStartResult {
        activeProvider = SubtitleProvider.GEMINI_LIVE
        audioBatcher.reset()
        audioBatcher.setBatchDurationMs(GeminiSessionConfig.CHUNK_DURATION_MS)
        audioBatcher.setListener(object : AudioBatcher.Listener {
            override fun onAudioBatchReady(pcmData: ByteArray) {
                geminiProvider.sendAudio(pcmData)
            }
        })
        subtitleAudioProcessor.setEnabled(true)

        val result = geminiProvider.start()
        if (result is SubtitleStartResult.Started) {
            sessionResetScheduler.start(::resetSession)
            Logger.logDebug(TAG, "Engine started provider=GEMINI_LIVE")
        } else {
            subtitleAudioProcessor.setEnabled(false)
            audioBatcher.reset()
        }
        return result
    }

    private fun resetSonioxSession() {
        val baseConfig = currentConfig ?: return
        Logger.logDebug(TAG, "Resetting Soniox session (make-before-break with carryover context)")
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

    private fun observeProviderFlows() {
        scope.launch {
            webSocketClient.status.collect { status ->
                if (activeProvider == SubtitleProvider.SONIOX) {
                    _status.value = status
                }
            }
        }
        scope.launch {
            sessionManager.displaySegments.collect { segments ->
                if (activeProvider == SubtitleProvider.SONIOX) {
                    _displaySegments.value = segments
                }
            }
        }
        scope.launch {
            sessionManager.provisionalText.collect { text ->
                if (activeProvider == SubtitleProvider.SONIOX) {
                    _provisionalText.value = text
                }
            }
        }
        scope.launch {
            sessionManager.provisionalSpeaker.collect { speaker ->
                if (activeProvider == SubtitleProvider.SONIOX) {
                    _provisionalSpeaker.value = speaker
                }
            }
        }
        scope.launch {
            geminiProvider.status.collect { status ->
                if (activeProvider == SubtitleProvider.GEMINI_LIVE) {
                    _status.value = status
                }
            }
        }
        scope.launch {
            geminiProvider.displaySegments.collect { segments ->
                if (activeProvider == SubtitleProvider.GEMINI_LIVE) {
                    _displaySegments.value = segments
                }
            }
        }
        scope.launch {
            geminiProvider.provisionalText.collect { text ->
                if (activeProvider == SubtitleProvider.GEMINI_LIVE) {
                    _provisionalText.value = text
                }
            }
        }
        scope.launch {
            geminiProvider.provisionalSpeaker.collect { speaker ->
                if (activeProvider == SubtitleProvider.GEMINI_LIVE) {
                    _provisionalSpeaker.value = speaker
                }
            }
        }
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
                Logger.logDebug(TAG, "Soniox status changed: $status")
            }

            override fun onError(message: String, isRecoverable: Boolean) {
                Logger.logError(TAG, "Soniox error recoverable=$isRecoverable")
            }
        })
    }
}
