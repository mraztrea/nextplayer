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
import dev.anilbeesetti.nextplayer.core.subtitle.engine.SubtitleEngine
import dev.anilbeesetti.nextplayer.core.subtitle.engine.SubtitleStartResult
import dev.anilbeesetti.nextplayer.core.subtitle.model.LookaheadPipelineStatus
import dev.anilbeesetti.nextplayer.core.subtitle.model.LookaheadSessionState
import dev.anilbeesetti.nextplayer.core.subtitle.model.SubtitleEngineStatus
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
) : ViewModel() {

    companion object {
        private const val CONNECTING_TO_SONIOX_MESSAGE = "Connecting to Soniox..."
    }

    var playWhenReady: Boolean = true

    // Live subtitle state
    private val _liveSubtitleActive = MutableStateFlow(false)
    val liveSubtitleActive: StateFlow<Boolean> = _liveSubtitleActive.asStateFlow()

    private val _subtitleNotice = MutableStateFlow<String?>(null)
    val subtitleNotice: StateFlow<String?> = _subtitleNotice.asStateFlow()

    val subtitleSegments: StateFlow<List<SubtitleSegment>> = subtitleEngine.displaySegments
    val provisionalText: StateFlow<String> = subtitleEngine.provisionalText
    val subtitleStatus: StateFlow<SubtitleEngineStatus> = subtitleEngine.status
    val lookaheadState: StateFlow<LookaheadSessionState> = subtitleEngine.lookaheadState

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
                if (status == SubtitleEngineStatus.ERROR || status == SubtitleEngineStatus.STOPPED || status == SubtitleEngineStatus.IDLE) {
                    _liveSubtitleActive.value = false
                }
                if (status == SubtitleEngineStatus.ACTIVE) {
                    _subtitleNotice.value = null
                }
            }
        }

        viewModelScope.launch {
            subtitleEngine.lookaheadState.collect { state ->
                _subtitleNotice.value = when {
                    !_liveSubtitleActive.value -> null
                    state.isFallbackActive -> "Lookahead unavailable, using live tap"
                    state.status == LookaheadPipelineStatus.WARMING -> "Preparing lookahead subtitle..."
                    else -> _subtitleNotice.value?.takeIf { it == CONNECTING_TO_SONIOX_MESSAGE }
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
        when (val startResult = subtitleEngine.start()) {
            SubtitleStartResult.Started -> {
                _liveSubtitleActive.value = true
                _subtitleNotice.value = CONNECTING_TO_SONIOX_MESSAGE
                preferencesRepository.updatePlayerPreferences {
                    it.copy(liveSubtitleEnabled = true)
                }
            }
            is SubtitleStartResult.Failed -> {
                _subtitleNotice.value = startResult.message
                preferencesRepository.updatePlayerPreferences {
                    it.copy(liveSubtitleEnabled = false)
                }
            }
        }
    }

    suspend fun stopLiveSubtitle() {
        subtitleEngine.stop()
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
            subtitleEngine.stop()
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
}

@Stable
data class PlayerUiState(
    val playerPreferences: PlayerPreferences? = null,
)

sealed interface PlayerEvent
