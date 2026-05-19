package dev.anilbeesetti.nextplayer.core.data.repository

import dev.anilbeesetti.nextplayer.core.model.LanErrorType
import dev.anilbeesetti.nextplayer.core.model.LanResult
import dev.anilbeesetti.nextplayer.core.model.LanServerProfile
import dev.anilbeesetti.nextplayer.core.database.dao.LanServerDao
import dev.anilbeesetti.nextplayer.core.database.entities.LanServerEntity
import dev.anilbeesetti.nextplayer.core.lan.security.LanCredentialStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalLanServerRepositoryTest {

    @Test
    fun rejectsServerWithoutRequiredFields() = runTest {
        val repository = LocalLanServerRepository(
            lanServerDao = FakeLanServerDao(),
            credentialStore = FakeLanCredentialStore(),
        )

        val result = repository.saveServer(validProfile.copy(host = ""), password = "secret")

        assertEquals(LanErrorType.InvalidInput, (result as LanResult.Failure).error.type)
    }

    @Test
    fun savesProfileAndMasksPasswordState() = runTest {
        val credentialStore = FakeLanCredentialStore()
        val repository = LocalLanServerRepository(
            lanServerDao = FakeLanServerDao(),
            credentialStore = credentialStore,
        )

        val result = repository.saveServer(validProfile, password = "secret")

        assertTrue(result is LanResult.Success)
        val saved = repository.observeServers().first().single()
        assertTrue(saved.hasPassword)
        assertEquals("secret", credentialStore.getPassword(saved.credentialKey!!))
    }

    @Test
    fun rejectsDuplicateServerProfile() = runTest {
        val repository = LocalLanServerRepository(
            lanServerDao = FakeLanServerDao(),
            credentialStore = FakeLanCredentialStore(),
        )

        repository.saveServer(validProfile, password = "secret")
        val result = repository.saveServer(validProfile.copy(id = "server-2"), password = "secret")

        assertEquals(LanErrorType.Duplicate, (result as LanResult.Failure).error.type)
    }

    @Test
    fun deleteClearsStoredCredential() = runTest {
        val credentialStore = FakeLanCredentialStore()
        val repository = LocalLanServerRepository(
            lanServerDao = FakeLanServerDao(),
            credentialStore = credentialStore,
        )

        val saved = (repository.saveServer(validProfile, password = "secret") as LanResult.Success).value
        repository.deleteServer(saved.id)

        assertTrue(repository.observeServers().first().isEmpty())
        assertFalse(credentialStore.hasPassword(saved.credentialKey!!))
    }
}

private val validProfile = LanServerProfile(
    id = "server-1",
    displayName = "NAS phim",
    host = "192.168.1.10",
    shareName = "Videos",
    username = "viewer",
    credentialKey = "lan-server-server-1",
    hasPassword = false,
    createdAt = 1,
    updatedAt = 1,
)

private class FakeLanServerDao : LanServerDao {
    private val servers = MutableStateFlow<List<LanServerEntity>>(emptyList())

    override fun observeAll(): MutableStateFlow<List<LanServerEntity>> = servers

    override suspend fun get(id: String): LanServerEntity? = servers.value.firstOrNull { it.id == id }

    override suspend fun findDuplicate(
        host: String,
        shareName: String,
        initialPath: String,
        username: String,
        excludeId: String,
    ): LanServerEntity? = servers.value.firstOrNull {
        it.host == host &&
            it.shareName == shareName &&
            it.initialPath == initialPath &&
            it.username == username &&
            it.id != excludeId
    }

    override suspend fun upsert(server: LanServerEntity) {
        servers.value = servers.value.filterNot { it.id == server.id } + server
    }

    override suspend fun delete(id: String) {
        servers.value = servers.value.filterNot { it.id == id }
    }
}

private class FakeLanCredentialStore : LanCredentialStore {
    private val passwords = mutableMapOf<String, String>()

    override suspend fun savePassword(key: String, password: String) {
        passwords[key] = password
    }

    override suspend fun getPassword(key: String): String? = passwords[key]

    override suspend fun hasPassword(key: String): Boolean = passwords.containsKey(key)

    override suspend fun clearPassword(key: String) {
        passwords.remove(key)
    }
}
