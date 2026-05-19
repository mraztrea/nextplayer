package dev.anilbeesetti.nextplayer.core.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.anilbeesetti.nextplayer.core.database.MediaDatabase
import dev.anilbeesetti.nextplayer.core.database.entities.LanServerEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class LanServerDaoTest {

    private lateinit var db: MediaDatabase
    private lateinit var dao: LanServerDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, MediaDatabase::class.java).build()
        dao = db.lanServerDao()
    }

    @Test
    fun upsertPersistsServerProfile() = runTest {
        dao.upsert(sampleServer)

        assertEquals(listOf(sampleServer), dao.observeAll().first())
        assertEquals(sampleServer, dao.get("server-1"))
    }

    @Test
    fun deleteRemovesServerProfile() = runTest {
        dao.upsert(sampleServer)

        dao.delete("server-1")

        assertNull(dao.get("server-1"))
    }
}

private val sampleServer = LanServerEntity(
    id = "server-1",
    displayName = "NAS phim",
    host = "192.168.1.10",
    shareName = "Videos",
    initialPath = "",
    username = "viewer",
    credentialKey = "lan-server-server-1",
    hasPassword = true,
    createdAt = 1,
    updatedAt = 1,
    lastConnectedAt = null,
)
