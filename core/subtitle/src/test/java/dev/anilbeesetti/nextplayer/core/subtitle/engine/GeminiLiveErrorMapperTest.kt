package dev.anilbeesetti.nextplayer.core.subtitle.engine

import dev.anilbeesetti.nextplayer.core.subtitle.model.GeminiLiveErrorCategory
import java.io.IOException
import java.net.SocketTimeoutException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiLiveErrorMapperTest {
    @Test
    fun mapsMissingApiKey() {
        val error = GeminiLiveErrorMapper.missingApiKey()

        assertEquals(GeminiLiveErrorCategory.MISSING_API_KEY, error.category)
        assertFalse(error.isRecoverable)
    }

    @Test
    fun mapsInvalidApiKey() {
        val error = GeminiLiveErrorMapper.fromProviderError("401", "API key not valid")

        assertEquals(GeminiLiveErrorCategory.INVALID_API_KEY, error.category)
        assertFalse(error.isRecoverable)
    }

    @Test
    fun mapsQuotaError() {
        val error = GeminiLiveErrorMapper.fromProviderError("RESOURCE_EXHAUSTED", "quota exceeded")

        assertEquals(GeminiLiveErrorCategory.RATE_LIMITED, error.category)
        assertTrue(error.isRecoverable)
    }

    @Test
    fun mapsNetworkError() {
        val error = GeminiLiveErrorMapper.fromThrowable(IOException("network down"))

        assertEquals(GeminiLiveErrorCategory.NETWORK, error.category)
        assertTrue(error.isRecoverable)
    }

    @Test
    fun mapsUnsupportedLanguage() {
        val error = GeminiLiveErrorMapper.fromProviderError("INVALID_ARGUMENT", "unsupported language")

        assertEquals(GeminiLiveErrorCategory.UNSUPPORTED_LANGUAGE, error.category)
        assertFalse(error.isRecoverable)
    }

    @Test
    fun mapsTimeout() {
        val error = GeminiLiveErrorMapper.fromThrowable(SocketTimeoutException("timeout"))

        assertEquals(GeminiLiveErrorCategory.TIMEOUT, error.category)
        assertTrue(error.isRecoverable)
    }
}
