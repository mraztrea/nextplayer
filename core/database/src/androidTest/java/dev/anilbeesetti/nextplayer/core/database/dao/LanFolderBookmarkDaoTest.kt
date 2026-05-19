package dev.anilbeesetti.nextplayer.core.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.anilbeesetti.nextplayer.core.database.MediaDatabase
import dev.anilbeesetti.nextplayer.core.database.entities.LanFolderBookmarkEntity
import dev.anilbeesetti.nextplayer.core.database.entities.LanServerEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class LanFolderBookmarkDaoTest {

    private lateinit var db: MediaDatabase
    private lateinit var serverDao: LanServerDao
    private lateinit var bookmarkDao: LanFolderBookmarkDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, MediaDatabase::class.java).build()
        serverDao = db.lanServerDao()
        bookmarkDao = db.lanFolderBookmarkDao()
    }

    @Test
    fun upsertPersistsBookmark() = runTest {
        serverDao.upsert(sampleServer)

        bookmarkDao.upsert(sampleBookmark)

        assertEquals(listOf(sampleBookmark), bookmarkDao.observeAll().first())
    }

    @Test
    fun findDuplicateReturnsExistingBookmark() = runTest {
        serverDao.upsert(sampleServer)
        bookmarkDao.upsert(sampleBookmark)

        val duplicate = bookmarkDao.findDuplicate("server-1", "Movies", excludeId = "bookmark-2")

        assertEquals(sampleBookmark, duplicate)
    }

    @Test
    fun deleteRemovesBookmark() = runTest {
        serverDao.upsert(sampleServer)
        bookmarkDao.upsert(sampleBookmark)

        bookmarkDao.delete("bookmark-1")

        assertNull(bookmarkDao.get("bookmark-1"))
    }
}

private val sampleBookmark = LanFolderBookmarkEntity(
    id = "bookmark-1",
    serverId = "server-1",
    folderPath = "Movies",
    displayName = "Movies",
    createdAt = 1,
    updatedAt = 1,
    lastOpenedAt = null,
)

private val sampleServer = LanServerEntity(
    id = "server-1",
    displayName = "NAS phim",
    host = "192.168.1.10",
    shareName = "Videos",
    initialPath = "",
    username = "viewer",
    credentialKey = null,
    hasPassword = false,
    createdAt = 1,
    updatedAt = 1,
    lastConnectedAt = null,
)
