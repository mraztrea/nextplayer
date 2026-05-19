package dev.anilbeesetti.nextplayer.core.model

sealed interface LanResult<out T> {
    data class Success<T>(val value: T) : LanResult<T>
    data class Failure(val error: LanError) : LanResult<Nothing>
}

data class LanError(
    val type: LanErrorType,
    val message: String? = null,
    val cause: Throwable? = null,
)

enum class LanErrorType {
    InvalidInput,
    AuthenticationFailed,
    ServerUnreachable,
    ShareNotFound,
    PermissionDenied,
    PathNotFound,
    UnsupportedMedia,
    ReadInterrupted,
    Duplicate,
    Unknown,
}
