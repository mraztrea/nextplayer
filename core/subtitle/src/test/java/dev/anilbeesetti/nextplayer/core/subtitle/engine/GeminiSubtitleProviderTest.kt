package dev.anilbeesetti.nextplayer.core.subtitle.engine

import dev.anilbeesetti.nextplayer.core.subtitle.model.GeminiLiveTranscriptEvent
import dev.anilbeesetti.nextplayer.core.subtitle.model.GeminiLiveTranscriptEventType.INPUT_TRANSCRIPT
import dev.anilbeesetti.nextplayer.core.subtitle.model.GeminiLiveTranscriptEventType.OUTPUT_TRANSCRIPT
import dev.anilbeesetti.nextplayer.core.subtitle.model.SegmentStatus
import dev.anilbeesetti.nextplayer.core.subtitle.model.SubtitleDisplayMode
import dev.anilbeesetti.nextplayer.core.subtitle.model.SubtitleProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class GeminiSubtitleProviderTest {
    @Test
    fun translationOnlySegmentUsesTranslatedDisplayText() {
        val segment = GeminiSubtitleProvider.toDisplaySegment(
            id = 1,
            inputEvent = inputEvent(),
            outputEvent = outputEvent(),
            displayMode = SubtitleDisplayMode.TRANSLATION_ONLY,
            targetLanguageCode = GeminiLiveTestFixtures.targetLanguageCode,
        )

        assertEquals("hello", segment.originalText)
        assertEquals("xin chao", segment.translationText)
        assertEquals("xin chao", segment.displayText)
        assertEquals(SegmentStatus.FINAL, segment.status)
        assertEquals(SubtitleProvider.GEMINI_LIVE, segment.provider)
        assertFalse(segment.isProvisional)
    }

    @Test
    fun bilingualSegmentKeepsOriginalAndTranslationInDisplayText() {
        val segment = GeminiSubtitleProvider.toDisplaySegment(
            id = 2,
            inputEvent = inputEvent(),
            outputEvent = outputEvent(),
            displayMode = SubtitleDisplayMode.BILINGUAL,
            targetLanguageCode = GeminiLiveTestFixtures.targetLanguageCode,
        )

        assertEquals("hello\nxin chao", segment.displayText)
    }

    @Test
    fun originalOnlySegmentUsesInputTranscriptDisplayText() {
        val segment = GeminiSubtitleProvider.toDisplaySegment(
            id = 3,
            inputEvent = inputEvent(),
            outputEvent = outputEvent(),
            displayMode = SubtitleDisplayMode.ORIGINAL_ONLY,
            targetLanguageCode = GeminiLiveTestFixtures.targetLanguageCode,
        )

        assertEquals("hello", segment.displayText)
    }

    private fun inputEvent() = GeminiLiveTranscriptEvent(
        eventType = INPUT_TRANSCRIPT,
        text = "hello",
        isFinal = true,
        languageCode = GeminiLiveTestFixtures.englishLanguageCode,
    )

    private fun outputEvent() = GeminiLiveTranscriptEvent(
        eventType = OUTPUT_TRANSCRIPT,
        text = "xin chao",
        isFinal = true,
        languageCode = GeminiLiveTestFixtures.targetLanguageCode,
    )
}
