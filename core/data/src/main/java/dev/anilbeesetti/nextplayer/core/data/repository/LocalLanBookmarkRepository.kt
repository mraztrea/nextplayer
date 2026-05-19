package dev.anilbeesetti.nextplayer.core.data.repository

import dev.anilbeesetti.nextplayer.core.data.mappers.asEntity
import dev.anilbeesetti.nextplayer.core.data.mappers.asExternalModel
import dev.anilbeesetti.nextplayer.core.database.dao.LanFolderBookmarkDao
import dev.anilbeesetti.nextplayer.core.model.LanError
import dev.anilbeesetti.nextplayer.core.model.LanErrorType
import dev.anilbeesetti.nextplayer.core.model.LanFolderBookmark
import dev.anilbeesetti.nextplayer.core.model.LanResult
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LocalLanBookmarkRepository @Inject constructor(
    private val lanFolderBookmarkDao: LanFolderBookmarkDao,
    private val lanServerRepository: LanServerRepository,
) : LanBookmarkRepository {

    override fun observeBookmarks(): Flow<List<LanFolderBookmark>> =
        lanFolderBookmarkDao.observeAll().map { bookmarks -> bookmarks.map { it.asExternalModel() } }

    override suspend fun saveBookmark(bookmark: LanFolderBookmark): LanResult<LanFolderBookmark> {
        if (lanServerRepository.getServer(bookmark.serverId) == null) {
            return LanResult.Failure(LanError(LanErrorType.PathNotFound))
        }
        val normalized = bookmark.copy(folderPath = bookmark.folderPath.normalizedFolderPath())
        if (lanFolderBookmarkDao.findDuplicate(normalized.serverId, normalized.folderPath, normalized.id) != null) {
            return LanResult.Failure(LanError(LanErrorType.Duplicate))
        }
        lanFolderBookmarkDao.upsert(normalized.asEntity())
        return LanResult.Success(normalized)
    }

    override suspend fun deleteBookmark(id: String) {
        lanFolderBookmarkDao.delete(id)
    }

    override suspend fun markOpened(id: String, openedAt: Long) {
        lanFolderBookmarkDao.markOpened(id, openedAt)
    }

    private fun String.normalizedFolderPath(): String =
        replace('\\', '/').trim('/').split('/').filter { it.isNotBlank() }.joinToString("/")
}
