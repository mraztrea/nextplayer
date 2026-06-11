package dev.anilbeesetti.nextplayer.core.subtitle.engine

import dev.anilbeesetti.nextplayer.core.subtitle.model.GeminiLiveTranscriptEvent
import dev.anilbeesetti.nextplayer.core.subtitle.model.GeminiLiveTranscriptEventType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiLiveTranscriptParser @Inject constructor() {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(message: String): List<GeminiLiveTranscriptEvent> {
        val root = json.parseToJsonElement(message).jsonObject
        val events = mutableListOf<GeminiLiveTranscriptEvent>()

        root["error"]?.jsonObjectOrNull()?.let { error ->
            events += GeminiLiveTranscriptEvent(
                eventType = GeminiLiveTranscriptEventType.ERROR,
                text = error.stringOrNull("message"),
                isFinal = true,
                errorCode = error.stringOrNull("status") ?: error.stringOrNull("code"),
            )
        }

        if (root.containsKey("setupComplete")) {
            events += GeminiLiveTranscriptEvent(
                eventType = GeminiLiveTranscriptEventType.STATUS,
                isFinal = true,
            )
        }

        val serverContent = root["serverContent"]?.jsonObjectOrNull() ?: return events
        transcriptEvent(serverContent, "inputTranscription", GeminiLiveTranscriptEventType.INPUT_TRANSCRIPT)?.let {
            events += it
        }
        transcriptEvent(serverContent, "outputTranscription", GeminiLiveTranscriptEventType.OUTPUT_TRANSCRIPT)?.let {
            events += it
        }

        return events
    }

    private fun transcriptEvent(
        serverContent: JsonObject,
        key: String,
        eventType: GeminiLiveTranscriptEventType,
    ): GeminiLiveTranscriptEvent? {
        val transcript = serverContent[key]?.jsonObjectOrNull() ?: return null
        val text = transcript.stringOrNull("text")
        if (text == null && !transcript.containsKey("finished") && !transcript.containsKey("isFinal")) {
            return null
        }

        return GeminiLiveTranscriptEvent(
            eventType = eventType,
            text = text,
            isFinal = transcript.booleanOrFalse("finished") || transcript.booleanOrFalse("isFinal"),
            languageCode = transcript.stringOrNull("languageCode") ?: transcript.stringOrNull("language_code"),
        )
    }

    private fun kotlinx.serialization.json.JsonElement.jsonObjectOrNull(): JsonObject? {
        return this as? JsonObject
    }

    private fun JsonObject.stringOrNull(key: String): String? {
        return this[key]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
    }

    private fun JsonObject.booleanOrFalse(key: String): Boolean {
        return this[key]?.jsonPrimitive?.booleanOrNull == true
    }
}
