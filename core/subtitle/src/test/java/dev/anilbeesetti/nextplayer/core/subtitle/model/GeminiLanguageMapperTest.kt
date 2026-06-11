package dev.anilbeesetti.nextplayer.core.subtitle.model

import org.junit.Assert.assertEquals
import org.junit.Test

class GeminiLanguageMapperTest {
    @Test
    fun mapsVietnameseAliasToBcp47Code() {
        assertEquals("vi", GeminiLanguageMapper.toTargetLanguageCode("vn"))
    }

    @Test
    fun keepsEnglishLanguageCode() {
        assertEquals("en", GeminiLanguageMapper.toTargetLanguageCode("en"))
    }

    @Test
    fun fallsBackToDefaultWhenBlank() {
        assertEquals(GeminiSessionConfig.DEFAULT_TARGET_LANGUAGE_CODE, GeminiLanguageMapper.toTargetLanguageCode(""))
    }
}
