package dev.anilbeesetti.nextplayer.core.data.repository

import dev.anilbeesetti.nextplayer.core.data.repository.fake.FakeLanServerRepository
import dev.anilbeesetti.nextplayer.core.database.dao.LanFolderBookmarkDao
import dev.anilbeesetti.nextplayer.core.database.entities.LanFolderBookmarkEntity
import dev.anilbeesetti.nextplayer.core.model.LanErrorType
import dev.anilbeesetti.nextplayer.core.model.LanFolderBookmark
import dev.anilbeesetti.nextplayer.core.model.LanResult
import dev.anilbeesetti.nextplayer.core.model.LanServerProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalLanBookmarkRepositoryTest {

    @Test
    fun saveBookmarkSucceedsForExistingServer() = runTest {
        val repository = repositoryWithServer()

        val result = repository.saveBookmark(sampleBookmark)

        assertTrue(result is LanResult.Success)
        assertEquals(listOf(sampleBookmark), repository.observeBookmarks().first())
    }

    @Test
    fun saveBookmarkRejectsDuplicateTarget() = runTest {
        val repository = repositoryWithServer()

        repository.saveBookmark(sampleBookmark)
        val result = repository.saveBookmark(sampleBookmark.copy(id = "bookmark-2"))

        assertEquals(LanErrorType.Duplicate, (result as LanResult.Failure).error.type)
    }

    @Test
    fun saveBookmarkFailsWhenServerUnavailable() = runTest {
        val repository = LocalLanBookmarkRepository(
            lanFolderBookmarkDao = FakeLanFolderBookmarkDao(),
            lanServerRepository = FakeLanServerRepository(),
        )

        val result = repository.saveBookmark(sampleBookmark)

        assertEquals(LanErrorType.PathNotFound, (result as LanResult.Failure).error.type)
    }
}

private suspend fun repositoryWithServer(): LocalLanBookmarkRepository {
    val serverRepository = FakeLanServerRepository()
    serverRepository.saveServer(sampleServer, password = null)
    return LocalLanBookmarkRepository(
        lanFolderBookmarkDao = FakeLanFolderBookmarkDao(),
        lanServerRepository = serverRepository,
    )
}

private val sampleBookmark = LanFolderBookmark(
    id = "bookmark-1",
    serverId = "server-1",
    folderPath = "Movies",
    displayName = "Movies",
    createdAt = 1,
    updatedAt = 1,
)

private val sampleServer = LanServerProfile(
    id = "server-1",
    displayName = "NAS phim",
    host = "192.168.1.10",
    shareName = "Videos",
    username = "viewer",
    credentialKey = null,
    hasPassword = false,
    createdAt = 1,
    updatedAt = 1,
)

private class FakeLanFolderBookmarkDao : LanFolderBookmarkDao {
    private val bookmarks = MutableStateFlow<List<LanFolderBookmarkEntity>>(emptyList())

    override fun observeAll(): MutableStateFlow<List<LanFolderBookmarkEntity>> = bookmarks

    override suspend fun get(id: String): LanFolderBookmarkEntity? =
        bookmarks.value.firstOrNull { it.id == id }

    override suspend fun findDuplicate(
        serverId: String,
        folderPath: String,
        excludeId: String,
    ): LanFolderBookmarkEntity? = bookmarks.value.firstOrNull {
        it.serverId == serverId && it.folderPath == folderPath && it.id != excludeId
    }

    override suspend fun upsert(bookmark: LanFolderBookmarkEntity) {
        bookmarks.value = bookmarks.value.filterNot { it.id == bookmark.id } + bookmark
    }

    override suspend fun delete(id: String) {
        bookmarks.value = bookmarks.value.filterNot { it.id == id }
    }

    override suspend fun markOpened(id: String, openedAt: Long) {
        bookmarks.value = bookmarks.value.map {
            if (it.id == id) it.copy(lastOpenedAt = openedAt) else it
        }
    }
}
