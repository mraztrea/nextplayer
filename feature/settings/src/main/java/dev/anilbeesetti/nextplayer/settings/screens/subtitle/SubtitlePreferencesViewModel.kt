package dev.anilbeesetti.nextplayer.settings.screens.subtitle

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.anilbeesetti.nextplayer.core.data.repository.PreferencesRepository
import dev.anilbeesetti.nextplayer.core.model.Font
import dev.anilbeesetti.nextplayer.core.model.PlayerPreferences
import dev.anilbeesetti.nextplayer.core.subtitle.engine.SonioxWebSocketClient
import dev.anilbeesetti.nextplayer.core.subtitle.model.ModelPreparationState
import dev.anilbeesetti.nextplayer.core.subtitle.model.OfflineModel
import dev.anilbeesetti.nextplayer.core.subtitle.model.SubtitleDisplayMode
import dev.anilbeesetti.nextplayer.core.subtitle.storage.OfflineModelRepository
import dev.anilbeesetti.nextplayer.core.subtitle.storage.SecureApiKeyStorage
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SubtitlePreferencesViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val secureApiKeyStorage: SecureApiKeyStorage,
    private val sonioxWebSocketClient: SonioxWebSocketClient,
    private val offlineModelRepository: OfflineModelRepository,
) : ViewModel() {

    private val uiStateInternal = MutableStateFlow(
        SubtitlePreferencesUiState(
            preferences = preferencesRepository.playerPreferences.value,
        ),
    )
    val uiState = uiStateInternal.asStateFlow()

    init {
        loadStoredApiKey()
        refreshOfflineModel()

        viewModelScope.launch {
            preferencesRepository.playerPreferences.collect { preferences ->
                uiStateInternal.update { currentState ->
                    currentState.copy(preferences = preferences)
                }
            }
        }

        viewModelScope.launch {
            offlineModelRepository.installedModel.collect { model ->
                uiStateInternal.update { currentState ->
                    currentState.copy(offlineModel = model)
                }
            }
        }

        viewModelScope.launch {
            offlineModelRepository.preparationState.collect { state ->
                uiStateInternal.update { currentState ->
                    currentState.copy(modelPreparationState = state)
                }
            }
        }
    }

    fun onEvent(event: SubtitlePreferencesUiEvent) {
        when (event) {
            is SubtitlePreferencesUiEvent.ShowDialog -> showDialog(event.value)
            is SubtitlePreferencesUiEvent.UpdateSubtitleLanguage -> updateSubtitleLanguage(event.value)
            is SubtitlePreferencesUiEvent.UpdateSubtitleFont -> updateSubtitleFont(event.value)
            SubtitlePreferencesUiEvent.ToggleSubtitleTextBold -> toggleSubtitleTextBold()
            is SubtitlePreferencesUiEvent.UpdateSubtitleFontSize -> updateSubtitleFontSize(event.value)
            SubtitlePreferencesUiEvent.ToggleSubtitleBackground -> toggleSubtitleBackground()
            SubtitlePreferencesUiEvent.ToggleApplyEmbeddedStyles -> toggleApplyEmbeddedStyles()
            is SubtitlePreferencesUiEvent.UpdateSubtitleEncoding -> updateSubtitleEncoding(event.value)
            SubtitlePreferencesUiEvent.ToggleUseSystemCaptionStyle -> toggleUseSystemCaptionStyle()
            is SubtitlePreferencesUiEvent.UpdateTranslationSourceLanguage -> updateTranslationSourceLanguage(event.value)
            is SubtitlePreferencesUiEvent.UpdateTranslationTargetLanguage -> updateTranslationTargetLanguage(event.value)
            is SubtitlePreferencesUiEvent.UpdateDisplayMode -> updateDisplayMode(event.value)
            is SubtitlePreferencesUiEvent.UpdateApiKeyInput -> updateApiKeyInput(event.value)
            SubtitlePreferencesUiEvent.ToggleApiKeyVisibility -> toggleApiKeyVisibility()
            SubtitlePreferencesUiEvent.ValidateAndSaveApiKey -> validateAndSaveApiKey()
            SubtitlePreferencesUiEvent.ClearApiKey -> clearApiKey()
            SubtitlePreferencesUiEvent.ToggleOfflineSubtitle -> toggleOfflineSubtitle()
            is SubtitlePreferencesUiEvent.UpdateOfflineTargetLanguage -> updateOfflineTargetLanguage(event.value)
            SubtitlePreferencesUiEvent.ToggleOfflineDownloadWifiOnly -> toggleOfflineDownloadWifiOnly()
            SubtitlePreferencesUiEvent.RefreshOfflineModel -> refreshOfflineModel()
            SubtitlePreferencesUiEvent.DeleteOfflineModel -> deleteOfflineModel()
        }
    }

    private fun loadStoredApiKey() {
        uiStateInternal.update {
            it.copy(apiKeyInput = secureApiKeyStorage.getApiKey().orEmpty())
        }
    }

    private fun showDialog(value: SubtitlePreferenceDialog?) {
        uiStateInternal.update {
            it.copy(showDialog = value)
        }
    }

    private fun updateSubtitleLanguage(value: String) {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences {
                it.copy(preferredSubtitleLanguage = value)
            }
        }
    }

    private fun updateSubtitleFont(value: Font) {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences {
                it.copy(subtitleFont = value)
            }
        }
    }

    private fun toggleSubtitleTextBold() {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences {
                it.copy(subtitleTextBold = !it.subtitleTextBold)
            }
        }
    }

    private fun updateSubtitleFontSize(value: Int) {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences {
                it.copy(subtitleTextSize = value)
            }
        }
    }

    private fun toggleSubtitleBackground() {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences {
                it.copy(subtitleBackground = !it.subtitleBackground)
            }
        }
    }

    private fun toggleApplyEmbeddedStyles() {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences {
                it.copy(applyEmbeddedStyles = !it.applyEmbeddedStyles)
            }
        }
    }

    private fun updateSubtitleEncoding(value: String) {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences { it.copy(subtitleTextEncoding = value) }
        }
    }

    private fun toggleUseSystemCaptionStyle() {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences { it.copy(useSystemCaptionStyle = !it.useSystemCaptionStyle) }
        }
    }

    private fun updateTranslationSourceLanguage(value: String) {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences {
                it.copy(sourceLanguage = value)
            }
        }
    }

    private fun updateTranslationTargetLanguage(value: String) {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences {
                it.copy(targetLanguage = value)
            }
        }
    }

    private fun toggleOfflineSubtitle() {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences {
                it.copy(offlineSubtitleEnabled = !it.offlineSubtitleEnabled)
            }
        }
    }

    private fun updateOfflineTargetLanguage(value: String) {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences {
                it.copy(offlineTargetLanguage = value)
            }
        }
    }

    private fun toggleOfflineDownloadWifiOnly() {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences {
                it.copy(offlineDownloadWifiOnly = !it.offlineDownloadWifiOnly)
            }
        }
    }

    private fun refreshOfflineModel() {
        offlineModelRepository.refreshInstalledModel()
        uiStateInternal.update {
            it.copy(
                offlineModel = offlineModelRepository.installedModel.value,
                modelPreparationState = offlineModelRepository.preparationState.value,
            )
        }
    }

    private fun deleteOfflineModel() {
        offlineModelRepository.deleteInstalledModel()
        refreshOfflineModel()
    }

    private fun updateDisplayMode(value: SubtitleDisplayMode) {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences {
                it.copy(displayMode = value.name)
            }
        }
    }

    private fun updateApiKeyInput(value: String) {
        uiStateInternal.update {
            it.copy(
                apiKeyInput = value,
                apiKeyValidationState = ApiKeyValidationState.Idle,
            )
        }
    }

    private fun toggleApiKeyVisibility() {
        uiStateInternal.update {
            it.copy(isApiKeyVisible = !it.isApiKeyVisible)
        }
    }

    private fun validateAndSaveApiKey() {
        viewModelScope.launch {
            val apiKey = uiStateInternal.value.apiKeyInput.trim()
            if (apiKey.isBlank()) {
                uiStateInternal.update {
                    it.copy(apiKeyValidationState = ApiKeyValidationState.Invalid("Enter API key before validating"))
                }
                return@launch
            }

            uiStateInternal.update {
                it.copy(apiKeyValidationState = ApiKeyValidationState.Validating)
            }

            val validationError = sonioxWebSocketClient.validateApiKey(apiKey)
            if (validationError == null) {
                secureApiKeyStorage.saveApiKey(apiKey)
                preferencesRepository.updatePlayerPreferences {
                    it.copy(hasApiKeyConfigured = true)
                }
                uiStateInternal.update {
                    it.copy(
                        apiKeyInput = apiKey,
                        apiKeyValidationState = ApiKeyValidationState.Valid,
                    )
                }
            } else {
                uiStateInternal.update {
                    it.copy(apiKeyValidationState = ApiKeyValidationState.Invalid(validationError))
                }
            }
        }
    }

    private fun clearApiKey() {
        secureApiKeyStorage.clearApiKey()
        uiStateInternal.update {
            it.copy(
                apiKeyInput = "",
                apiKeyValidationState = ApiKeyValidationState.Idle,
            )
        }
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences {
                it.copy(hasApiKeyConfigured = false)
            }
        }
    }
}

