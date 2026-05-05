package dev.anilbeesetti.nextplayer.core.subtitle.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SubtitleSessionManagerTest {

    @Test
    fun `segment becomes visible only inside playback window`() {
        val manager = SubtitleSessionManager()

        manager.beginGeneration(generationId = 7L, basePositionMs = 1000L)
        manager.onOriginal(
            text = "Hello",
            speaker = "1",
            language = "en",
            confidence = 0.9f,
            startMs = 100L,
            endMs = 300L,
        )

        manager.onPlaybackPosition(1050L)
        assertTrue(manager.displaySegments.value.isEmpty())

        manager.onPlaybackPosition(1100L)
        assertEquals(1, manager.displaySegments.value.size)
        assertEquals(1100L, manager.displaySegments.value.single().targetStartMs)

        manager.onPlaybackPosition(1400L)
        assertTrue(manager.displaySegments.value.isEmpty())
    }

    @Test
    fun `translation inherits timing from the oldest pending original in same generation`() {
        val manager = SubtitleSessionManager()

        manager.beginGeneration(generationId = 3L, basePositionMs = 2000L)
        manager.onOriginal(
            text = "How are you",
            speaker = null,
            language = "en",
            confidence = 0.8f,
            startMs = 50L,
            endMs = 450L,
        )
        manager.onTranslation("Ban khoe khong")
        manager.onPlaybackPosition(2050L)

        val segment = manager.displaySegments.value.single()
        assertEquals("Ban khoe khong", segment.translationText)
        assertEquals(2050L, segment.targetStartMs)
        assertTrue(segment.targetEndMs >= 2450L)
    }

    @Test
    fun `generation reset clears old visible and pending segments`() {
        val manager = SubtitleSessionManager()

        manager.beginGeneration(generationId = 1L, basePositionMs = 0L)
        manager.onOriginal(
            text = "Old",
            speaker = null,
            language = "en",
            confidence = 0.7f,
            startMs = 0L,
            endMs = 100L,
        )
        manager.onPlaybackPosition(0L)
        assertEquals(1, manager.displaySegments.value.size)

        manager.beginGeneration(generationId = 2L, basePositionMs = 5000L)
        manager.onPlaybackPosition(5000L)

        assertTrue(manager.displaySegments.value.isEmpty())
        assertTrue(manager.provisionalText.value.isEmpty())
    }

    @Test
    fun `short translated segment stays visible for minimum readable duration`() {
        val manager = SubtitleSessionManager()

        manager.beginGeneration(generationId = 9L, basePositionMs = 0L)
        manager.onOriginal(
            text = "Yes",
            speaker = null,
            language = "en",
            confidence = 0.9f,
            startMs = 0L,
            endMs = 150L,
        )
        manager.onTranslation("Vang")

        manager.onPlaybackPosition(1000L)
        assertEquals(1, manager.displaySegments.value.size)
        assertEquals("Vang", manager.displaySegments.value.single().translationText)

        manager.onPlaybackPosition(1700L)
        assertTrue(manager.displaySegments.value.isEmpty())
    }

    @Test
    fun `late translation still becomes visible after original audio window passed`() {
        val manager = SubtitleSessionManager()

        manager.beginGeneration(generationId = 10L, basePositionMs = 0L)
        manager.onOriginal(
            text = "Thank you",
            speaker = null,
            language = "en",
            confidence = 0.9f,
            startMs = 0L,
            endMs = 250L,
        )

        manager.onPlaybackPosition(400L)
        assertTrue(manager.displaySegments.value.isEmpty())

        manager.onTranslation("Cam on")

        assertEquals(1, manager.displaySegments.value.size)
        assertEquals("Cam on", manager.displaySegments.value.single().translationText)
    }
}
