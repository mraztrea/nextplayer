package dev.anilbeesetti.nextplayer.feature.videopicker.lan

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.anilbeesetti.nextplayer.core.model.LanErrorType
import dev.anilbeesetti.nextplayer.core.model.LanMediaItem
import dev.anilbeesetti.nextplayer.core.model.LanMediaItemType
import dev.anilbeesetti.nextplayer.core.ui.R
import dev.anilbeesetti.nextplayer.core.ui.components.NextTopAppBar
import dev.anilbeesetti.nextplayer.core.ui.designsystem.NextIcons
import dev.anilbeesetti.nextplayer.feature.videopicker.composables.LanFolderItem
import dev.anilbeesetti.nextplayer.feature.videopicker.composables.LanVideoItem

@Composable
fun LanFolderBrowserRoute(
    serverId: String,
    folderPath: String,
    viewModel: LanFolderBrowserViewModel = hiltViewModel(),
    bookmarkViewModel: LanBookmarkViewModel = hiltViewModel(),
    onNavigateUp: () -> Unit,
    onFolderClick: (serverId: String, folderPath: String) -> Unit,
    onPlayVideo: (uri: Uri, siblingUris: List<Uri>) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(serverId, folderPath) {
        viewModel.load(serverId, folderPath)
    }

    LanFolderBrowserScreen(
        uiState = uiState,
        onNavigateUp = onNavigateUp,
        onRefresh = viewModel::refresh,
        onBookmarkCurrent = {
            uiState.folder?.let { folder ->
                bookmarkViewModel.addCurrentFolder(folder.serverId, folder.path, folder.displayPath)
            }
        },
        onFolderClick = onFolderClick,
        onPlayVideo = onPlayVideo,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LanFolderBrowserScreen(
    uiState: LanFolderBrowserUiState,
    onNavigateUp: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onBookmarkCurrent: () -> Unit = {},
    onFolderClick: (serverId: String, folderPath: String) -> Unit = { _, _ -> },
    onPlayVideo: (uri: Uri, siblingUris: List<Uri>) -> Unit = { _, _ -> },
) {
    Scaffold(
        topBar = {
            NextTopAppBar(
                title = uiState.folder?.displayPath ?: stringResource(R.string.lan_video),
                navigationIcon = {
                    FilledTonalIconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = NextIcons.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_up),
                        )
                    }
                },
                actions = {
                    TextButton(onClick = onBookmarkCurrent) {
                        Text(text = stringResource(R.string.lan_bookmark_current_folder))
                    }
                    TextButton(onClick = onRefresh) {
                        Text(text = stringResource(R.string.lan_refresh))
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) { scaffoldPadding ->
        when {
            uiState.loading -> {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(scaffoldPadding)
                        .fillMaxSize()
                        .wrapContentSize(),
                )
            }
            uiState.error != null -> {
                LazyColumn(
                    modifier = Modifier
                        .padding(scaffoldPadding)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    item {
                        Text(
                            text = stringResource(lanFolderErrorMessageRes(uiState.error)),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    item {
                        TextButton(onClick = onRefresh) {
                            Text(text = stringResource(R.string.lan_retry))
                        }
                    }
                }
            }
            uiState.folder?.items.isNullOrEmpty() -> {
                LazyColumn(
                    modifier = Modifier
                        .padding(scaffoldPadding)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    item {
                        Text(text = stringResource(R.string.lan_empty_folder))
                    }
                }
            }
            else -> {
                val items = uiState.folder.items
                val siblingUris = items.asSequence()
                    .filter { it.type == LanMediaItemType.Video }
                    .mapNotNull { it.playbackUri?.toUri() }
                    .toList()
                LazyColumn(
                    modifier = Modifier
                        .padding(scaffoldPadding)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    itemsIndexed(
                        items = items,
                        key = { _, item -> item.path },
                    ) { index, item ->
                        val isFirst = index == 0
                        val isLast = index == items.lastIndex
                        when (item.type) {
                            LanMediaItemType.Folder -> LanFolderItem(
                                item = item,
                                isFirstItem = isFirst,
                                isLastItem = isLast,
                                onClick = { onFolderClick(item.serverId, item.path) },
                            )
                            LanMediaItemType.Video -> LanVideoItem(
                                item = item,
                                isFirstItem = isFirst,
                                isLastItem = isLast,
                                onClick = {
                                    item.playbackUri?.toUri()?.let { onPlayVideo(it, siblingUris) }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun lanFolderErrorMessageRes(errorTypeName: String): Int {
    return when (errorTypeName) {
        LanErrorType.AuthenticationFailed.name -> R.string.lan_error_authentication
        LanErrorType.ServerUnreachable.name -> R.string.lan_error_unreachable
        LanErrorType.PermissionDenied.name -> R.string.lan_error_permission
        LanErrorType.PathNotFound.name,
        LanErrorType.ShareNotFound.name,
        -> R.string.lan_error_missing_path
        else -> R.string.lan_error_unknown
    }
}
