package dev.anilbeesetti.nextplayer.settings.screens.subtitle

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.anilbeesetti.nextplayer.core.model.Font
import dev.anilbeesetti.nextplayer.core.model.PlayerPreferences
import dev.anilbeesetti.nextplayer.core.subtitle.model.SubtitleDisplayMode
import dev.anilbeesetti.nextplayer.core.ui.R
import dev.anilbeesetti.nextplayer.core.ui.components.ClickablePreferenceItem
import dev.anilbeesetti.nextplayer.core.ui.components.ListSectionTitle
import dev.anilbeesetti.nextplayer.core.ui.components.NextTopAppBar
import dev.anilbeesetti.nextplayer.core.ui.components.PreferenceSlider
import dev.anilbeesetti.nextplayer.core.ui.components.PreferenceSwitch
import dev.anilbeesetti.nextplayer.core.ui.components.PreferenceSwitchWithDivider
import dev.anilbeesetti.nextplayer.core.ui.components.RadioTextButton
import dev.anilbeesetti.nextplayer.core.ui.designsystem.NextIcons
import dev.anilbeesetti.nextplayer.core.ui.theme.NextPlayerTheme
import dev.anilbeesetti.nextplayer.settings.composables.OptionsDialog
import dev.anilbeesetti.nextplayer.settings.extensions.name
import dev.anilbeesetti.nextplayer.settings.utils.LocalesHelper
import java.nio.charset.Charset

