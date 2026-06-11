package dev.anilbeesetti.nextplayer.core.subtitle.engine

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SubtitleDiagnosticLoggerTest {
    @Test
    fun metadataMessageRedactsAudioTranscriptAndApiKeyFields() {
        val message = SubtitleDiagnosticLogger.metadata(
            "provider" to "Gemini Live",
            "apiKey" to "secret-key",
            "audio" to "raw-pcm",
            "transcript" to "hello world",
            "targetLanguage" to "vi",
        )

        assertTrue(message.contains("provider=Gemini Live"))
        assertTrue(message.contains("targetLanguage=vi"))
        assertFalse(message.contains("secret-key"))
        assertFalse(message.contains("raw-pcm"))
        assertFalse(message.contains("hello world"))
    }
}
