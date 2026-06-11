package dev.anilbeesetti.nextplayer.core.subtitle.engine

import dev.anilbeesetti.nextplayer.core.subtitle.model.GeminiLiveTranscriptEventType.INPUT_TRANSCRIPT
import dev.anilbeesetti.nextplayer.core.subtitle.model.GeminiLiveTranscriptEventType.OUTPUT_TRANSCRIPT
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiLiveTranscriptParserTest {
    private val parser = GeminiLiveTranscriptParser()

    @Test
    fun parsesInputTranscriptEvent() {
        val events = parser.parse(GeminiLiveTestFixtures.inputTranscriptMessage)

        assertEquals(1, events.size)
        assertEquals(INPUT_TRANSCRIPT, events.single().eventType)
        assertEquals("hello", events.single().text)
        assertTrue(events.single().isFinal)
        assertEquals(GeminiLiveTestFixtures.englishLanguageCode, events.single().languageCode)
    }

    @Test
    fun parsesOutputTranscriptEvent() {
        val events = parser.parse(GeminiLiveTestFixtures.outputTranscriptMessage)

        assertEquals(1, events.size)
        assertEquals(OUTPUT_TRANSCRIPT, events.single().eventType)
        assertEquals("xin chao", events.single().text)
        assertTrue(events.single().isFinal)
        assertEquals(GeminiLiveTestFixtures.targetLanguageCode, events.single().languageCode)
    }
}
