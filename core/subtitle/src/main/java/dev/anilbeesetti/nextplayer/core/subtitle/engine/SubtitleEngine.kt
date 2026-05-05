package dev.anilbeesetti.nextplayer.core.subtitle.engine

import dev.anilbeesetti.nextplayer.core.common.Logger
import dev.anilbeesetti.nextplayer.core.subtitle.audio.LookaheadAudioPipeline
import dev.anilbeesetti.nextplayer.core.data.repository.PreferencesRepository
import dev.anilbeesetti.nextplayer.core.model.PlayerPreferences
import dev.anilbeesetti.nextplayer.core.subtitle.audio.AudioBatcher
import dev.anilbeesetti.nextplayer.core.subtitle.audio.SubtitleAudioProcessor
import dev.anilbeesetti.nextplayer.core.subtitle.di.SubtitleScope
import dev.anilbeesetti.nextplayer.core.subtitle.model.LookaheadPipelineStatus
import dev.anilbeesetti.nextplayer.core.subtitle.model.LookaheadSessionState
import dev.anilbeesetti.nextplayer.core.subtitle.model.SonioxSessionConfig
import dev.anilbeesetti.nextplayer.core.subtitle.model.SubtitleEngineStatus
import dev.anilbeesetti.nextplayer.core.subtitle.model.SubtitleSegment
import dev.anilbeesetti.nextplayer.core.subtitle.session.SessionResetScheduler
import dev.anilbeesetti.nextplayer.core.subtitle.session.SubtitleSessionManager
import dev.anilbeesetti.nextplayer.core.subtitle.storage.SecureApiKeyStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
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
    fun onPlaybackPosition(positionMs: Long)
    fun onPlayWhenReadyChanged(isPlaying: Boolean)
    fun onSeek(positionMs: Long)
    fun onMediaItemChanged(mediaId: String, positionMs: Long)
    fun onAudioTrackChanged(trackKey: String?)
    fun onStop()
    val status: StateFlow<SubtitleEngineStatus>
    val displaySegments: StateFlow<List<SubtitleSegment>>
    val provisionalText: StateFlow<String>
    val provisionalSpeaker: StateFlow<String?>
    val lookaheadState: StateFlow<LookaheadSessionState>
}

