package dev.anilbeesetti.nextplayer.feature.videopicker.lan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.anilbeesetti.nextplayer.core.domain.lan.BrowseLanFolderUseCase
import dev.anilbeesetti.nextplayer.core.model.LanFolder
import dev.anilbeesetti.nextplayer.core.model.LanResult
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class LanFolderBrowserViewModel @Inject constructor(
    private val browseLanFolderUseCase: BrowseLanFolderUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LanFolderBrowserUiState())
    val uiState: StateFlow<LanFolderBrowserUiState> = _uiState.asStateFlow()

    fun load(serverId: String, folderPath: String) {
        _uiState.update {
            it.copy(
                serverId = serverId,
                folderPath = folderPath,
                loading = true,
                error = null,
            )
        }
        viewModelScope.launch {
            when (val result = browseLanFolderUseCase(serverId, folderPath)) {
                is LanResult.Success -> _uiState.update {
                    it.copy(
                        loading = false,
                        folder = result.value,
                        error = null,
                    )
                }
                is LanResult.Failure -> _uiState.update {
                    it.copy(
                        loading = false,
                        folder = null,
                        error = result.error.type.name,
                    )
                }
            }
        }
    }

    fun refresh() {
        val current = uiState.value
        load(current.serverId, current.folderPath)
    }
}

data class LanFolderBrowserUiState(
    val serverId: String = "",
    val folderPath: String = "",
    val loading: Boolean = false,
    val folder: LanFolder? = null,
    val error: String? = null,
)