@Composable
fun SubtitlePreferencesScreen(
    onNavigateUp: () -> Unit,
    viewModel: SubtitlePreferencesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SubtitlePreferencesContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateUp = onNavigateUp,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SubtitlePreferencesContent(
    uiState: SubtitlePreferencesUiState,
    onEvent: (SubtitlePreferencesUiEvent) -> Unit,
    onNavigateUp: () -> Unit,
) {
    val languages = remember { listOf(Pair("None", "")) + LocalesHelper.getAvailableLocales() }
    val autoDetectLabel = stringResource(id = R.string.auto_detect)
    val translationLanguages = remember { LocalesHelper.getAvailableTranslationLanguages() }
    val sourceLanguages = remember(autoDetectLabel, translationLanguages) {
        listOf(autoDetectLabel to PlayerPreferences.DEFAULT_LIVE_SUBTITLE_SOURCE_LANGUAGE) + translationLanguages
    }
    val charsetResource = stringArrayResource(id = R.array.charsets_list)
    val context = LocalContext.current
    val selectedDisplayMode = SubtitleDisplayMode.fromPreference(uiState.preferences.displayMode)
    val sourceLanguageDescription = remember(uiState.preferences.sourceLanguage, autoDetectLabel) {
        if (uiState.preferences.sourceLanguage == PlayerPreferences.DEFAULT_LIVE_SUBTITLE_SOURCE_LANGUAGE) {
            autoDetectLabel
        } else {
            LocalesHelper.getTranslationLanguageDisplayName(uiState.preferences.sourceLanguage)
        }
    }
    val targetLanguageDescription = remember(uiState.preferences.targetLanguage) {
        LocalesHelper.getTranslationLanguageDisplayName(uiState.preferences.targetLanguage)
            .ifBlank { uiState.preferences.targetLanguage }
    }
    val offlineTargetLanguages = remember {
        listOf(
            "Vietnamese" to PlayerPreferences.OFFLINE_SUBTITLE_TARGET_VIETNAMESE,
            "English" to PlayerPreferences.OFFLINE_SUBTITLE_TARGET_ENGLISH,
        )
    }
    val offlineTargetLanguageDescription = remember(uiState.preferences.offlineTargetLanguage) {
        offlineTargetLanguages.firstOrNull { it.second == uiState.preferences.offlineTargetLanguage }?.first
            ?: uiState.preferences.offlineTargetLanguage
    }
    val offlineModelDescription = remember(uiState.offlineModel) {
        uiState.offlineModel?.let { model ->
            if (model.isReady) {
                "${model.displayName} ${model.version}"
            } else {
                model.lastError ?: context.getString(R.string.offline_subtitle_model_missing)
            }
        } ?: context.getString(R.string.offline_subtitle_model_missing)
    }
    val apiKeyStatusText = remember(
        uiState.apiKeyValidationState,
        uiState.preferences.hasApiKeyConfigured,
    ) {
        when (val validationState = uiState.apiKeyValidationState) {
            ApiKeyValidationState.Idle -> {
                if (uiState.preferences.hasApiKeyConfigured) {
                    context.getString(R.string.api_key_configured)
                } else {
                    context.getString(R.string.api_key_not_configured)
                }
            }
            ApiKeyValidationState.Valid -> context.getString(R.string.api_key_validation_success)
            ApiKeyValidationState.Validating -> context.getString(R.string.api_key_validation_in_progress)
            is ApiKeyValidationState.Invalid -> validationState.message
        }
    }
    val apiKeyStatusColor = when (uiState.apiKeyValidationState) {
        ApiKeyValidationState.Valid -> MaterialTheme.colorScheme.primary
        is ApiKeyValidationState.Invalid -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Scaffold(
        topBar = {
            NextTopAppBar(
                title = stringResource(id = R.string.subtitle),
                navigationIcon = {
                    FilledTonalIconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = NextIcons.ArrowBack,
                            contentDescription = stringResource(id = R.string.navigate_up),
                        )
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(state = rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
        ) {
            ListSectionTitle(text = stringResource(id = R.string.translation))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedTextField(
                        value = uiState.apiKeyInput,
                        onValueChange = { onEvent(SubtitlePreferencesUiEvent.UpdateApiKeyInput(it)) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(text = stringResource(id = R.string.soniox_api_key)) },
                        placeholder = { Text(text = stringResource(id = R.string.soniox_api_key_placeholder)) },
                        singleLine = true,
                        visualTransformation = if (uiState.isApiKeyVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                    )
                    Text(
                        text = apiKeyStatusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = apiKeyStatusColor,
                    )
                    androidx.compose.foundation.layout.Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TextButton(onClick = { onEvent(SubtitlePreferencesUiEvent.ToggleApiKeyVisibility) }) {
                            Text(
                                text = stringResource(
                                    id = if (uiState.isApiKeyVisible) R.string.hide else R.string.show,
                                ),
                            )
                        }
                        FilledTonalButton(onClick = { onEvent(SubtitlePreferencesUiEvent.ValidateAndSaveApiKey) }) {
                            Text(text = stringResource(id = R.string.validate_and_save_api_key))
                        }
                        TextButton(
                            enabled = uiState.apiKeyInput.isNotBlank() || uiState.preferences.hasApiKeyConfigured,
                            onClick = { onEvent(SubtitlePreferencesUiEvent.ClearApiKey) },
                        ) {
                            Text(text = stringResource(id = R.string.clear_api_key))
                        }
                    }
                }
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            ) {
                ClickablePreferenceItem(
                    title = stringResource(id = R.string.source_language),
                    description = sourceLanguageDescription,
                    icon = NextIcons.Language,
                    onClick = { onEvent(SubtitlePreferencesUiEvent.ShowDialog(SubtitlePreferenceDialog.SourceLanguageDialog)) },
                    isFirstItem = true,
                )
                ClickablePreferenceItem(
                    title = stringResource(id = R.string.target_language),
                    description = targetLanguageDescription,
                    icon = NextIcons.Language,
                    onClick = { onEvent(SubtitlePreferencesUiEvent.ShowDialog(SubtitlePreferenceDialog.TargetLanguageDialog)) },
                )
                ClickablePreferenceItem(
                    title = stringResource(id = R.string.subtitle_display_mode),
                    description = selectedDisplayMode.label(),
                    icon = NextIcons.Caption,
                    onClick = { onEvent(SubtitlePreferencesUiEvent.ShowDialog(SubtitlePreferenceDialog.DisplayModeDialog)) },
                    isLastItem = true,
                )
            }
            ListSectionTitle(text = stringResource(id = R.string.offline_subtitle))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = stringResource(id = R.string.offline_subtitle_privacy),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    androidx.compose.foundation.layout.Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilledTonalButton(onClick = { onEvent(SubtitlePreferencesUiEvent.RefreshOfflineModel) }) {
                            Text(text = stringResource(id = R.string.offline_subtitle_refresh_model))
                        }
                        TextButton(
                            enabled = uiState.offlineModel != null,
                            onClick = { onEvent(SubtitlePreferencesUiEvent.DeleteOfflineModel) },
                        ) {
                            Text(text = stringResource(id = R.string.offline_subtitle_delete_model))
                        }
                    }
                }
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            ) {
                PreferenceSwitch(
                    title = stringResource(id = R.string.offline_subtitle),
                    description = offlineModelDescription,
                    icon = NextIcons.Caption,
                    isChecked = uiState.preferences.offlineSubtitleEnabled,
                    onClick = { onEvent(SubtitlePreferencesUiEvent.ToggleOfflineSubtitle) },
                    isFirstItem = true,
                )
                ClickablePreferenceItem(
                    title = stringResource(id = R.string.offline_subtitle_target_language),
                    description = offlineTargetLanguageDescription,
                    icon = NextIcons.Language,
                    onClick = {
                        onEvent(SubtitlePreferencesUiEvent.ShowDialog(SubtitlePreferenceDialog.OfflineTargetLanguageDialog))
                    },
                )
                PreferenceSwitch(
                    title = stringResource(id = R.string.offline_subtitle_download_wifi_only),
                    description = stringResource(id = R.string.offline_subtitle_model),
                    icon = NextIcons.Update,
                    isChecked = uiState.preferences.offlineDownloadWifiOnly,
                    onClick = { onEvent(SubtitlePreferencesUiEvent.ToggleOfflineDownloadWifiOnly) },
                    isLastItem = true,
                )
            }
            ListSectionTitle(text = stringResource(id = R.string.playback))
            Column(
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            ) {
                ClickablePreferenceItem(
                    title = stringResource(id = R.string.preferred_subtitle_lang),
                    description = LocalesHelper.getLocaleDisplayLanguage(uiState.preferences.preferredSubtitleLanguage)
                        .takeIf { it.isNotBlank() } ?: stringResource(R.string.preferred_subtitle_lang_description),
                    icon = NextIcons.Language,
                    onClick = { onEvent(SubtitlePreferencesUiEvent.ShowDialog(SubtitlePreferenceDialog.SubtitleLanguageDialog)) },
                    isFirstItem = true
                )
                ClickablePreferenceItem(
                    title = stringResource(R.string.subtitle_text_encoding),
                    description = charsetResource.first { it.contains(uiState.preferences.subtitleTextEncoding) },
                    icon = NextIcons.Subtitle,
                    onClick = { onEvent(SubtitlePreferencesUiEvent.ShowDialog(SubtitlePreferenceDialog.SubtitleEncodingDialog)) },
                    isLastItem = true
                )
            }
            ListSectionTitle(text = stringResource(id = R.string.appearance_name))
            Column(
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            ) {
                PreferenceSwitchWithDivider(
                    title = stringResource(R.string.system_caption_style),
                    description = stringResource(R.string.system_caption_style_desc),
                    icon = NextIcons.Caption,
                    isChecked = uiState.preferences.useSystemCaptionStyle,
                    onChecked = { onEvent(SubtitlePreferencesUiEvent.ToggleUseSystemCaptionStyle) },
                    onClick = { context.startActivity(Intent(Settings.ACTION_CAPTIONING_SETTINGS)) },
                    isFirstItem = true,
                )
                ClickablePreferenceItem(
                    title = stringResource(id = R.string.subtitle_font),
                    description = uiState.preferences.subtitleFont.name(),
                    icon = NextIcons.Font,
                    enabled = uiState.preferences.useSystemCaptionStyle.not(),
                    onClick = { onEvent(SubtitlePreferencesUiEvent.ShowDialog(SubtitlePreferenceDialog.SubtitleFontDialog)) },
                )
                PreferenceSwitch(
                    title = stringResource(id = R.string.subtitle_text_bold),
                    description = stringResource(id = R.string.subtitle_text_bold_desc),
                    icon = NextIcons.Bold,
                    enabled = uiState.preferences.useSystemCaptionStyle.not(),
                    isChecked = uiState.preferences.subtitleTextBold,
                    onClick = { onEvent(SubtitlePreferencesUiEvent.ToggleSubtitleTextBold) },
                )
                PreferenceSlider(
                    title = stringResource(id = R.string.subtitle_text_size),
                    description = uiState.preferences.subtitleTextSize.toString(),
                    icon = NextIcons.FontSize,
                    enabled = uiState.preferences.useSystemCaptionStyle.not(),
                    value = uiState.preferences.subtitleTextSize.toFloat(),
                    valueRange = 10f..60f,
                    onValueChange = { onEvent(SubtitlePreferencesUiEvent.UpdateSubtitleFontSize(it.toInt())) },
                    trailingContent = {
                        FilledIconButton(
                            enabled = uiState.preferences.useSystemCaptionStyle.not(),
                            onClick = {
                                onEvent(SubtitlePreferencesUiEvent.UpdateSubtitleFontSize(PlayerPreferences.DEFAULT_SUBTITLE_TEXT_SIZE))
                            },
                        ) {
                            Icon(
                                imageVector = NextIcons.History,
                                contentDescription = stringResource(id = R.string.reset_seek_increment),
                            )
                        }
                    },
                )
                PreferenceSwitch(
                    title = stringResource(id = R.string.subtitle_background),
                    description = stringResource(id = R.string.subtitle_background_desc),
                    icon = NextIcons.Background,
                    enabled = uiState.preferences.useSystemCaptionStyle.not(),
                    isChecked = uiState.preferences.subtitleBackground,
                    onClick = { onEvent(SubtitlePreferencesUiEvent.ToggleSubtitleBackground) },
                )
                PreferenceSwitch(
                    title = stringResource(R.string.embedded_styles),
                    description = stringResource(R.string.embedded_styles_desc),
                    icon = NextIcons.Style,
                    isChecked = uiState.preferences.applyEmbeddedStyles,
                    onClick = { onEvent(SubtitlePreferencesUiEvent.ToggleApplyEmbeddedStyles) },
                    isLastItem = true
                )
            }
        }

        uiState.showDialog?.let { showDialog ->
            when (showDialog) {
                SubtitlePreferenceDialog.SubtitleLanguageDialog -> {
                    OptionsDialog(
                        text = stringResource(id = R.string.preferred_subtitle_lang),
                        onDismissClick = { onEvent(SubtitlePreferencesUiEvent.ShowDialog(null)) },
                    ) {
                        items(languages) {
                            RadioTextButton(
                                text = it.first,
                                selected = it.second == uiState.preferences.preferredSubtitleLanguage,
                                onClick = {
                                    onEvent(SubtitlePreferencesUiEvent.UpdateSubtitleLanguage(it.second))
                                    onEvent(SubtitlePreferencesUiEvent.ShowDialog(null))
                                },
                            )
                        }
                    }
                }

                SubtitlePreferenceDialog.SubtitleFontDialog -> {
                    OptionsDialog(
                        text = stringResource(id = R.string.subtitle_font),
                        onDismissClick = { onEvent(SubtitlePreferencesUiEvent.ShowDialog(null)) },
                    ) {
                        items(Font.entries.toTypedArray()) {
                            RadioTextButton(
                                text = it.name(),
                                selected = it == uiState.preferences.subtitleFont,
                                onClick = {
                                    onEvent(SubtitlePreferencesUiEvent.UpdateSubtitleFont(it))
                                    onEvent(SubtitlePreferencesUiEvent.ShowDialog(null))
                                },
                            )
                        }
                    }
                }

                SubtitlePreferenceDialog.SubtitleEncodingDialog -> {
                    OptionsDialog(
                        text = stringResource(id = R.string.subtitle_text_encoding),
                        onDismissClick = { onEvent(SubtitlePreferencesUiEvent.ShowDialog(null)) },
                    ) {
                        items(charsetResource) {
                            val currentCharset = it.substringAfterLast("(", "").removeSuffix(")")
                            if (currentCharset.isEmpty() || Charset.isSupported(currentCharset)) {
                                RadioTextButton(
                                    text = it,
                                    selected = currentCharset == uiState.preferences.subtitleTextEncoding,
                                    onClick = {
                                        onEvent(SubtitlePreferencesUiEvent.UpdateSubtitleEncoding(currentCharset))
                                        onEvent(SubtitlePreferencesUiEvent.ShowDialog(null))
                                    },
                                )
                            }
                        }
                    }
                }

                SubtitlePreferenceDialog.SourceLanguageDialog -> {
                    OptionsDialog(
                        text = stringResource(id = R.string.source_language),
                        onDismissClick = { onEvent(SubtitlePreferencesUiEvent.ShowDialog(null)) },
                    ) {
                        items(sourceLanguages) { option ->
                            RadioTextButton(
                                text = option.first,
                                selected = option.second == uiState.preferences.sourceLanguage,
                                onClick = {
                                    onEvent(SubtitlePreferencesUiEvent.UpdateTranslationSourceLanguage(option.second))
                                    onEvent(SubtitlePreferencesUiEvent.ShowDialog(null))
                                },
                            )
                        }
                    }
                }

                SubtitlePreferenceDialog.TargetLanguageDialog -> {
                    OptionsDialog(
                        text = stringResource(id = R.string.target_language),
                        onDismissClick = { onEvent(SubtitlePreferencesUiEvent.ShowDialog(null)) },
                    ) {
                        items(translationLanguages) { option ->
                            RadioTextButton(
                                text = option.first,
                                selected = option.second == uiState.preferences.targetLanguage,
                                onClick = {
                                    onEvent(SubtitlePreferencesUiEvent.UpdateTranslationTargetLanguage(option.second))
                                    onEvent(SubtitlePreferencesUiEvent.ShowDialog(null))
                                },
                            )
                        }
                    }
                }

                SubtitlePreferenceDialog.OfflineTargetLanguageDialog -> {
                    OptionsDialog(
                        text = stringResource(id = R.string.offline_subtitle_target_language),
                        onDismissClick = { onEvent(SubtitlePreferencesUiEvent.ShowDialog(null)) },
                    ) {
                        items(offlineTargetLanguages) { option ->
                            RadioTextButton(
                                text = option.first,
                                selected = option.second == uiState.preferences.offlineTargetLanguage,
                                onClick = {
                                    onEvent(SubtitlePreferencesUiEvent.UpdateOfflineTargetLanguage(option.second))
                                    onEvent(SubtitlePreferencesUiEvent.ShowDialog(null))
                                },
                            )
                        }
                    }
                }

                SubtitlePreferenceDialog.DisplayModeDialog -> {
                    OptionsDialog(
                        text = stringResource(id = R.string.subtitle_display_mode),
                        onDismissClick = { onEvent(SubtitlePreferencesUiEvent.ShowDialog(null)) },
                    ) {
                        items(SubtitleDisplayMode.entries.toTypedArray()) { mode ->
                            RadioTextButton(
                                text = mode.label(),
                                selected = mode == selectedDisplayMode,
                                onClick = {
                                    onEvent(SubtitlePreferencesUiEvent.UpdateDisplayMode(mode))
                                    onEvent(SubtitlePreferencesUiEvent.ShowDialog(null))
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubtitleDisplayMode.label(): String {
    return when (this) {
        SubtitleDisplayMode.ORIGINAL_ONLY -> stringResource(id = R.string.original_only)
        SubtitleDisplayMode.TRANSLATION_ONLY -> stringResource(id = R.string.translation_only)
        SubtitleDisplayMode.BILINGUAL -> stringResource(id = R.string.bilingual)
    }
}

@PreviewLightDark
@Composable
private fun SubtitlePreferencesScreenPreview() {
    NextPlayerTheme {
        SubtitlePreferencesContent(
            uiState = SubtitlePreferencesUiState(),
            onEvent = {},
            onNavigateUp = {},
        )
    }
}