@Singleton
class SubtitleEngineImpl @Inject constructor(
    private val webSocketClient: SonioxWebSocketClient,
    private val tokenParser: SonioxTokenParser,
    private val sessionManager: SubtitleSessionManager,
    private val sessionResetScheduler: SessionResetScheduler,
    private val audioBatcher: AudioBatcher,
    private val lookaheadAudioPipeline: LookaheadAudioPipeline,
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
    private var currentMediaId: String? = null
    private var currentAudioTrackKey: String? = null
    private var currentPlaybackPositionMs = 0L
    private var currentGenerationId = 0L
    private var isStarted = false

    override val status: StateFlow<SubtitleEngineStatus> = webSocketClient.status
    override val displaySegments: StateFlow<List<SubtitleSegment>> = sessionManager.displaySegments
    override val provisionalText: StateFlow<String> = sessionManager.provisionalText
    override val provisionalSpeaker: StateFlow<String?> = sessionManager.provisionalSpeaker
    override val lookaheadState: StateFlow<LookaheadSessionState> = lookaheadAudioPipeline.state

    init {
        setupCallbacks()
        scope.launch {
            lookaheadAudioPipeline.state.collectLatest { state ->
                if (!isStarted || state.isFallbackActive) {
                    return@collectLatest
                }
                if (state.status == LookaheadPipelineStatus.ERROR) {
                    activateFallback("Lookahead unavailable, switching to live tap")
                }
            }
        }
    }

    override suspend fun start(): SubtitleStartResult {
        if (status.value == SubtitleEngineStatus.ACTIVE || status.value == SubtitleEngineStatus.CONNECTING) {
            return SubtitleStartResult.Started
        }

        val mediaId = currentMediaId ?: return failStart("No active media item for live subtitle")

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
        isStarted = true
        sessionManager.reset()
        audioBatcher.setListener(object : AudioBatcher.Listener {
            override fun onAudioBatchReady(pcmData: ByteArray) {
                webSocketClient.sendAudio(pcmData)
            }
        })
        subtitleAudioProcessor.setEnabled(false)
        startGeneration(
            mediaId = mediaId,
            positionMs = currentPlaybackPositionMs,
            audioTrackKey = currentAudioTrackKey,
            reconnectTransport = true,
        )
        sessionResetScheduler.start(::resetSession)
        Logger.logDebug(TAG, "Engine started with target=${config.targetLanguage}")
        return SubtitleStartResult.Started
    }

    override fun stop() {
        isStarted = false
        sessionResetScheduler.stop()
        lookaheadAudioPipeline.stop()
        subtitleAudioProcessor.setEnabled(false)
        webSocketClient.disconnect()
        audioBatcher.reset()
        sessionManager.clearDisplay()
        currentConfig = null
        Logger.logDebug(TAG, "Engine stopped")
    }

    override fun resetSession() {
        Logger.logDebug(TAG, "Resetting session (make-before-break với carryover context)")
        val baseConfig = currentConfig ?: return
        val carryover = sessionManager.getCarryoverContext().ifBlank { null }
        val newConfig = baseConfig.copy(carryoverContext = carryover)

        currentConfig = newConfig
        webSocketClient.rotateSession(newConfig)
        sessionResetScheduler.start(::resetSession)
    }

    override fun onPlaybackPosition(positionMs: Long) {
        currentPlaybackPositionMs = positionMs
        sessionManager.onPlaybackPosition(positionMs)
        lookaheadAudioPipeline.updatePlaybackPosition(positionMs)
    }

    override fun onPlayWhenReadyChanged(isPlaying: Boolean) {
        if (!isStarted) {
            return
        }
        if (isPlaying) {
            if (!lookaheadState.value.isFallbackActive) {
                lookaheadAudioPipeline.resume(currentPlaybackPositionMs)
            }
        } else {
            lookaheadAudioPipeline.pause()
        }
    }

    override fun onSeek(positionMs: Long) {
        currentPlaybackPositionMs = positionMs
        if (!isStarted) {
            return
        }
        restartForPlaybackReset(positionMs, currentAudioTrackKey)
    }

    override fun onMediaItemChanged(mediaId: String, positionMs: Long) {
        val previousMediaId = currentMediaId
        currentMediaId = mediaId
        currentPlaybackPositionMs = positionMs
        if (!isStarted || previousMediaId == null || previousMediaId == mediaId) {
            return
        }
        restartForPlaybackReset(positionMs, currentAudioTrackKey)
    }

    override fun onAudioTrackChanged(trackKey: String?) {
        if (trackKey == currentAudioTrackKey) {
            return
        }
        currentAudioTrackKey = trackKey
        if (!isStarted) {
            return
        }
        restartForPlaybackReset(currentPlaybackPositionMs, currentAudioTrackKey)
    }

    override fun onStop() {
        stop()
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
                startMs: Long?,
                endMs: Long?,
            ) {
                sessionManager.onOriginal(text, speaker, language, confidence, startMs, endMs)
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

    private fun startGeneration(
        mediaId: String,
        positionMs: Long,
        audioTrackKey: String?,
        reconnectTransport: Boolean,
    ) {
        val config = currentConfig ?: return
        val generationId = ++currentGenerationId
        sessionManager.beginGeneration(
            generationId = generationId,
            basePositionMs = positionMs,
            clearHistory = true,
        )
        audioBatcher.reset()
        subtitleAudioProcessor.setEnabled(false)
        lookaheadAudioPipeline.setFallbackActive(false)

        if (reconnectTransport) {
            webSocketClient.reconnectImmediately(config)
        } else {
            webSocketClient.connect(config)
        }

        lookaheadAudioPipeline.start(
            mediaId = mediaId,
            initialPositionMs = positionMs,
            audioTrackKey = audioTrackKey,
            generationId = generationId,
        )
    }

    private fun restartForPlaybackReset(positionMs: Long, audioTrackKey: String?) {
        val mediaId = currentMediaId ?: return
        startGeneration(
            mediaId = mediaId,
            positionMs = positionMs,
            audioTrackKey = audioTrackKey,
            reconnectTransport = true,
        )
    }

    private fun activateFallback(reason: String) {
        if (!isStarted) {
            return
        }

        val config = currentConfig ?: return
        val mediaId = currentMediaId ?: return
        Logger.logDebug(TAG, reason)
        lookaheadAudioPipeline.stop()
        lookaheadAudioPipeline.setFallbackActive(true)

        val generationId = ++currentGenerationId
        sessionManager.beginGeneration(
            generationId = generationId,
            basePositionMs = currentPlaybackPositionMs,
            clearHistory = true,
        )
        audioBatcher.reset()
        webSocketClient.reconnectImmediately(config)
        subtitleAudioProcessor.setEnabled(true)
        currentMediaId = mediaId
    }
}
