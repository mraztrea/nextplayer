package dev.anilbeesetti.nextplayer.core.subtitle.engine

import dev.anilbeesetti.nextplayer.core.data.repository.PreferencesRepository
import dev.anilbeesetti.nextplayer.core.model.PlayerPreferences
import dev.anilbeesetti.nextplayer.core.subtitle.model.SegmentStatus
import dev.anilbeesetti.nextplayer.core.subtitle.model.SubtitleEngineStatus
import dev.anilbeesetti.nextplayer.core.subtitle.model.SubtitleSegment
import dev.anilbeesetti.nextplayer.core.subtitle.storage.OfflineModelReadiness
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrototypeOfflineSubtitleEngine @Inject constructor(
    private val modelReadiness: OfflineModelReadiness,
    private val preferencesRepository: PreferencesRepository,
) : OfflineSubtitleEngine {
    private val _status = MutableStateFlow(SubtitleEngineStatus.IDLE)
    private val _displaySegments = MutableStateFlow<List<SubtitleSegment>>(emptyList())
    private val _provisionalText = MutableStateFlow("")
    private val _provisionalSpeaker = MutableStateFlow<String?>(null)

    override val status: StateFlow<SubtitleEngineStatus> = _status.asStateFlow()
    override val displaySegments: StateFlow<List<SubtitleSegment>> = _displaySegments.asStateFlow()
    override val provisionalText: StateFlow<String> = _provisionalText.asStateFlow()
    override val provisionalSpeaker: StateFlow<String?> = _provisionalSpeaker.asStateFlow()

    override suspend fun start(): SubtitleStartResult {
        modelReadiness.refreshInstalledModel()
        if (!modelReadiness.isModelReady()) {
            _status.value = SubtitleEngineStatus.ERROR
            return SubtitleStartResult.Failed("No offline subtitle model installed")
        }

        _status.value = SubtitleEngineStatus.ACTIVE
        _displaySegments.value = listOf(createPrototypeSegment())
        return SubtitleStartResult.Started
    }

    override fun stop() {
        _displaySegments.value = emptyList()
        _provisionalText.value = ""
        _provisionalSpeaker.value = null
        _status.value = SubtitleEngineStatus.STOPPED
    }

    override fun resetSession() {
        _displaySegments.value = emptyList()
        _provisionalText.value = ""
        _provisionalSpeaker.value = null
        if (_status.value == SubtitleEngineStatus.ERROR) return
        _status.value = SubtitleEngineStatus.IDLE
    }

    private fun createPrototypeSegment(): SubtitleSegment {
        val targetLanguage = preferencesRepository.playerPreferences.value.offlineTargetLanguage.ifBlank {
            PlayerPreferences.DEFAULT_OFFLINE_SUBTITLE_TARGET_LANGUAGE
        }
        val translation = when (targetLanguage) {
            PlayerPreferences.OFFLINE_SUBTITLE_TARGET_ENGLISH -> "This is an offline subtitle prototype."
            else -> "Day la phu de offline thu nghiem."
        }

        return SubtitleSegment(
            id = System.currentTimeMillis(),
            originalText = "これはオフライン字幕のテストです。",
            translationText = translation,
            status = SegmentStatus.TRANSLATED,
            language = PlayerPreferences.OFFLINE_SUBTITLE_SOURCE_JAPANESE,
            confidence = 1.0f,
        )
    }
}
