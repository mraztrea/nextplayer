package dev.anilbeesetti.nextplayer.core.subtitle.model

data class LookaheadSessionState(
    val generationId: Long = 0L,
    val playbackPositionMs: Long = 0L,
    val lookaheadCursorMs: Long = 0L,
    val bufferedUntilMs: Long = 0L,
    val targetLeadMs: Long = 5_000L,
    val lowWaterMs: Long = 3_000L,
    val highWaterMs: Long = 5_000L,
    val isPaused: Boolean = false,
    val isFallbackActive: Boolean = false,
    val activeAudioTrackKey: String? = null,
    val status: LookaheadPipelineStatus = LookaheadPipelineStatus.IDLE,
)
