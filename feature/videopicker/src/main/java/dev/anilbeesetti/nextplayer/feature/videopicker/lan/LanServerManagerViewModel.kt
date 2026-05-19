package dev.anilbeesetti.nextplayer.feature.videopicker.lan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.anilbeesetti.nextplayer.core.domain.lan.ManageLanServersUseCases
import dev.anilbeesetti.nextplayer.core.model.LanResult
import dev.anilbeesetti.nextplayer.core.model.LanServerProfile
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class LanServerManagerViewModel @Inject constructor(
    private val manageLanServersUseCases: ManageLanServersUseCases,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LanServerManagerUiState())
    val uiState: StateFlow<LanServerManagerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            manageLanServersUseCases.observeServers().collect { servers ->
                _uiState.update { it.copy(servers = servers) }
            }
        }
    }

    fun onEvent(event: LanServerManagerEvent) {
        when (event) {
            is LanServerManagerEvent.DisplayNameChanged -> updateForm { copy(displayName = event.value, displayNameError = false) }
            is LanServerManagerEvent.HostChanged -> updateForm { copy(host = event.value, hostError = false) }
            is LanServerManagerEvent.ShareNameChanged -> updateForm { copy(shareName = event.value, shareNameError = false) }
            is LanServerManagerEvent.InitialPathChanged -> updateForm { copy(initialPath = event.value) }
            is LanServerManagerEvent.UsernameChanged -> updateForm { copy(username = event.value) }
            is LanServerManagerEvent.PasswordChanged -> updateForm { copy(password = event.value) }
            is LanServerManagerEvent.EditServer -> editServer(event.server)
            is LanServerManagerEvent.DeleteServer -> deleteServer(event.serverId)
            LanServerManagerEvent.SaveServer -> saveServer()
            LanServerManagerEvent.ClearForm -> clearForm()
        }
    }

    private fun saveServer() {
        val current = uiState.value
        val displayNameError = current.displayName.isBlank()
        val hostError = current.host.isBlank()
        val shareNameError = current.shareName.isBlank()
        if (displayNameError || hostError || shareNameError) {
            _uiState.value = current.copy(
                displayNameError = displayNameError,
                hostError = hostError,
                shareNameError = shareNameError,
            )
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(saving = true, error = null) }
            val now = System.currentTimeMillis()
            val serverId = current.editingServerId ?: UUID.randomUUID().toString()
            val existing = current.editingServerId?.let { manageLanServersUseCases.getServer(it) }
            val profile = LanServerProfile(
                id = serverId,
                displayName = current.displayName,
                host = current.host,
                shareName = current.shareName,
                initialPath = current.initialPath,
                username = current.username,
                credentialKey = existing?.credentialKey,
                hasPassword = existing?.hasPassword ?: false,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
                lastConnectedAt = existing?.lastConnectedAt,
            )
            when (val result = manageLanServersUseCases.saveServer(profile, current.password.takeIf { it.isNotEmpty() })) {
                is LanResult.Success -> clearForm()
                is LanResult.Failure -> _uiState.update { it.copy(saving = false, error = result.error.type.name) }
            }
        }
    }

    private fun editServer(server: LanServerProfile) {
        _uiState.update {
            it.copy(
                editingServerId = server.id,
                displayName = server.displayName,
                host = server.host,
                shareName = server.shareName,
                initialPath = server.initialPath,
                username = server.username,
                password = "",
                passwordSaved = server.hasPassword,
                displayNameError = false,
                hostError = false,
                shareNameError = false,
                error = null,
            )
        }
    }

    private fun deleteServer(serverId: String) {
        viewModelScope.launch {
            manageLanServersUseCases.deleteServer(serverId)
        }
    }

    private fun updateForm(block: LanServerFormState.() -> LanServerFormState) {
        _uiState.update {
            val updated = LanServerFormState(
                editingServerId = it.editingServerId,
                displayName = it.displayName,
                host = it.host,
                shareName = it.shareName,
                initialPath = it.initialPath,
                username = it.username,
                password = it.password,
                passwordSaved = it.passwordSaved,
                displayNameError = it.displayNameError,
                hostError = it.hostError,
                shareNameError = it.shareNameError,
                saving = it.saving,
                error = it.error,
            ).block()
            it.copy(
                editingServerId = updated.editingServerId,
                displayName = updated.displayName,
                host = updated.host,
                shareName = updated.shareName,
                initialPath = updated.initialPath,
                username = updated.username,
                password = updated.password,
                passwordSaved = updated.passwordSaved,
                displayNameError = updated.displayNameError,
                hostError = updated.hostError,
                shareNameError = updated.shareNameError,
                saving = updated.saving,
                error = updated.error,
            )
        }
    }

    private fun clearForm() {
        _uiState.update {
            LanServerManagerUiState(servers = it.servers)
        }
    }
}

data class LanServerManagerUiState(
    val servers: List<LanServerProfile> = emptyList(),
    val displayName: String = "",
    val host: String = "",
    val shareName: String = "",
    val initialPath: String = "",
    val username: String = "",
    val password: String = "",
    val passwordSaved: Boolean = false,
    val editingServerId: String? = null,
    val displayNameError: Boolean = false,
    val hostError: Boolean = false,
    val shareNameError: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
)

sealed interface LanServerManagerEvent {
    data class DisplayNameChanged(val value: String) : LanServerManagerEvent
    data class HostChanged(val value: String) : LanServerManagerEvent
    data class ShareNameChanged(val value: String) : LanServerManagerEvent
    data class InitialPathChanged(val value: String) : LanServerManagerEvent
    data class UsernameChanged(val value: String) : LanServerManagerEvent
    data class PasswordChanged(val value: String) : LanServerManagerEvent
    data class EditServer(val server: LanServerProfile) : LanServerManagerEvent
    data class DeleteServer(val serverId: String) : LanServerManagerEvent
    data object SaveServer : LanServerManagerEvent
    data object ClearForm : LanServerManagerEvent
}

private data class LanServerFormState(
    val editingServerId: String? = null,
    val displayName: String = "",
    val host: String = "",
    val shareName: String = "",
    val initialPath: String = "",
    val username: String = "",
    val password: String = "",
    val passwordSaved: Boolean = false,
    val displayNameError: Boolean = false,
    val hostError: Boolean = false,
    val shareNameError: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
)
