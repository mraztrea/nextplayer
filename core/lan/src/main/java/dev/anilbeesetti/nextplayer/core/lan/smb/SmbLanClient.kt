package dev.anilbeesetti.nextplayer.core.lan.smb

import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.mserref.NtStatus
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2CreateOptions
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.mssmb2.SMBApiException
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.share.DiskShare
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.anilbeesetti.nextplayer.core.common.Dispatcher
import dev.anilbeesetti.nextplayer.core.common.NextDispatchers
import dev.anilbeesetti.nextplayer.core.model.LanError
import dev.anilbeesetti.nextplayer.core.model.LanErrorType
import dev.anilbeesetti.nextplayer.core.model.LanMediaItem
import dev.anilbeesetti.nextplayer.core.model.LanMediaItemType
import dev.anilbeesetti.nextplayer.core.model.LanPlaybackDescriptor
import dev.anilbeesetti.nextplayer.core.model.LanResult
import dev.anilbeesetti.nextplayer.core.model.LanServerProfile
import java.io.Closeable
import java.io.IOException
import java.nio.ByteBuffer
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

interface LanClient {
    suspend fun listFolder(
        profile: LanServerProfile,
        password: String?,
        folderPath: String,
    ): LanResult<List<LanMediaItem>>

    fun openFile(
        profile: LanServerProfile,
        password: String?,
        mediaPath: String,
    ): SeekableLanFile
}

interface SmbFileSystem {
    fun list(
        profile: LanServerProfile,
        password: String?,
        folderPath: String,
    ): List<SmbLanEntry>

    fun openFile(
        profile: LanServerProfile,
        password: String?,
        mediaPath: String,
    ): SeekableLanFile
}

interface SeekableLanFile : Closeable {
    val length: Long

    @Throws(IOException::class)
    fun read(position: Long, buffer: ByteArray, offset: Int, length: Int): Int
}

data class SmbLanEntry(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val sizeBytes: Long? = null,
    val modifiedAt: Long? = null,
)

class LanClientException(
    val type: LanErrorType,
    cause: Throwable? = null,
) : IOException(type.name, cause)

