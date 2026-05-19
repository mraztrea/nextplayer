package dev.anilbeesetti.nextplayer.feature.player.lan

import android.content.Context
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.TransferListener
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.anilbeesetti.nextplayer.core.data.repository.LanServerRepository
import dev.anilbeesetti.nextplayer.core.lan.security.LanCredentialStore
import dev.anilbeesetti.nextplayer.core.lan.smb.LanClient
import dev.anilbeesetti.nextplayer.core.lan.smb.LanClientException
import dev.anilbeesetti.nextplayer.core.lan.smb.SeekableLanFile
import dev.anilbeesetti.nextplayer.core.model.LanErrorType
import dev.anilbeesetti.nextplayer.core.model.LanPlaybackDescriptor
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.runBlocking

@UnstableApi
class SmbMediaDataSource(
    private val lanServerRepository: LanServerRepository,
    private val credentialStore: LanCredentialStore,
    private val lanClient: LanClient,
) : BaseDataSource(true) {

    private var uri: Uri? = null
    private var file: SeekableLanFile? = null
    private var readPosition: Long = 0
    private var bytesRemaining: Long = 0

    override fun open(dataSpec: DataSpec): Long {
        transferInitializing(dataSpec)
        uri = dataSpec.uri
        val descriptor = LanPlaybackDescriptor.parse(dataSpec.uri.toString())
            ?: throw IOException("Unsupported LAN URI: ${dataSpec.uri}")
        val profile = runBlocking { lanServerRepository.getServer(descriptor.serverId) }
            ?: throw LanClientException(LanErrorType.PathNotFound)
        val password = runBlocking { profile.credentialKey?.let { credentialStore.getPassword(it) } }
        file = lanClient.openFile(profile, password, descriptor.mediaPath)
        readPosition = dataSpec.position
        bytesRemaining = if (dataSpec.length == C.LENGTH_UNSET.toLong()) {
            (file?.length ?: 0L) - readPosition
        } else {
            dataSpec.length
        }.coerceAtLeast(0)
        transferStarted(dataSpec)
        return bytesRemaining
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (bytesRemaining == 0L) return C.RESULT_END_OF_INPUT
        val readLength = minOf(length.toLong(), bytesRemaining).toInt()
        val read = try {
            file?.read(readPosition, buffer, offset, readLength) ?: C.RESULT_END_OF_INPUT
        } catch (error: IOException) {
            throw error
        } catch (error: Exception) {
            throw IOException(error)
        }
        if (read <= 0) return C.RESULT_END_OF_INPUT
        readPosition += read
        bytesRemaining -= read
        bytesTransferred(read)
        return read
    }

    override fun getUri(): Uri? = uri

    override fun close() {
        uri = null
        runCatching { file?.close() }
        file = null
        transferEnded()
    }

    class Factory @Inject constructor(
        private val lanServerRepository: LanServerRepository,
        private val credentialStore: LanCredentialStore,
        private val lanClient: LanClient,
    ) : DataSource.Factory {
        override fun createDataSource(): DataSource =
            SmbMediaDataSource(lanServerRepository, credentialStore, lanClient)
    }
}

@UnstableApi
class SmbAwareDataSourceFactory @Inject constructor(
    @ApplicationContext context: Context,
    private val smbMediaDataSourceFactory: SmbMediaDataSource.Factory,
) : DataSource.Factory {
    private val defaultDataSourceFactory = DefaultDataSource.Factory(context)

    override fun createDataSource(): DataSource {
        return RoutingDataSource(
            defaultDataSource = defaultDataSourceFactory.createDataSource(),
            smbDataSource = smbMediaDataSourceFactory.createDataSource(),
        )
    }
}

@UnstableApi
private class RoutingDataSource(
    private val defaultDataSource: DataSource,
    private val smbDataSource: DataSource,
) : DataSource {
    private var activeDataSource: DataSource? = null

    override fun addTransferListener(transferListener: TransferListener) {
        defaultDataSource.addTransferListener(transferListener)
        smbDataSource.addTransferListener(transferListener)
    }

    override fun open(dataSpec: DataSpec): Long {
        activeDataSource = if (dataSpec.uri.scheme == LanPlaybackDescriptor.SCHEME) {
            smbDataSource
        } else {
            defaultDataSource
        }
        return activeDataSource!!.open(dataSpec)
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
        activeDataSource?.read(buffer, offset, length) ?: C.RESULT_END_OF_INPUT

    override fun getUri(): Uri? = activeDataSource?.uri

    override fun getResponseHeaders(): Map<String, List<String>> =
        activeDataSource?.responseHeaders ?: emptyMap()

    override fun close() {
        activeDataSource?.close()
        activeDataSource = null
    }
}
