package dev.anilbeesetti.nextplayer.core.subtitle.engine

import dev.anilbeesetti.nextplayer.core.subtitle.model.GeminiSessionConfig
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okio.ByteString.Companion.toByteString

object GeminiLiveMessageBuilder {
    fun buildSetupMessage(config: GeminiSessionConfig): String {
        val generationConfig = buildJsonObject {
            put("responseModalities", buildJsonArray { add(JsonPrimitive("AUDIO")) })
            if (config.inputAudioTranscriptionEnabled) {
                put("inputAudioTranscription", JsonObject(emptyMap()))
            }
            if (config.outputAudioTranscriptionEnabled) {
                put("outputAudioTranscription", JsonObject(emptyMap()))
            }
            put(
                "translationConfig",
                buildJsonObject {
                    put("targetLanguageCode", config.targetLanguageCode)
                    put("echoTargetLanguage", config.echoTargetLanguage)
                },
            )
        }

        return buildJsonObject {
            put(
                "setup",
                buildJsonObject {
                    put("model", config.model)
                    put("generationConfig", generationConfig)
                },
            )
        }.toString()
    }

    fun buildRealtimeAudioMessage(pcmData: ByteArray): String {
        return buildJsonObject {
            put(
                "realtimeInput",
                buildJsonObject {
                    put(
                        "audio",
                        buildJsonObject {
                            put("data", pcmData.toByteString().base64())
                            put("mimeType", GeminiSessionConfig.AUDIO_MIME_TYPE)
                        },
                    )
                },
            )
        }.toString()
    }
}
