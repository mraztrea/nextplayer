package dev.anilbeesetti.nextplayer.core.data.repository

import dev.anilbeesetti.nextplayer.core.lan.security.LanCredentialStore
import dev.anilbeesetti.nextplayer.core.lan.smb.LanClient
import dev.anilbeesetti.nextplayer.core.model.LanError
import dev.anilbeesetti.nextplayer.core.model.LanErrorType
import dev.anilbeesetti.nextplayer.core.model.LanFolder
import dev.anilbeesetti.nextplayer.core.model.LanResult
import javax.inject.Inject

class LocalLanFolderRepository @Inject constructor(
    private val lanServerRepository: LanServerRepository,
    private val credentialStore: LanCredentialStore,
    private val lanClient: LanClient,
) : LanFolderRepository {

    override suspend fun listFolder(serverId: String, folderPath: String): LanResult<LanFolder> {
        val profile = lanServerRepository.getServer(serverId)
            ?: return LanResult.Failure(LanError(LanErrorType.PathNotFound))
        val normalizedPath = folderPath.normalizedFolderPath()
        val password = profile.credentialKey?.let { credentialStore.getPassword(it) }
        return when (val result = lanClient.listFolder(profile, password, normalizedPath)) {
            is LanResult.Success -> LanResult.Success(
                LanFolder(
                    serverId = serverId,
                    path = normalizedPath,
                    displayPath = normalizedPath.ifBlank { profile.displayName },
                    parentPath = normalizedPath.parentPath(),
                    items = result.value,
                    loadedAt = System.currentTimeMillis(),
                ),
            )
            is LanResult.Failure -> LanResult.Failure(result.error)
        }
    }

    private fun String.normalizedFolderPath(): String =
        replace('\\', '/').trim('/').split('/').filter { it.isNotBlank() }.joinToString("/")

    private fun String.parentPath(): String? =
        substringBeforeLast("/", missingDelimiterValue = "").ifBlank { null }
}
