package dev.anilbeesetti.nextplayer.core.domain

import android.net.Uri
import dev.anilbeesetti.nextplayer.core.common.Dispatcher
import dev.anilbeesetti.nextplayer.core.common.NextDispatchers
import dev.anilbeesetti.nextplayer.core.model.PlaybackLaunchContext
import dev.anilbeesetti.nextplayer.core.model.PlaybackQueueSnapshot
import dev.anilbeesetti.nextplayer.core.model.PlaybackSourceType
import dev.anilbeesetti.nextplayer.core.model.SiblingVideoEntry
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class ResolvePlaybackQueueUseCase @Inject constructor(
    private val getSortedPlaylistUseCase: GetSortedPlaylistUseCase,
    @Dispatcher(NextDispatchers.Default) private val defaultDispatcher: CoroutineDispatcher,
) {
    suspend operator fun invoke(
        currentUri: Uri,
        launchContext: PlaybackLaunchContext?,
    ): PlaybackQueueSnapshot = withContext(defaultDispatcher) {
        val currentUriString = currentUri.toString()
        launchContext?.toSnapshot(currentUriString)?.let { return@withContext it }

        val fallbackEntries = getSortedPlaylistUseCase(currentUri)
            .map { SiblingVideoEntry(it.uriString) }
            .normalizedEntries(currentUriString)
        if (fallbackEntries.isNotEmpty()) {
            return@withContext PlaybackQueueSnapshot(
                currentUriString = currentUriString,
                currentIndex = fallbackEntries.indexOfFirst { it.uriString == currentUriString }
                    .takeIf { it >= 0 } ?: 0,
                entries = fallbackEntries,
                sourceType = PlaybackSourceType.APP_SORT_FALLBACK,
            )
        }

        PlaybackQueueSnapshot(
            currentUriString = currentUriString,
            currentIndex = 0,
            entries = listOf(SiblingVideoEntry(currentUriString)),
            sourceType = PlaybackSourceType.SINGLE_ITEM,
        )
    }
}

private fun PlaybackLaunchContext.toSnapshot(currentUriString: String): PlaybackQueueSnapshot? {
    val entries = siblings.normalizedEntries(currentUriString)
    if (entries.isEmpty()) return null
    return PlaybackQueueSnapshot(
        currentUriString = currentUriString,
        currentIndex = entries.indexOfFirst { it.uriString == currentUriString }
            .takeIf { it >= 0 } ?: 0,
        entries = entries,
        sourceType = sourceType,
    )
}

private fun List<SiblingVideoEntry>.normalizedEntries(currentUriString: String): List<SiblingVideoEntry> {
    return buildList {
        add(SiblingVideoEntry(currentUriString))
        addAll(this@normalizedEntries)
    }.distinctBy { it.uriString }
}
