package dev.anilbeesetti.nextplayer.core.subtitle.model

enum class SubtitleEngineStatus {
    IDLE,
    NOT_CONFIGURED,
    CONNECTING,
    ACTIVE,
    TRANSLATING,
    RESETTING,
    RECONNECTING,
    ERROR,
    GEMINI_ERROR,
    STOPPED,
}