@Stable
data class SubtitlePreferencesUiState(
    val showDialog: SubtitlePreferenceDialog? = null,
    val preferences: PlayerPreferences = PlayerPreferences(),
    val apiKeyInput: String = "",
    val isApiKeyVisible: Boolean = false,
    val apiKeyValidationState: ApiKeyValidationState = ApiKeyValidationState.Idle,
    val offlineModel: OfflineModel? = null,
    val modelPreparationState: ModelPreparationState = ModelPreparationState(),
)

sealed interface ApiKeyValidationState {
    data object Idle : ApiKeyValidationState
    data object Validating : ApiKeyValidationState
    data object Valid : ApiKeyValidationState
    data class Invalid(val message: String) : ApiKeyValidationState
}

sealed interface SubtitlePreferenceDialog {
    data object SubtitleLanguageDialog : SubtitlePreferenceDialog
    data object SubtitleFontDialog : SubtitlePreferenceDialog
    data object SubtitleEncodingDialog : SubtitlePreferenceDialog
    data object SourceLanguageDialog : SubtitlePreferenceDialog
    data object TargetLanguageDialog : SubtitlePreferenceDialog
    data object OfflineTargetLanguageDialog : SubtitlePreferenceDialog
    data object DisplayModeDialog : SubtitlePreferenceDialog
}

