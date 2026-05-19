package dev.anilbeesetti.nextplayer.feature.videopicker.lan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.anilbeesetti.nextplayer.core.model.LanFolderBookmark
import dev.anilbeesetti.nextplayer.core.ui.R
import dev.anilbeesetti.nextplayer.core.ui.components.NextTopAppBar
import dev.anilbeesetti.nextplayer.core.ui.designsystem.NextIcons

@Composable
fun LanBookmarkRoute(
    viewModel: LanBookmarkViewModel = hiltViewModel(),
    onNavigateUp: () -> Unit,
    onOpenBookmark: (serverId: String, folderPath: String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LanBookmarkScreen(
        uiState = uiState,
        onNavigateUp = onNavigateUp,
        onOpenBookmark = { bookmark ->
            viewModel.openBookmark(bookmark)
            onOpenBookmark(bookmark.serverId, bookmark.folderPath)
        },
        onDeleteBookmark = viewModel::deleteBookmark,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LanBookmarkScreen(
    uiState: LanBookmarkUiState,
    onNavigateUp: () -> Unit = {},
    onOpenBookmark: (LanFolderBookmark) -> Unit = {},
    onDeleteBookmark: (String) -> Unit = {},
) {
    Scaffold(
        topBar = {
            NextTopAppBar(
                title = stringResource(R.string.lan_bookmarks),
                navigationIcon = {
                    FilledTonalIconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = NextIcons.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_up),
                        )
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) { scaffoldPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (uiState.bookmarks.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.lan_no_bookmarks),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(uiState.bookmarks, key = { it.id }) { bookmark ->
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = bookmark.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = bookmark.folderPath.ifBlank { "/" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { onOpenBookmark(bookmark) }) {
                                Text(text = stringResource(R.string.lan_open_bookmark))
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            TextButton(onClick = { onDeleteBookmark(bookmark.id) }) {
                                Text(text = stringResource(R.string.delete))
                            }
                        }
                    }
                }
            }
        }
    }
}
