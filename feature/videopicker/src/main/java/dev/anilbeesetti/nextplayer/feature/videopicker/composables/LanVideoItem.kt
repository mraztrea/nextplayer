package dev.anilbeesetti.nextplayer.feature.videopicker.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.anilbeesetti.nextplayer.core.model.LanMediaItem
import dev.anilbeesetti.nextplayer.core.model.LanThumbnailState
import dev.anilbeesetti.nextplayer.core.ui.components.NextSegmentedListItem
import dev.anilbeesetti.nextplayer.core.ui.designsystem.NextIcons

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LanVideoItem(
    item: LanMediaItem,
    modifier: Modifier = Modifier,
    isFirstItem: Boolean = false,
    isLastItem: Boolean = false,
    onClick: () -> Unit = {},
) {
    NextSegmentedListItem(
        modifier = modifier,
        contentPadding = PaddingValues(8.dp),
        colors = ListItemDefaults.segmentedColors(),
        isFirstItem = isFirstItem,
        isLastItem = isLastItem,
        onClick = onClick,
        leadingContent = {
            Icon(
                imageVector = when (item.thumbnailState) {
                    LanThumbnailState.Ready -> NextIcons.Image
                    else -> NextIcons.Video
                },
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        content = {
            Text(
                text = item.name,
                maxLines = 2,
                style = MaterialTheme.typography.titleMedium,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item.sizeBytes?.let { Text(text = it.formatBytes(), style = MaterialTheme.typography.bodySmall) }
                item.modifiedAt?.let { Text(text = "LAN", style = MaterialTheme.typography.bodySmall) }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LanFolderItem(
    item: LanMediaItem,
    modifier: Modifier = Modifier,
    isFirstItem: Boolean = false,
    isLastItem: Boolean = false,
    onClick: () -> Unit = {},
) {
    NextSegmentedListItem(
        modifier = modifier,
        contentPadding = PaddingValues(8.dp),
        colors = ListItemDefaults.segmentedColors(),
        isFirstItem = isFirstItem,
        isLastItem = isLastItem,
        onClick = onClick,
        leadingContent = {
            Icon(
                imageVector = NextIcons.Folder,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        content = {
            Column {
                Text(
                    text = item.name,
                    maxLines = 1,
                    style = MaterialTheme.typography.titleMedium,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
    )
}

private fun Long.formatBytes(): String {
    if (this < 1024) return "$this B"
    val kb = this / 1024.0
    if (kb < 1024) return "%.1f KB".format(kb)
    val mb = kb / 1024.0
    if (mb < 1024) return "%.1f MB".format(mb)
    return "%.1f GB".format(mb / 1024.0)
}
