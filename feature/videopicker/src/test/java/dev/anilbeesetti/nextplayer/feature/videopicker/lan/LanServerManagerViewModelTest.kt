package dev.anilbeesetti.nextplayer.feature.videopicker.lan

import dev.anilbeesetti.nextplayer.core.data.repository.fake.FakeLanServerRepository
import dev.anilbeesetti.nextplayer.core.domain.lan.ManageLanServersUseCases
import dev.anilbeesetti.nextplayer.core.model.LanServerProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class LanServerManagerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun addServerValidatesRequiredFields() = runTest {
        val viewModel = LanServerManagerViewModel(FakeLanServerRepository().asUseCases())

        viewModel.onEvent(LanServerManagerEvent.SaveServer)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.hostError)
        assertTrue(viewModel.uiState.value.shareNameError)
        assertTrue(viewModel.uiState.value.displayNameError)
    }

    @Test
    fun addServerStoresServerWithoutExposingPassword() = runTest {
        val repository = FakeLanServerRepository()
        val viewModel = LanServerManagerViewModel(repository.asUseCases())

        viewModel.onEvent(LanServerManagerEvent.DisplayNameChanged("NAS phim"))
        viewModel.onEvent(LanServerManagerEvent.HostChanged("192.168.1.10"))
        viewModel.onEvent(LanServerManagerEvent.ShareNameChanged("Videos"))
        viewModel.onEvent(LanServerManagerEvent.UsernameChanged("viewer"))
        viewModel.onEvent(LanServerManagerEvent.PasswordChanged("secret"))
        viewModel.onEvent(LanServerManagerEvent.SaveServer)
        advanceUntilIdle()

        val saved = repository.observeServers().value.single()
        assertEquals("NAS phim", saved.displayName)
        assertTrue(saved.hasPassword)
        assertEquals("", viewModel.uiState.value.password)
    }

    @Test
    fun editServerFillsFormWithoutExposingPassword() = runTest {
        val repository = FakeLanServerRepository()
        repository.saveServer(sampleProfile, password = "secret")
        val viewModel = LanServerManagerViewModel(repository.asUseCases())
        advanceUntilIdle()

        viewModel.onEvent(LanServerManagerEvent.EditServer(repository.servers.value.single()))

        assertEquals("NAS phim", viewModel.uiState.value.displayName)
        assertEquals("192.168.1.10", viewModel.uiState.value.host)
        assertEquals("", viewModel.uiState.value.password)
        assertTrue(viewModel.uiState.value.passwordSaved)
    }

    @Test
    fun deleteServerRemovesSavedProfile() = runTest {
        val repository = FakeLanServerRepository()
        repository.saveServer(sampleProfile, password = "secret")
        val viewModel = LanServerManagerViewModel(repository.asUseCases())
        advanceUntilIdle()

        viewModel.onEvent(LanServerManagerEvent.DeleteServer(sampleProfile.id))
        advanceUntilIdle()

        assertTrue(repository.servers.value.isEmpty())
    }
}

private fun FakeLanServerRepository.asUseCases() = ManageLanServersUseCases(this)

private val sampleProfile = LanServerProfile(
    id = "server-1",
    displayName = "NAS phim",
    host = "192.168.1.10",
    shareName = "Videos",
    username = "viewer",
    credentialKey = null,
    hasPassword = false,
    createdAt = 1,
    updatedAt = 1,
)

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val dispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
