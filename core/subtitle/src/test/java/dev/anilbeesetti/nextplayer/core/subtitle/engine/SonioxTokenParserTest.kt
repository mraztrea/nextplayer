package dev.anilbeesetti.nextplayer.core.subtitle.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SonioxTokenParserTest {

    @Test
    fun `original final tokens keep earliest start and latest end timing`() {
        val parser = SonioxTokenParser()
        var callbackText: String? = null
        var callbackStartMs: Long? = null
        var callbackEndMs: Long? = null

        parser.setCallback(object : SonioxTokenParser.Callback {
            override fun onOriginal(
                text: String,
                speaker: String?,
                language: String?,
                confidence: Float?,
                startMs: Long?,
                endMs: Long?,
            ) {
                callbackText = text
                callbackStartMs = startMs
                callbackEndMs = endMs
            }

            override fun onTranslation(text: String) = Unit

            override fun onProvisional(text: String, speaker: String?, language: String?) = Unit

            override fun onEndpointReached() = Unit
        })

        val tokens = listOf(
            SonioxTokenParser.TokenData(
                text = "Hel",
                translationStatus = "original",
                isFinal = true,
                speaker = null,
                language = null,
                confidence = null,
                startMs = 120L,
                endMs = 180L,
            ),
            SonioxTokenParser.TokenData(
                text = "lo",
                translationStatus = "original",
                isFinal = true,
                speaker = null,
                language = null,
                confidence = null,
                startMs = 180L,
                endMs = 260L,
            ),
        )

        parser.parseTokenData(tokens)

        assertEquals("Hello", callbackText)
        assertEquals(120L, callbackStartMs)
        assertEquals(260L, callbackEndMs)
    }

    @Test
    fun `translation final tokens do not require timing`() {
        val parser = SonioxTokenParser()
        var callbackTranslation: String? = null
        var originalTimingSeen = false

        parser.setCallback(object : SonioxTokenParser.Callback {
            override fun onOriginal(
                text: String,
                speaker: String?,
                language: String?,
                confidence: Float?,
                startMs: Long?,
                endMs: Long?,
            ) {
                originalTimingSeen = startMs != null || endMs != null
            }

            override fun onTranslation(text: String) {
                callbackTranslation = text
            }

            override fun onProvisional(text: String, speaker: String?, language: String?) = Unit

            override fun onEndpointReached() = Unit
        })

        val tokens = listOf(
            SonioxTokenParser.TokenData(
                text = "Xin chào",
                translationStatus = "translation",
                isFinal = true,
                speaker = null,
                language = null,
                confidence = null,
                startMs = null,
                endMs = null,
            ),
        )

        parser.parseTokenData(tokens)

        assertEquals("Xin chào", callbackTranslation)
        assertNull("translation callback should not synthesize original timing", if (originalTimingSeen) 1 else null)
    }
}
