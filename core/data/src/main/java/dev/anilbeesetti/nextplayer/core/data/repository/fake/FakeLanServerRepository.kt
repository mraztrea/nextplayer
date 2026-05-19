package dev.anilbeesetti.nextplayer.core.data.repository.fake

import dev.anilbeesetti.nextplayer.core.data.repository.LanServerRepository
import dev.anilbeesetti.nextplayer.core.model.LanError
import dev.anilbeesetti.nextplayer.core.model.LanErrorType
import dev.anilbeesetti.nextplayer.core.model.LanResult
import dev.anilbeesetti.nextplayer.core.model.LanServerProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeLanServerRepository : LanServerRepository {
    val servers = MutableStateFlow<List<LanServerProfile>>(emptyList())
    val passwords = mutableMapOf<String, String>()

    override fun observeServers(): StateFlow<List<LanServerProfile>> = servers

    override suspend fun getServer(id: String): LanServerProfile? =
        servers.value.firstOrNull { it.id == id }

    override suspend fun saveServer(
        profile: LanServerProfile,
        password: String?,
    ): LanResult<LanServerProfile> {
        if (profile.displayName.isBlank() || profile.host.isBlank() || profile.shareName.isBlank()) {
            return LanResult.Failure(LanError(LanErrorType.InvalidInput))
        }
        val credentialKey = profile.credentialKey ?: "lan-server-${profile.id}"
        if (!password.isNullOrBlank()) {
            passwords[credentialKey] = password
        }
        val saved = profile.copy(
            credentialKey = credentialKey,
            hasPassword = !password.isNullOrBlank() || passwords.containsKey(credentialKey),
        )
        servers.value = servers.value.filterNot { it.id == saved.id } + saved
        return LanResult.Success(saved)
    }

    override suspend fun deleteServer(id: String) {
        servers.value = servers.value.filterNot { it.id == id }
    }
}
