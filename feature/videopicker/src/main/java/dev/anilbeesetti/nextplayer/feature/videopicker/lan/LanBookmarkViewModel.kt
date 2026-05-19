package dev.anilbeesetti.nextplayer.feature.videopicker.lan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.anilbeesetti.nextplayer.core.domain.lan.ManageLanBookmarksUseCases
import dev.anilbeesetti.nextplayer.core.model.LanFolderBookmark
import dev.anilbeesetti.nextplayer.core.model.LanResult
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class LanBookmarkViewModel @Inject constructor(
    private val manageLanBookmarksUseCases: ManageLanBookmarksUseCases,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LanBookmarkUiState())
    val uiState: StateFlow<LanBookmarkUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            manageLanBookmarksUseCases.observeBookmarks().collect { bookmarks ->
                _uiState.update { it.copy(bookmarks = bookmarks) }
            }
        }
    }

    fun addCurrentFolder(serverId: String, folderPath: String, displayName: String) {
        viewModelScope.launch {
            when (val result = manageLanBookmarksUseCases.addBookmark(serverId, folderPath, displayName)) {
                is LanResult.Success -> _uiState.update { it.copy(message = LanBookmarkMessage.Saved) }
                is LanResult.Failure -> _uiState.update { it.copy(message = LanBookmarkMessage.Error(result.error.type.name)) }
            }
        }
    }

    fun openBookmark(bookmark: LanFolderBookmark) {
        viewModelScope.launch {
            manageLanBookmarksUseCases.markOpened(bookmark.id)
        }
    }

    fun deleteBookmark(id: String) {
        viewModelScope.launch {
            manageLanBookmarksUseCases.deleteBookmark(id)
        }
    }
}

data class LanBookmarkUiState(
    val bookmarks: List<LanFolderBookmark> = emptyList(),
    val message: LanBookmarkMessage? = null,
)

sealed interface LanBookmarkMessage {
    data object Saved : LanBookmarkMessage
    data class Error(val typeName: String) : LanBookmarkMessage
}
