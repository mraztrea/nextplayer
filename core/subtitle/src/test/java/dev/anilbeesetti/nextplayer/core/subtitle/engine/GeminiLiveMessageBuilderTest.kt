package dev.anilbeesetti.nextplayer.core.subtitle.engine

import dev.anilbeesetti.nextplayer.core.subtitle.model.GeminiSessionConfig
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiLiveMessageBuilderTest {
    @Test
    fun setupMessageIncludesTranslationAndTranscriptionConfig() {
        val config = GeminiSessionConfig(
            apiKey = GeminiLiveTestFixtures.apiKey,
            targetLanguageCode = GeminiLiveTestFixtures.targetLanguageCode,
        )

        val setup = Json.parseToJsonElement(GeminiLiveMessageBuilder.buildSetupMessage(config))
            .jsonObject.getValue("setup").jsonObject
        val generationConfig = setup.getValue("generationConfig").jsonObject
        val translationConfig = generationConfig.getValue("translationConfig").jsonObject

        assertEquals(GeminiSessionConfig.DEFAULT_MODEL, setup.getValue("model").jsonPrimitive.content)
        assertEquals(
            "AUDIO",
            generationConfig.getValue("responseModalities").jsonArray.first().jsonPrimitive.content,
        )
        assertTrue(generationConfig.containsKey("inputAudioTranscription"))
        assertTrue(generationConfig.containsKey("outputAudioTranscription"))
        assertEquals(
            GeminiLiveTestFixtures.targetLanguageCode,
            translationConfig.getValue("targetLanguageCode").jsonPrimitive.content,
        )
        assertEquals(true, translationConfig.getValue("echoTargetLanguage").jsonPrimitive.boolean)
    }

    @Test
    fun audioMessageEncodesRealtimePcmChunk() {
        val message = Json.parseToJsonElement(
            GeminiLiveMessageBuilder.buildRealtimeAudioMessage(GeminiLiveTestFixtures.pcm100Ms16KhzMono),
        ).jsonObject

        val audio = message
            .getValue("realtimeInput")
            .jsonObject
            .getValue("audio")
            .jsonObject

        assertEquals(GeminiSessionConfig.AUDIO_MIME_TYPE, audio.getValue("mimeType").jsonPrimitive.content)
        assertEquals(GeminiLiveTestFixtures.pcm100Ms16KhzMonoBase64, audio.getValue("data").jsonPrimitive.content)
    }
}
