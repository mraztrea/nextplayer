package dev.anilbeesetti.nextplayer.core.subtitle.storage

import dev.anilbeesetti.nextplayer.core.subtitle.model.OfflineModel
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class OfflineModelManifest(
    val manifestVersion: Int,
    val modelId: String,
    val displayName: String,
    val version: String,
    val sourceLanguage: String,
    val targetLanguages: Set<String>,
    val capabilities: Set<String>,
    val files: List<OfflineModelManifestFile>,
) {
    fun toModel(installedPath: String, sizeBytes: Long): OfflineModel {
        return OfflineModel(
            id = modelId,
            displayName = displayName,
            version = version,
            manifestVersion = manifestVersion,
            sourceLanguage = sourceLanguage,
            targetLanguages = targetLanguages,
            capabilities = capabilities,
            sizeBytes = sizeBytes,
            installedPath = installedPath,
            state = if (isV1Supported()) {
                dev.anilbeesetti.nextplayer.core.subtitle.model.OfflineModelState.READY
            } else {
                dev.anilbeesetti.nextplayer.core.subtitle.model.OfflineModelState.ERROR
            },
        )
    }

    fun isV1Supported(): Boolean {
        return sourceLanguage == OfflineModel.JAPANESE_LANGUAGE &&
            OfflineModel.VIETNAMESE_LANGUAGE in targetLanguages &&
            OfflineModel.ENGLISH_LANGUAGE in targetLanguages &&
            OfflineModel.JAPANESE_TRANSCRIPT_CAPABILITY in capabilities &&
            OfflineModel.JAPANESE_TO_VIETNAMESE_CAPABILITY in capabilities &&
            OfflineModel.JAPANESE_TO_ENGLISH_CAPABILITY in capabilities &&
            files.isNotEmpty()
    }

    companion object {
        fun parse(rawJson: String): OfflineModelManifest {
            val json = Json.parseToJsonElement(rawJson).jsonObject
            return OfflineModelManifest(
                manifestVersion = json.requiredPrimitive("manifestVersion").content.toInt(),
                modelId = json.requiredPrimitive("modelId").content,
                displayName = json.requiredPrimitive("displayName").content,
                version = json.requiredPrimitive("version").content,
                sourceLanguage = json.requiredPrimitive("sourceLanguage").content,
                targetLanguages = json.requiredArray("targetLanguages").toStringSet(),
                capabilities = json.requiredArray("capabilities").toStringSet(),
                files = json.requiredArray("files").map { fileElement ->
                    fileElement.jsonObject.let { file ->
                        OfflineModelManifestFile(
                            path = file.requiredPrimitive("path").content,
                            sha256 = file.requiredPrimitive("sha256").content,
                            sizeBytes = file.requiredPrimitive("sizeBytes").content.toLong(),
                        )
                    }
                },
            )
        }
    }
}

data class OfflineModelManifestFile(
    val path: String,
    val sha256: String,
    val sizeBytes: Long,
)

private fun JsonObject.requiredPrimitive(name: String): JsonPrimitive {
    return this[name]?.jsonPrimitive ?: error("Missing manifest field: $name")
}

private fun JsonObject.requiredArray(name: String): JsonArray {
    return this[name]?.jsonArray ?: error("Missing manifest field: $name")
}

private fun JsonArray.toStringSet(): Set<String> {
    return map { it.jsonPrimitive.content }.toSet()
}
