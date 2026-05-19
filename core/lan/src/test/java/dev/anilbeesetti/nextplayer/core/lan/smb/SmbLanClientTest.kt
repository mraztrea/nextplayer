package dev.anilbeesetti.nextplayer.core.lan.smb

import dev.anilbeesetti.nextplayer.core.model.LanErrorType
import dev.anilbeesetti.nextplayer.core.model.LanMediaItemType
import dev.anilbeesetti.nextplayer.core.model.LanResult
import dev.anilbeesetti.nextplayer.core.model.LanServerProfile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SmbLanClientTest {

    @Test
    fun listFolderReturnsFoldersAndVideosOnly() = runTest {
        val client = SmbLanClient(
            fileSystem = FakeSmbFileSystem(
                entries = listOf(
                    SmbLanEntry("Movies", "Movies", isDirectory = true),
                    SmbLanEntry("clip.mp4", "clip.mp4", isDirectory = false, sizeBytes = 100),
                    SmbLanEntry("notes.txt", "notes.txt", isDirectory = false, sizeBytes = 10),
                ),
            ),
            ioDispatcher = UnconfinedTestDispatcher(testScheduler),
        )

        val result = client.listFolder(sampleProfile, password = "secret", folderPath = "")

        val items = (result as LanResult.Success).value
        assertEquals(listOf("Movies", "clip.mp4"), items.map { it.name })
        assertEquals(LanMediaItemType.Folder, items.first().type)
        assertTrue(items.last().playbackUri!!.startsWith("nextplayer-smb://server-1/"))
    }

    @Test
    fun listFolderMapsAuthenticationError() = runTest {
        val result = failingClient(LanErrorType.AuthenticationFailed)
            .listFolder(sampleProfile, password = "wrong", folderPath = "")

        assertEquals(LanErrorType.AuthenticationFailed, (result as LanResult.Failure).error.type)
    }

    @Test
    fun listFolderMapsOfflineError() = runTest {
        val result = failingClient(LanErrorType.ServerUnreachable)
            .listFolder(sampleProfile, password = "secret", folderPath = "")

        assertEquals(LanErrorType.ServerUnreachable, (result as LanResult.Failure).error.type)
    }

    @Test
    fun listFolderMapsPermissionError() = runTest {
        val result = failingClient(LanErrorType.PermissionDenied)
            .listFolder(sampleProfile, password = "secret", folderPath = "private")

        assertEquals(LanErrorType.PermissionDenied, (result as LanResult.Failure).error.type)
    }

    private fun failingClient(errorType: LanErrorType): SmbLanClient {
        return SmbLanClient(
            fileSystem = FakeSmbFileSystem(errorType = errorType),
            ioDispatcher = UnconfinedTestDispatcher(),
        )
    }
}

private val sampleProfile = LanServerProfile(
    id = "server-1",
    displayName = "NAS phim",
    host = "192.168.1.10",
    shareName = "Videos",
    username = "viewer",
    credentialKey = "lan-server-server-1",
    hasPassword = true,
    createdAt = 1,
    updatedAt = 1,
)

private class FakeSmbFileSystem(
    private val entries: List<SmbLanEntry> = emptyList(),
    private val errorType: LanErrorType? = null,
) : SmbFileSystem {
    override fun list(
        profile: LanServerProfile,
        password: String?,
        folderPath: String,
    ): List<SmbLanEntry> {
        errorType?.let { throw LanClientException(it) }
        return entries
    }

    override fun openFile(
        profile: LanServerProfile,
        password: String?,
        mediaPath: String,
    ): SeekableLanFile = error("Not needed")
}
