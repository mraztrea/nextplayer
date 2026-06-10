package dev.anilbeesetti.nextplayer.core.subtitle.storage

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.anilbeesetti.nextplayer.core.subtitle.model.ModelPreparationState
import dev.anilbeesetti.nextplayer.core.subtitle.model.OfflineModel
import dev.anilbeesetti.nextplayer.core.subtitle.model.OfflineModelState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

interface OfflineModelReadiness {
    val installedModel: StateFlow<OfflineModel?>
    fun refreshInstalledModel()
    fun isModelReady(): Boolean
}

@Singleton
class OfflineModelRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : OfflineModelReadiness {
    private val modelRoot: File
        get() = File(context.filesDir, OFFLINE_MODEL_DIR)

    private val _installedModel = MutableStateFlow<OfflineModel?>(null)
    override val installedModel: StateFlow<OfflineModel?> = _installedModel.asStateFlow()

    private val _preparationState = MutableStateFlow(ModelPreparationState())
    val preparationState: StateFlow<ModelPreparationState> = _preparationState.asStateFlow()

    override fun refreshInstalledModel() {
        _installedModel.value = readManifestModel()
    }

    override fun isModelReady(): Boolean {
        return installedModel.value?.isReady == true
    }

    fun deleteInstalledModel() {
        modelRoot.deleteRecursively()
        _installedModel.value = null
    }

    private fun readManifestModel(): OfflineModel? {
        val manifest = File(modelRoot, MANIFEST_FILE)
        if (!manifest.isFile) return null

        return runCatching {
            val parsed = OfflineModelManifest.parse(manifest.readText())
            val sizeBytes = modelRoot.walkTopDown()
                .filter { it.isFile }
                .sumOf { it.length() }
            parsed.toModel(modelRoot.absolutePath, sizeBytes)
        }.getOrElse { error ->
            OfflineModel(
                id = "invalid-offline-model",
                displayName = "Invalid offline model",
                version = "",
                manifestVersion = 0,
                sourceLanguage = OfflineModel.JAPANESE_LANGUAGE,
                targetLanguages = emptySet(),
                capabilities = emptySet(),
                sizeBytes = 0L,
                installedPath = modelRoot.absolutePath,
                state = OfflineModelState.ERROR,
                lastError = error.message,
            )
        }
    }

    companion object {
        const val OFFLINE_MODEL_DIR = "offline-subtitle-model"
        const val MANIFEST_FILE = "manifest.json"
    }
}
