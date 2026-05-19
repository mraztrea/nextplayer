package dev.anilbeesetti.nextplayer.feature.player.lan

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import dev.anilbeesetti.nextplayer.core.data.repository.LanServerRepository
import dev.anilbeesetti.nextplayer.core.lan.security.LanCredentialStore
import dev.anilbeesetti.nextplayer.core.lan.smb.LanClient
import dev.anilbeesetti.nextplayer.core.lan.smb.SeekableLanFile
import dev.anilbeesetti.nextplayer.core.model.LanFolder
import dev.anilbeesetti.nextplayer.core.model.LanPlaybackDescriptor
import dev.anilbeesetti.nextplayer.core.model.LanResult
import dev.anilbeesetti.nextplayer.core.model.LanServerProfile
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@UnstableApi
@RunWith(RobolectricTestRunner::class)
class SmbMediaDataSourceTest {

    @Test
    fun openReturnsRemainingLength() {
        val dataSource = dataSource("abcdef".toByteArray())

        val length = dataSource.open(dataSpec(position = 0))

        assertEquals(6, length)
    }

    @Test
    fun readCopiesBytes() {
        val dataSource = dataSource("abcdef".toByteArray())
        val buffer = ByteArray(3)

        dataSource.open(dataSpec(position = 0))
        val read = dataSource.read(buffer, 0, buffer.size)

        assertEquals(3, read)
        assertArrayEquals("abc".toByteArray(), buffer)
    }

    @Test
    fun seekReadsFromRequestedPosition() {
        val dataSource = dataSource("abcdef".toByteArray())
        val buffer = ByteArray(2)

        dataSource.open(dataSpec(position = 2))
        dataSource.read(buffer, 0, buffer.size)

        assertArrayEquals("cd".toByteArray(), buffer)
    }

    @Test(expected = IOException::class)
    fun readInterruptionThrowsIOException() {
        val dataSource = dataSource("abcdef".toByteArray(), interruptReads = true)

        dataSource.open(dataSpec(position = 0))
        dataSource.read(ByteArray(1), 0, 1)
    }
}

private fun dataSource(
    bytes: ByteArray,
    interruptReads: Boolean = false,
) = SmbMediaDataSource(
    lanServerRepository = FakeLanServerRepository(),
    credentialStore = FakeLanCredentialStore(),
    lanClient = FakeLanClient(bytes, interruptReads),
)

private fun dataSpec(position: Long): DataSpec =
    DataSpec.Builder()
        .setUri(Uri.parse(LanPlaybackDescriptor("server-1", "clip.mp4", "clip.mp4").uriString))
        .setPosition(position)
        .setLength(C.LENGTH_UNSET.toLong())
        .build()

private class FakeLanClient(
    private val bytes: ByteArray,
    private val interruptReads: Boolean,
) : LanClient {
    override suspend fun listFolder(
        profile: LanServerProfile,
        password: String?,
        folderPath: String,
    ): LanResult<List<dev.anilbeesetti.nextplayer.core.model.LanMediaItem>> =
        LanResult.Success(emptyList())

    override fun openFile(
        profile: LanServerProfile,
        password: String?,
        mediaPath: String,
    ): SeekableLanFile = ByteArraySeekableLanFile(bytes, interruptReads)
}

private class ByteArraySeekableLanFile(
    private val bytes: ByteArray,
    private val interruptReads: Boolean,
) : SeekableLanFile {
    override val length: Long = bytes.size.toLong()

    override fun read(position: Long, buffer: ByteArray, offset: Int, length: Int): Int {
        if (interruptReads) throw IOException("interrupted")
        if (position >= bytes.size) return C.RESULT_END_OF_INPUT
        val count = minOf(length, bytes.size - position.toInt())
        bytes.copyInto(buffer, offset, position.toInt(), position.toInt() + count)
        return count
    }

    override fun close() = Unit
}

private class FakeLanServerRepository : LanServerRepository {
    override fun observeServers(): Flow<List<LanServerProfile>> = flowOf(listOf(profile))

    override suspend fun getServer(id: String): LanServerProfile? = profile.takeIf { it.id == id }

    override suspend fun saveServer(profile: LanServerProfile, password: String?): LanResult<LanServerProfile> =
        LanResult.Success(profile)

    override suspend fun deleteServer(id: String) = Unit

    private val profile = LanServerProfile(
        id = "server-1",
        displayName = "NAS phim",
        host = "192.168.1.10",
        shareName = "Videos",
        username = "viewer",
        credentialKey = "credential-1",
        hasPassword = true,
        createdAt = 1,
        updatedAt = 1,
    )
}

private class FakeLanCredentialStore : LanCredentialStore {
    override suspend fun savePassword(key: String, password: String) = Unit

    override suspend fun getPassword(key: String): String? = "secret"

    override suspend fun hasPassword(key: String): Boolean = true

    override suspend fun clearPassword(key: String) = Unit
}
