package dev.anilbeesetti.nextplayer.core.subtitle.model

enum class CredentialValidationState {
    UNKNOWN,
    VALIDATING,
    VALID,
    MISSING,
    INVALID,
    NO_PERMISSION,
    RATE_LIMITED,
}
