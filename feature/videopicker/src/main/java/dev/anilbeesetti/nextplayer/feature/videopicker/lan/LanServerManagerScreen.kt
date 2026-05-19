package dev.anilbeesetti.nextplayer.feature.videopicker.lan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.anilbeesetti.nextplayer.core.model.LanErrorType
import dev.anilbeesetti.nextplayer.core.model.LanServerProfile
import dev.anilbeesetti.nextplayer.core.ui.R
import dev.anilbeesetti.nextplayer.core.ui.components.NextTopAppBar
import dev.anilbeesetti.nextplayer.core.ui.designsystem.NextIcons

@Composable
fun LanServerManagerRoute(
    viewModel: LanServerManagerViewModel = hiltViewModel(),
    onNavigateUp: () -> Unit,
    onOpenServer: (serverId: String) -> Unit,
    onBookmarksClick: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LanServerManagerScreen(
        uiState = uiState,
        onNavigateUp = onNavigateUp,
        onOpenServer = onOpenServer,
        onBookmarksClick = onBookmarksClick,
        onEvent = viewModel::onEvent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LanServerManagerScreen(
    uiState: LanServerManagerUiState,
    onNavigateUp: () -> Unit = {},
    onOpenServer: (serverId: String) -> Unit = {},
    onBookmarksClick: () -> Unit = {},
    onEvent: (LanServerManagerEvent) -> Unit = {},
) {
    Scaffold(
        topBar = {
            NextTopAppBar(
                title = stringResource(R.string.lan_servers),
                navigationIcon = {
                    FilledTonalIconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = NextIcons.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_up),
                        )
                    }
                },
                actions = {
                    TextButton(onClick = onBookmarksClick) {
                        Text(text = stringResource(R.string.lan_bookmarks))
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) { scaffoldPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                LanServerForm(
                    uiState = uiState,
                    onEvent = onEvent,
                )
            }

            item {
                Text(
                    text = stringResource(R.string.lan_saved_servers),
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            if (uiState.servers.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.lan_no_servers),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            items(
                items = uiState.servers,
                key = { it.id },
            ) { server ->
                LanServerListItem(
                    server = server,
                    onOpenServer = onOpenServer,
                    onEvent = onEvent,
                )
            }
        }
    }
}

@Composable
private fun LanServerForm(
    uiState: LanServerManagerUiState,
    onEvent: (LanServerManagerEvent) -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(
                    if (uiState.editingServerId == null) R.string.lan_add_server else R.string.lan_edit_server,
                ),
                style = MaterialTheme.typography.titleMedium,
            )
            LanTextField(
                value = uiState.displayName,
                onValueChange = { onEvent(LanServerManagerEvent.DisplayNameChanged(it)) },
                label = stringResource(R.string.lan_server_name),
                isError = uiState.displayNameError,
            )
            LanTextField(
                value = uiState.host,
                onValueChange = { onEvent(LanServerManagerEvent.HostChanged(it)) },
                label = stringResource(R.string.lan_server_host),
                isError = uiState.hostError,
            )
            LanTextField(
                value = uiState.shareName,
                onValueChange = { onEvent(LanServerManagerEvent.ShareNameChanged(it)) },
                label = stringResource(R.string.lan_server_share),
                isError = uiState.shareNameError,
            )
            LanTextField(
                value = uiState.initialPath,
                onValueChange = { onEvent(LanServerManagerEvent.InitialPathChanged(it)) },
                label = stringResource(R.string.lan_initial_path),
            )
            LanTextField(
                value = uiState.username,
                onValueChange = { onEvent(LanServerManagerEvent.UsernameChanged(it)) },
                label = stringResource(R.string.lan_server_username),
            )
            OutlinedTextField(
                value = uiState.password,
                onValueChange = { onEvent(LanServerManagerEvent.PasswordChanged(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.lan_server_password)) },
                supportingText = {
                    if (uiState.passwordSaved && uiState.password.isBlank()) {
                        Text(stringResource(R.string.lan_password_saved_hint))
                    }
                },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
            )
            uiState.error?.let {
                Text(
                    text = stringResource(lanErrorMessageRes(it)),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    enabled = !uiState.saving,
                    onClick = { onEvent(LanServerManagerEvent.SaveServer) },
                ) {
                    Text(text = stringResource(R.string.lan_save_server))
                }
                if (uiState.editingServerId != null) {
                    TextButton(onClick = { onEvent(LanServerManagerEvent.ClearForm) }) {
                        Text(text = stringResource(R.string.lan_clear_form))
                    }
                }
            }
        }
    }
}

@Composable
private fun LanTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isError: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        isError = isError,
        supportingText = {
            if (isError) {
                Text(stringResource(R.string.lan_field_required))
            }
        },
        singleLine = true,
    )
}

@Composable
private fun LanServerListItem(
    server: LanServerProfile,
    onOpenServer: (serverId: String) -> Unit,
    onEvent: (LanServerManagerEvent) -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = server.displayName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${server.host}/${server.shareName}${server.initialPath}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { onOpenServer(server.id) }) {
                    Text(text = stringResource(R.string.lan_open_server))
                }
                TextButton(onClick = { onEvent(LanServerManagerEvent.EditServer(server)) }) {
                    Text(text = stringResource(R.string.lan_edit))
                }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = { onEvent(LanServerManagerEvent.DeleteServer(server.id)) }) {
                    Text(text = stringResource(R.string.delete))
                }
            }
        }
    }
}

private fun lanErrorMessageRes(errorTypeName: String): Int {
    return when (errorTypeName) {
        LanErrorType.AuthenticationFailed.name -> R.string.lan_error_authentication
        LanErrorType.ServerUnreachable.name -> R.string.lan_error_unreachable
        LanErrorType.PermissionDenied.name -> R.string.lan_error_permission
        LanErrorType.PathNotFound.name,
        LanErrorType.ShareNotFound.name,
        -> R.string.lan_error_missing_path
        LanErrorType.Duplicate.name -> R.string.lan_duplicate_server
        else -> R.string.lan_error_unknown
    }
}
