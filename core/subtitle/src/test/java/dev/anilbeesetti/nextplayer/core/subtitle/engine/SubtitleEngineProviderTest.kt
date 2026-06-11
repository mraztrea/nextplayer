package dev.anilbeesetti.nextplayer.core.subtitle.engine

import dev.anilbeesetti.nextplayer.core.subtitle.model.SubtitleProvider
import org.junit.Assert.assertEquals
import org.junit.Test

class SubtitleEngineProviderTest {
    @Test
    fun geminiPreferenceResolvesToGeminiProviderWithoutFallback() {
        assertEquals(
            SubtitleProvider.GEMINI_LIVE,
            SubtitleProvider.fromPreference(SubtitleProvider.GEMINI_LIVE.name),
        )
    }
}
