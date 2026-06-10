package dev.anilbeesetti.nextplayer.feature.player

import android.net.Uri
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.anilbeesetti.nextplayer.core.data.repository.MediaRepository
import dev.anilbeesetti.nextplayer.core.data.repository.PreferencesRepository
import dev.anilbeesetti.nextplayer.core.domain.GetSortedPlaylistUseCase
import dev.anilbeesetti.nextplayer.core.model.LoopMode
import dev.anilbeesetti.nextplayer.core.model.PlayerPreferences
import dev.anilbeesetti.nextplayer.core.model.Video
import dev.anilbeesetti.nextplayer.core.model.VideoContentScale
import dev.anilbeesetti.nextplayer.core.subtitle.engine.OfflineSubtitleEngine
import dev.anilbeesetti.nextplayer.core.subtitle.engine.SubtitleEngine
import dev.anilbeesetti.nextplayer.core.subtitle.engine.SubtitleStartResult
import dev.anilbeesetti.nextplayer.core.subtitle.model.SubtitleEngineStatus
import dev.anilbeesetti.nextplayer.core.subtitle.model.SubtitleRuntimeMode
import dev.anilbeesetti.nextplayer.core.subtitle.model.SubtitleSegment
import dev.anilbeesetti.nextplayer.feature.player.state.SubtitleOptionsEvent
import dev.anilbeesetti.nextplayer.feature.player.state.VideoZoomEvent
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val preferencesRepository: PreferencesRepository,
    private val getSortedPlaylistUseCase: GetSortedPlaylistUseCase,
    val subtitleEngine: SubtitleEngine,
    private val offlineSubtitleEngine: OfflineSubtitleEngine,
) : ViewModel() {

    companion object {
        private const val CONNECTING_TO_SONIOX_MESSAGE = "Connecting to Soniox..."
        private const val CONNECTING_TO_OFFLINE_SUBTITLE_MESSAGE = "Starting offline subtitles..."
    }

    var playWhenReady: Boolean = true

    // Live subtitle state
    private val _liveSubtitleActive = MutableStateFlow(false)
    val liveSubtitleActive: StateFlow<Boolean> = _liveSubtitleActive.asStateFlow()

    private val _subtitleNotice = MutableStateFlow<String?>(null)
    val subtitleNotice: StateFlow<String?> = _subtitleNotice.asStateFlow()

    private val _subtitleSegments = MutableStateFlow<List<SubtitleSegment>>(emptyList())
    val subtitleSegments: StateFlow<List<SubtitleSegment>> = _subtitleSegments.asStateFlow()

    private val _provisionalText = MutableStateFlow("")
    val provisionalText: StateFlow<String> = _provisionalText.asStateFlow()

    private val _subtitleStatus = MutableStateFlow(SubtitleEngineStatus.IDLE)
    val subtitleStatus: StateFlow<SubtitleEngineStatus> = _subtitleStatus.asStateFlow()

    private val _activeSubtitleRuntimeMode = MutableStateFlow<SubtitleRuntimeMode?>(null)

    private val internalUiState = MutableStateFlow(
        PlayerUiState(
            playerPreferences = preferencesRepository.playerPreferences.value,
        ),
    )
    val uiState = internalUiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesRepository.playerPreferences.collect { prefs ->
                internalUiState.update { it.copy(playerPreferences = prefs) }
            }
        }

        viewModelScope.launch {
            subtitleEngine.status.collect { status ->
                if (_activeSubtitleRuntimeMode.value == SubtitleRuntimeMode.ONLINE_SONIOX) {
                    _subtitleStatus.value = status
                    if (status.isInactive()) {
                        _liveSubtitleActive.value = false
                    }
                }
            }
        }

        viewModelScope.launch {
            offlineSubtitleEngine.status.collect { status ->
                if (_activeSubtitleRuntimeMode.value == SubtitleRuntimeMode.OFFLINE) {
                    _subtitleStatus.value = status
                    if (status.isInactive()) {
                        _liveSubtitleActive.value = false
                    }
                }
            }
        }

        viewModelScope.launch {
            subtitleEngine.displaySegments.collect { segments ->
                if (_activeSubtitleRuntimeMode.value == SubtitleRuntimeMode.ONLINE_SONIOX) {
                    _subtitleSegments.value = segments
                }
            }
        }

        viewModelScope.launch {
            offlineSubtitleEngine.displaySegments.collect { segments ->
                if (_activeSubtitleRuntimeMode.value == SubtitleRuntimeMode.OFFLINE) {
                    _subtitleSegments.value = segments
                }
            }
        }

        viewModelScope.launch {
            subtitleEngine.provisionalText.collect { text ->
                if (_activeSubtitleRuntimeMode.value == SubtitleRuntimeMode.ONLINE_SONIOX) {
                    _provisionalText.value = text
                }
            }
        }

        viewModelScope.launch {
            offlineSubtitleEngine.provisionalText.collect { text ->
                if (_activeSubtitleRuntimeMode.value == SubtitleRuntimeMode.OFFLINE) {
                    _provisionalText.value = text
                }
            }
        }
    }

    suspend fun getPlaylistFromUri(uri: Uri): List<Video> {
        return getSortedPlaylistUseCase.invoke(uri)
    }

    fun updateVideoZoom(uri: String, zoom: Float) {
        viewModelScope.launch {
            mediaRepository.updateMediumZoom(uri, zoom)
        }
    }

    fun updatePlayerBrightness(value: Float) {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences { it.copy(playerBrightness = value) }
        }
    }

    fun updateVideoContentScale(contentScale: VideoContentScale) {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences { it.copy(playerVideoZoom = contentScale) }
        }
    }

    fun setLoopMode(loopMode: LoopMode) {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences { it.copy(loopMode = loopMode) }
        }
    }

    fun onVideoZoomEvent(event: VideoZoomEvent) {
        when (event) {
            is VideoZoomEvent.ContentScaleChanged -> {
                updateVideoContentScale(event.contentScale)
            }
            is VideoZoomEvent.ZoomChanged -> {
                updateVideoZoom(event.mediaItem.mediaId, event.zoom)
            }
        }
    }

    fun toggleLiveSubtitle() {
        viewModelScope.launch {
            if (_liveSubtitleActive.value) {
                stopLiveSubtitle()
            } else {
                startLiveSubtitle()
            }
        }
    }

    suspend fun startLiveSubtitle() {
        val runtimeMode = if (preferencesRepository.playerPreferences.value.offlineSubtitleEnabled) {
            SubtitleRuntimeMode.OFFLINE
        } else {
            SubtitleRuntimeMode.ONLINE_SONIOX
        }
        val engine = engineFor(runtimeMode)
        _activeSubtitleRuntimeMode.value = runtimeMode

        when (val startResult = engine.start()) {
            SubtitleStartResult.Started -> {
                _liveSubtitleActive.value = true
                _subtitleNotice.value = when (runtimeMode) {
                    SubtitleRuntimeMode.ONLINE_SONIOX -> CONNECTING_TO_SONIOX_MESSAGE
                    SubtitleRuntimeMode.OFFLINE -> CONNECTING_TO_OFFLINE_SUBTITLE_MESSAGE
                }
                preferencesRepository.updatePlayerPreferences {
                    it.copy(liveSubtitleEnabled = runtimeMode == SubtitleRuntimeMode.ONLINE_SONIOX)
                }
            }
            is SubtitleStartResult.Failed -> {
                _activeSubtitleRuntimeMode.value = null
                _subtitleNotice.value = startResult.message
                preferencesRepository.updatePlayerPreferences {
                    it.copy(liveSubtitleEnabled = false)
                }
            }
        }
    }

    suspend fun stopLiveSubtitle() {
        activeEngine()?.stop()
        _activeSubtitleRuntimeMode.value = null
        _subtitleSegments.value = emptyList()
        _provisionalText.value = ""
        _subtitleStatus.value = SubtitleEngineStatus.STOPPED
        _liveSubtitleActive.value = false
        preferencesRepository.updatePlayerPreferences {
            it.copy(liveSubtitleEnabled = false)
        }
    }

    fun consumeSubtitleNotice() {
        _subtitleNotice.value = null
    }

    override fun onCleared() {
        super.onCleared()
        if (_liveSubtitleActive.value) {
            activeEngine()?.stop()
        }
    }

    fun onSubtitleOptionEvent(event: SubtitleOptionsEvent) {
        when (event) {
            is SubtitleOptionsEvent.DelayChanged -> {
                updateSubtitleDelay(event.mediaItem.mediaId, event.delay)
            }
            is SubtitleOptionsEvent.SpeedChanged -> {
                updateSubtitleSpeed(event.mediaItem.mediaId, event.speed)
            }
        }
    }

    private fun updateSubtitleDelay(uri: String, delay: Long) {
        viewModelScope.launch {
            mediaRepository.updateSubtitleDelay(uri, delay)
        }
    }

    private fun updateSubtitleSpeed(uri: String, speed: Float) {
        viewModelScope.launch {
            mediaRepository.updateSubtitleSpeed(uri, speed)
        }
    }

    private fun activeEngine(): SubtitleEngine? {
        return _activeSubtitleRuntimeMode.value?.let(::engineFor)
    }

    private fun engineFor(runtimeMode: SubtitleRuntimeMode): SubtitleEngine {
        return when (runtimeMode) {
            SubtitleRuntimeMode.ONLINE_SONIOX -> subtitleEngine
            SubtitleRuntimeMode.OFFLINE -> offlineSubtitleEngine
        }
    }

    private fun SubtitleEngineStatus.isInactive(): Boolean {
        return this == SubtitleEngineStatus.ERROR ||
            this == SubtitleEngineStatus.STOPPED ||
            this == SubtitleEngineStatus.IDLE
    }
}

@Stable
data class PlayerUiState(
    val playerPreferences: PlayerPreferences? = null,
)

sealed interface PlayerEvent