sealed interface SubtitlePreferencesUiEvent {
    data class ShowDialog(val value: SubtitlePreferenceDialog?) : SubtitlePreferencesUiEvent
    data class UpdateSubtitleLanguage(val value: String) : SubtitlePreferencesUiEvent
    data class UpdateSubtitleFont(val value: Font) : SubtitlePreferencesUiEvent
    data object ToggleSubtitleTextBold : SubtitlePreferencesUiEvent
    data class UpdateSubtitleFontSize(val value: Int) : SubtitlePreferencesUiEvent
    data object ToggleSubtitleBackground : SubtitlePreferencesUiEvent
    data object ToggleApplyEmbeddedStyles : SubtitlePreferencesUiEvent
    data class UpdateSubtitleEncoding(val value: String) : SubtitlePreferencesUiEvent
    data object ToggleUseSystemCaptionStyle : SubtitlePreferencesUiEvent
    data class UpdateTranslationSourceLanguage(val value: String) : SubtitlePreferencesUiEvent
    data class UpdateTranslationTargetLanguage(val value: String) : SubtitlePreferencesUiEvent
    data class UpdateDisplayMode(val value: SubtitleDisplayMode) : SubtitlePreferencesUiEvent
    data class UpdateApiKeyInput(val value: String) : SubtitlePreferencesUiEvent
    data object ToggleApiKeyVisibility : SubtitlePreferencesUiEvent
    data object ValidateAndSaveApiKey : SubtitlePreferencesUiEvent
    data object ClearApiKey : SubtitlePreferencesUiEvent
    data object ToggleOfflineSubtitle : SubtitlePreferencesUiEvent
    data class UpdateOfflineTargetLanguage(val value: String) : SubtitlePreferencesUiEvent
    data object ToggleOfflineDownloadWifiOnly : SubtitlePreferencesUiEvent
    data object RefreshOfflineModel : SubtitlePreferencesUiEvent
    data object DeleteOfflineModel : SubtitlePreferencesUiEvent
}
