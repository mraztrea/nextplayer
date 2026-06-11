package dev.anilbeesetti.nextplayer.core.subtitle.model

data class GeminiLiveError(
    val category: GeminiLiveErrorCategory,
    val message: String,
    val providerCode: String? = null,
    val isRecoverable: Boolean = false,
)

enum class GeminiLiveErrorCategory {
    MISSING_API_KEY,
    INVALID_API_KEY,
    NO_PERMISSION,
    RATE_LIMITED,
    UNSUPPORTED_LANGUAGE,
    NETWORK,
    TIMEOUT,
    PROVIDER_UNAVAILABLE,
    UNKNOWN,
}
