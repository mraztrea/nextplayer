package dev.anilbeesetti.nextplayer.core.data.repository

import dev.anilbeesetti.nextplayer.core.database.dao.LanServerDao
import dev.anilbeesetti.nextplayer.core.data.mappers.asEntity
import dev.anilbeesetti.nextplayer.core.data.mappers.asExternalModel
import dev.anilbeesetti.nextplayer.core.lan.security.LanCredentialStore
import dev.anilbeesetti.nextplayer.core.model.LanError
import dev.anilbeesetti.nextplayer.core.model.LanErrorType
import dev.anilbeesetti.nextplayer.core.model.LanResult
import dev.anilbeesetti.nextplayer.core.model.LanServerProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class LocalLanServerRepository @Inject constructor(
    private val lanServerDao: LanServerDao,
    private val credentialStore: LanCredentialStore,
) : LanServerRepository {

    override fun observeServers(): Flow<List<LanServerProfile>> =
        lanServerDao.observeAll().map { servers -> servers.map { it.asExternalModel() } }

    override suspend fun getServer(id: String): LanServerProfile? =
        lanServerDao.get(id)?.asExternalModel()

    override suspend fun saveServer(
        profile: LanServerProfile,
        password: String?,
    ): LanResult<LanServerProfile> {
        val normalized = profile.normalized()
        if (normalized.displayName.isBlank() || normalized.host.isBlank() || normalized.shareName.isBlank()) {
            return LanResult.Failure(LanError(LanErrorType.InvalidInput))
        }
        if (
            lanServerDao.findDuplicate(
                host = normalized.host,
                shareName = normalized.shareName,
                initialPath = normalized.initialPath,
                username = normalized.username,
                excludeId = normalized.id,
            ) != null
        ) {
            return LanResult.Failure(LanError(LanErrorType.Duplicate))
        }

        val credentialKey = normalized.credentialKey ?: "lan-server-${normalized.id}"
        val hasPassword = when {
            password != null -> {
                if (password.isBlank()) {
                    credentialStore.clearPassword(credentialKey)
                    false
                } else {
                    credentialStore.savePassword(credentialKey, password)
                    true
                }
            }

            else -> credentialStore.hasPassword(credentialKey)
        }
        val saved = normalized.copy(
            credentialKey = credentialKey,
            hasPassword = hasPassword,
        )
        lanServerDao.upsert(saved.asEntity())
        return LanResult.Success(saved)
    }

    override suspend fun deleteServer(id: String) {
        val existing = lanServerDao.get(id)?.asExternalModel()
        existing?.credentialKey?.let { credentialStore.clearPassword(it) }
        lanServerDao.delete(id)
    }

    private fun LanServerProfile.normalized(): LanServerProfile = copy(
        displayName = displayName.trim(),
        host = host.trim(),
        shareName = shareName.trim().trim('/'),
        initialPath = initialPath.trim().trim('/'),
        username = username.trim(),
    )
}
