package dev.anilbeesetti.nextplayer.core.subtitle.model

enum class LookaheadPipelineStatus {
    IDLE,
    WARMING,
    READY,
    THROTTLED,
    RESETTING,
    FALLBACK,
    ERROR,
}
