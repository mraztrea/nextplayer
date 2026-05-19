package dev.anilbeesetti.nextplayer.core.domain.lan

import dev.anilbeesetti.nextplayer.core.data.repository.LanServerRepository
import dev.anilbeesetti.nextplayer.core.model.LanResult
import dev.anilbeesetti.nextplayer.core.model.LanServerProfile
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ManageLanServersUseCases @Inject constructor(
    private val lanServerRepository: LanServerRepository,
) {
    fun observeServers(): Flow<List<LanServerProfile>> = lanServerRepository.observeServers()

    suspend fun getServer(id: String): LanServerProfile? = lanServerRepository.getServer(id)

    suspend fun saveServer(
        profile: LanServerProfile,
        password: String?,
    ): LanResult<LanServerProfile> = lanServerRepository.saveServer(profile, password)

    suspend fun deleteServer(id: String) = lanServerRepository.deleteServer(id)
}