@Singleton
class SmbLanClient @Inject constructor(
    private val fileSystem: SmbFileSystem,
    @param:Dispatcher(NextDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : LanClient {

    override suspend fun listFolder(
        profile: LanServerProfile,
        password: String?,
        folderPath: String,
    ): LanResult<List<LanMediaItem>> = withContext(ioDispatcher) {
        val safeFolderPath = normalizePath(folderPath)
            ?: return@withContext LanResult.Failure(LanError(LanErrorType.InvalidInput))
        try {
            val items = fileSystem.list(profile, password, safeFolderPath)
                .filter { it.isDirectory || it.name.isVideoFile() }
                .map { entry ->
                    LanMediaItem(
                        serverId = profile.id,
                        path = entry.path,
                        name = entry.name,
                        type = if (entry.isDirectory) LanMediaItemType.Folder else LanMediaItemType.Video,
                        sizeBytes = entry.sizeBytes,
                        modifiedAt = entry.modifiedAt,
                        playbackUri = if (entry.isDirectory) {
                            null
                        } else {
                            LanPlaybackDescriptor(
                                serverId = profile.id,
                                mediaPath = entry.path,
                                displayName = entry.name,
                                sizeBytes = entry.sizeBytes,
                                modifiedAt = entry.modifiedAt,
                            ).uriString
                        },
                    )
                }
                .sortedWith(compareBy<LanMediaItem> { it.type != LanMediaItemType.Folder }.thenBy { it.name.lowercase() })
            LanResult.Success(items)
        } catch (error: LanClientException) {
            LanResult.Failure(LanError(error.type, cause = error))
        } catch (error: Exception) {
            LanResult.Failure(LanError(mapError(error), cause = error))
        }
    }

    override fun openFile(
        profile: LanServerProfile,
        password: String?,
        mediaPath: String,
    ): SeekableLanFile {
        val safeMediaPath = normalizePath(mediaPath) ?: throw LanClientException(LanErrorType.InvalidInput)
        if (!safeMediaPath.isVideoFile()) throw LanClientException(LanErrorType.UnsupportedMedia)
        return try {
            fileSystem.openFile(profile, password, safeMediaPath)
        } catch (error: LanClientException) {
            throw error
        } catch (error: Exception) {
            throw LanClientException(mapError(error), error)
        }
    }

    private fun normalizePath(path: String): String? {
        val normalized = path.replace('\\', '/').trim('/').split('/')
            .filter { it.isNotBlank() }
        if (normalized.any { it == "." || it == ".." }) return null
        return normalized.joinToString("/")
    }

    private fun String.isVideoFile(): Boolean {
        val extension = substringAfterLast('.', missingDelimiterValue = "").lowercase()
        return extension in videoExtensions
    }

    private fun mapError(error: Throwable): LanErrorType {
        return when (error) {
            is SMBApiException -> when (error.status) {
                NtStatus.STATUS_LOGON_FAILURE -> LanErrorType.AuthenticationFailed
                NtStatus.STATUS_ACCESS_DENIED -> LanErrorType.PermissionDenied
                NtStatus.STATUS_BAD_NETWORK_NAME -> LanErrorType.ShareNotFound
                NtStatus.STATUS_OBJECT_NAME_NOT_FOUND,
                NtStatus.STATUS_OBJECT_PATH_NOT_FOUND,
                NtStatus.STATUS_NO_SUCH_FILE,
                -> LanErrorType.PathNotFound
                else -> LanErrorType.Unknown
            }
            is IOException -> LanErrorType.ServerUnreachable
            else -> LanErrorType.Unknown
        }
    }

    private companion object {
        val videoExtensions = setOf("mp4", "mkv", "avi", "mov", "webm", "m4v", "3gp", "ts")
    }
}

@Singleton
class SmbjFileSystem @Inject constructor() : SmbFileSystem {
    override fun list(
        profile: LanServerProfile,
        password: String?,
        folderPath: String,
    ): List<SmbLanEntry> = withShare(profile, password) { share ->
        share.openDirectory(
            folderPath,
            setOf(AccessMask.FILE_LIST_DIRECTORY, AccessMask.FILE_READ_ATTRIBUTES),
            null,
            SMB2ShareAccess.ALL,
            SMB2CreateDisposition.FILE_OPEN,
            setOf(SMB2CreateOptions.FILE_DIRECTORY_FILE),
        ).use { directory ->
            directory.list()
                .filterNot { it.fileName == "." || it.fileName == ".." }
                .map { it.toEntry(folderPath) }
        }
    }

    override fun openFile(
        profile: LanServerProfile,
        password: String?,
        mediaPath: String,
    ): SeekableLanFile = withShare(profile, password) { share ->
        val file = share.openFile(
            mediaPath,
            setOf(AccessMask.FILE_READ_DATA, AccessMask.FILE_READ_ATTRIBUTES),
            null,
            SMB2ShareAccess.ALL,
            SMB2CreateDisposition.FILE_OPEN,
            setOf(SMB2CreateOptions.FILE_NON_DIRECTORY_FILE, SMB2CreateOptions.FILE_RANDOM_ACCESS),
        )
        SmbjSeekableLanFile(file)
    }

    private fun <T> withShare(
        profile: LanServerProfile,
        password: String?,
        block: (DiskShare) -> T,
    ): T {
        SMBClient().use { client ->
            client.connect(profile.host).use { connection ->
                val auth = if (profile.username.isBlank() && password.isNullOrBlank()) {
                    AuthenticationContext.guest()
                } else {
                    AuthenticationContext(profile.username, password.orEmpty().toCharArray(), null)
                }
                val session = connection.authenticate(auth)
                val share = session.connectShare(profile.shareName) as DiskShare
                share.use {
                    return block(it)
                }
            }
        }
    }

    private fun FileIdBothDirectoryInformation.toEntry(parentPath: String): SmbLanEntry {
        val childPath = listOf(parentPath, fileName).filter { it.isNotBlank() }.joinToString("/")
        val isDirectory = fileAttributes and DIRECTORY_ATTRIBUTE != 0L
        return SmbLanEntry(
            name = fileName,
            path = childPath,
            isDirectory = isDirectory,
            sizeBytes = endOfFile.takeIf { !isDirectory },
            modifiedAt = lastWriteTime?.toEpochMillis(),
        )
    }

    private companion object {
        const val DIRECTORY_ATTRIBUTE = 0x10L
    }
}

private class SmbjSeekableLanFile(
    private val file: com.hierynomus.smbj.share.File,
) : SeekableLanFile {
    override val length: Long = file.fileInformation.standardInformation.endOfFile

    override fun read(position: Long, buffer: ByteArray, offset: Int, length: Int): Int {
        val targetBuffer = ByteBuffer.wrap(buffer, offset, length)
        val read = file.read(targetBuffer, position)
        return read.toInt()
    }

    override fun close() {
        file.close()
    }
}

@Module
@InstallIn(SingletonComponent::class)
interface SmbLanModule {
    @Binds
    fun bindLanClient(client: SmbLanClient): LanClient

    @Binds
    fun bindSmbFileSystem(fileSystem: SmbjFileSystem): SmbFileSystem
}
