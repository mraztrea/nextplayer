package dev.anilbeesetti.nextplayer.core.subtitle.engine

import dev.anilbeesetti.nextplayer.core.common.Logger
import dev.anilbeesetti.nextplayer.core.data.repository.PreferencesRepository
import dev.anilbeesetti.nextplayer.core.subtitle.model.GeminiLiveError
import dev.anilbeesetti.nextplayer.core.subtitle.model.GeminiLiveTranscriptEvent
import dev.anilbeesetti.nextplayer.core.subtitle.model.GeminiLiveTranscriptEventType
import dev.anilbeesetti.nextplayer.core.subtitle.model.GeminiLanguageMapper
import dev.anilbeesetti.nextplayer.core.subtitle.model.GeminiSessionConfig
import dev.anilbeesetti.nextplayer.core.subtitle.model.SegmentStatus
import dev.anilbeesetti.nextplayer.core.subtitle.model.SubtitleDisplayMode
import dev.anilbeesetti.nextplayer.core.subtitle.model.SubtitleEngineStatus
import dev.anilbeesetti.nextplayer.core.subtitle.model.SubtitleProvider
import dev.anilbeesetti.nextplayer.core.subtitle.model.SubtitleSegment
import dev.anilbeesetti.nextplayer.core.subtitle.storage.SecureApiKeyStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiSubtitleProvider @Inject constructor(
    private val webSocketClient: GeminiLiveWebSocketClient,
    private val preferencesRepository: PreferencesRepository,
    private val secureApiKeyStorage: SecureApiKeyStorage,
) : RealtimeSubtitleProvider {
    companion object {
        private const val TAG = "GeminiSubtitleProvider"
        private const val MAX_DISPLAY_SEGMENTS = 20

        internal fun toDisplaySegment(
            id: Long,
            inputEvent: GeminiLiveTranscriptEvent?,
            outputEvent: GeminiLiveTranscriptEvent,
            displayMode: SubtitleDisplayMode,
            targetLanguageCode: String,
        ): SubtitleSegment {
            val originalText = inputEvent?.text?.takeIf { it.isNotBlank() }
                ?: outputEvent.text.orEmpty()
            val translationText = outputEvent.text.orEmpty()
            val displayText = when (displayMode) {
                SubtitleDisplayMode.ORIGINAL_ONLY -> originalText
                SubtitleDisplayMode.BILINGUAL -> listOf(originalText, translationText)
                    .filter { it.isNotBlank() }
                    .joinToString("\n")
                SubtitleDisplayMode.TRANSLATION_ONLY -> translationText.ifBlank { originalText }
            }

            return SubtitleSegment(
                id = id,
                originalText = originalText,
                translationText = translationText.ifBlank { null },
                status = SegmentStatus.FINAL,
                provider = SubtitleProvider.GEMINI_LIVE,
                displayText = displayText,
                targetLanguageCode = targetLanguageCode,
                isProvisional = false,
                language = inputEvent?.languageCode,
            )
        }
    }

    override val provider: SubtitleProvider = SubtitleProvider.GEMINI_LIVE

    private val _status = MutableStateFlow(SubtitleEngineStatus.IDLE)
    override val status: StateFlow<SubtitleEngineStatus> = _status.asStateFlow()

    private val _displaySegments = MutableStateFlow<List<SubtitleSegment>>(emptyList())
    override val displaySegments: StateFlow<List<SubtitleSegment>> = _displaySegments.asStateFlow()

    private val _provisionalText = MutableStateFlow("")
    override val provisionalText: StateFlow<String> = _provisionalText.asStateFlow()

    private val _provisionalSpeaker = MutableStateFlow<String?>(null)
    override val provisionalSpeaker: StateFlow<String?> = _provisionalSpeaker.asStateFlow()

    private val idCounter = AtomicLong(0)
    private var pendingInputEvent: GeminiLiveTranscriptEvent? = null
    private var currentConfig: GeminiSessionConfig? = null

    init {
        webSocketClient.setListener(object : GeminiLiveWebSocketClient.Listener {
            override fun onTranscriptEvents(events: List<GeminiLiveTranscriptEvent>) {
                handleTranscriptEvents(events)
            }

            override fun onStatusChange(status: SubtitleEngineStatus) {
                _status.value = status
            }

            override fun onError(error: GeminiLiveError) {
                Logger.logError(
                    TAG,
                    SubtitleDiagnosticLogger.metadata(
                        "event" to "error",
                        "provider" to "Gemini Live",
                        "category" to error.category.name,
                        "providerCode" to error.providerCode,
                        "recoverable" to error.isRecoverable.toString(),
                    ),
                )
                _status.value = SubtitleEngineStatus.GEMINI_ERROR
            }
        })
    }

    override suspend fun start(): SubtitleStartResult {
        if (status.value == SubtitleEngineStatus.ACTIVE || status.value == SubtitleEngineStatus.CONNECTING) {
            return SubtitleStartResult.Started
        }

        val apiKey = secureApiKeyStorage.getApiKey(SubtitleProvider.GEMINI_LIVE)?.trim().orEmpty()
        if (apiKey.isBlank()) {
            val error = GeminiLiveErrorMapper.missingApiKey()
            _status.value = SubtitleEngineStatus.NOT_CONFIGURED
            return SubtitleStartResult.Failed(error.message)
        }

        val validationError = webSocketClient.validateApiKey(apiKey)
        if (validationError != null) {
            _status.value = SubtitleEngineStatus.GEMINI_ERROR
            return SubtitleStartResult.Failed(validationError)
        }

        val preferences = preferencesRepository.playerPreferences.value
        val config = GeminiSessionConfig(
            apiKey = apiKey,
            targetLanguageCode = GeminiLanguageMapper.toTargetLanguageCode(preferences.geminiTargetLanguage),
            displayMode = SubtitleDisplayMode.fromPreference(preferences.geminiDisplayMode),
        )

        currentConfig = config
        resetDisplay()
        webSocketClient.connect(config)
        return SubtitleStartResult.Started
    }

    override fun stop() {
        webSocketClient.disconnect()
        resetDisplay()
        currentConfig = null
        _status.value = SubtitleEngineStatus.STOPPED
    }

    override fun resetSession() {
        val config = currentConfig ?: return
        _status.value = SubtitleEngineStatus.RESETTING
        resetDisplay()
        webSocketClient.disconnect()
        webSocketClient.connect(config)
    }

    override fun sendAudio(pcmData: ByteArray) {
        webSocketClient.sendAudio(pcmData)
    }

    private fun handleTranscriptEvents(events: List<GeminiLiveTranscriptEvent>) {
        events.forEach { event ->
            when (event.eventType) {
                GeminiLiveTranscriptEventType.INPUT_TRANSCRIPT -> handleInputTranscript(event)
                GeminiLiveTranscriptEventType.OUTPUT_TRANSCRIPT -> handleOutputTranscript(event)
                GeminiLiveTranscriptEventType.STATUS -> Unit
                GeminiLiveTranscriptEventType.ERROR -> {
                    _status.value = SubtitleEngineStatus.GEMINI_ERROR
                    Logger.logError(
                        TAG,
                        SubtitleDiagnosticLogger.metadata(
                            "event" to "transcript_error",
                            "provider" to "Gemini Live",
                            "providerCode" to event.errorCode,
                        ),
                    )
                }
            }
        }
    }

    private fun handleInputTranscript(event: GeminiLiveTranscriptEvent) {
        if (event.isFinal) {
            pendingInputEvent = event
            _provisionalText.value = ""
        } else {
            _provisionalText.value = event.text.orEmpty()
        }
    }

    private fun handleOutputTranscript(event: GeminiLiveTranscriptEvent) {
        val config = currentConfig ?: return
        if (!event.isFinal) {
            _provisionalText.value = event.text.orEmpty()
            return
        }

        val segment = toDisplaySegment(
            id = idCounter.incrementAndGet(),
            inputEvent = pendingInputEvent,
            outputEvent = event,
            displayMode = config.displayMode,
            targetLanguageCode = config.targetLanguageCode,
        )
        pendingInputEvent = null
        _provisionalText.value = ""
        val next = (_displaySegments.value + segment).takeLast(MAX_DISPLAY_SEGMENTS)
        _displaySegments.value = next
        _status.value = SubtitleEngineStatus.TRANSLATING
    }

    private fun resetDisplay() {
        pendingInputEvent = null
        idCounter.set(0)
        _displaySegments.value = emptyList()
        _provisionalText.value = ""
        _provisionalSpeaker.value = null
    }
}
