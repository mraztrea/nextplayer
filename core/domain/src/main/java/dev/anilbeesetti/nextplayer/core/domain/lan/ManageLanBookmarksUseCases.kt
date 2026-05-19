package dev.anilbeesetti.nextplayer.core.domain.lan

import dev.anilbeesetti.nextplayer.core.data.repository.LanBookmarkRepository
import dev.anilbeesetti.nextplayer.core.model.LanFolderBookmark
import dev.anilbeesetti.nextplayer.core.model.LanResult
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ManageLanBookmarksUseCases @Inject constructor(
    private val lanBookmarkRepository: LanBookmarkRepository,
) {
    fun observeBookmarks(): Flow<List<LanFolderBookmark>> = lanBookmarkRepository.observeBookmarks()

    suspend fun addBookmark(
        serverId: String,
        folderPath: String,
        displayName: String,
    ): LanResult<LanFolderBookmark> {
        val now = System.currentTimeMillis()
        return lanBookmarkRepository.saveBookmark(
            LanFolderBookmark(
                id = UUID.randomUUID().toString(),
                serverId = serverId,
                folderPath = folderPath,
                displayName = displayName,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    suspend fun deleteBookmark(id: String) = lanBookmarkRepository.deleteBookmark(id)

    suspend fun markOpened(id: String, openedAt: Long = System.currentTimeMillis()) =
        lanBookmarkRepository.markOpened(id, openedAt)
}
