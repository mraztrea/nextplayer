package dev.anilbeesetti.nextplayer.core.subtitle.model

data class LookaheadAudioChunk(
    val sequenceId: Long,
    val generationId: Long,
    val mediaStartMs: Long,
    val mediaEndMs: Long,
    val sampleRate: Int,
    val channelCount: Int,
    val pcmSizeBytes: Int,
)
