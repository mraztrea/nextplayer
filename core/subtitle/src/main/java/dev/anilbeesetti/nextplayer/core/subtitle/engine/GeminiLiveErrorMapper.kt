package dev.anilbeesetti.nextplayer.core.subtitle.engine

import dev.anilbeesetti.nextplayer.core.subtitle.model.GeminiLiveError
import dev.anilbeesetti.nextplayer.core.subtitle.model.GeminiLiveErrorCategory
import java.io.IOException
import java.net.SocketTimeoutException

object GeminiLiveErrorMapper {
    fun missingApiKey(): GeminiLiveError {
        return GeminiLiveError(
            category = GeminiLiveErrorCategory.MISSING_API_KEY,
            message = "Configure Google API key in Settings > Subtitle",
            isRecoverable = false,
        )
    }

    fun fromProviderError(providerCode: String?, message: String?): GeminiLiveError {
        val normalizedCode = providerCode.orEmpty().uppercase()
        val normalizedMessage = message.orEmpty().lowercase()
        val category = when {
            normalizedCode == "401" || "api key" in normalizedMessage && "not valid" in normalizedMessage -> {
                GeminiLiveErrorCategory.INVALID_API_KEY
            }
            normalizedCode == "403" || normalizedCode == "PERMISSION_DENIED" -> GeminiLiveErrorCategory.NO_PERMISSION
            normalizedCode == "429" || normalizedCode == "RESOURCE_EXHAUSTED" || "quota" in normalizedMessage -> {
                GeminiLiveErrorCategory.RATE_LIMITED
            }
            normalizedCode == "INVALID_ARGUMENT" && "language" in normalizedMessage -> {
                GeminiLiveErrorCategory.UNSUPPORTED_LANGUAGE
            }
            normalizedCode == "503" || normalizedCode == "UNAVAILABLE" -> GeminiLiveErrorCategory.PROVIDER_UNAVAILABLE
            else -> GeminiLiveErrorCategory.UNKNOWN
        }

        return GeminiLiveError(
            category = category,
            message = messageFor(category),
            providerCode = providerCode,
            isRecoverable = category == GeminiLiveErrorCategory.RATE_LIMITED ||
                category == GeminiLiveErrorCategory.PROVIDER_UNAVAILABLE ||
                category == GeminiLiveErrorCategory.UNKNOWN,
        )
    }

    fun fromThrowable(throwable: Throwable): GeminiLiveError {
        val category = when (throwable) {
            is SocketTimeoutException -> GeminiLiveErrorCategory.TIMEOUT
            is IOException -> GeminiLiveErrorCategory.NETWORK
            else -> GeminiLiveErrorCategory.UNKNOWN
        }
        return GeminiLiveError(
            category = category,
            message = messageFor(category),
            isRecoverable = true,
        )
    }

    private fun messageFor(category: GeminiLiveErrorCategory): String {
        return when (category) {
            GeminiLiveErrorCategory.MISSING_API_KEY -> "Configure Google API key in Settings > Subtitle"
            GeminiLiveErrorCategory.INVALID_API_KEY -> "Invalid Google API key"
            GeminiLiveErrorCategory.NO_PERMISSION -> "Google API key does not have Gemini Live permission"
            GeminiLiveErrorCategory.RATE_LIMITED -> "Gemini Live quota or rate limit reached"
            GeminiLiveErrorCategory.UNSUPPORTED_LANGUAGE -> "Gemini Live does not support the selected language"
            GeminiLiveErrorCategory.NETWORK -> "Gemini Live network error"
            GeminiLiveErrorCategory.TIMEOUT -> "Gemini Live connection timed out"
            GeminiLiveErrorCategory.PROVIDER_UNAVAILABLE -> "Gemini Live is unavailable"
            GeminiLiveErrorCategory.UNKNOWN -> "Gemini Live error"
        }
    }
}
