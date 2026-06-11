package dev.anilbeesetti.nextplayer.core.subtitle.session

import org.junit.Assert.assertEquals
import org.junit.Test

class SubtitleSessionManagerTest {
    @Test
    fun resetClearsDisplayAndProvisionalState() {
        val sessionManager = SubtitleSessionManager()

        sessionManager.onOriginal(
            text = "hello",
            speaker = null,
            language = "en",
            confidence = 1f,
        )
        sessionManager.onProvisional(
            text = "hel",
            speaker = null,
            language = "en",
        )

        sessionManager.reset()

        assertEquals(emptyList<Any>(), sessionManager.displaySegments.value)
        assertEquals("", sessionManager.provisionalText.value)
        assertEquals(null, sessionManager.provisionalSpeaker.value)
    }
}
