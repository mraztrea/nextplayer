package dev.anilbeesetti.nextplayer.core.subtitle.model

data class SubtitleSegment(
    val id: Long,
    val originalText: String,
    val translationText: String? = null,
    val status: SegmentStatus = SegmentStatus.ORIGINAL,
    val speaker: String? = null,
    val language: String? = null,
    val confidence: Float? = null,
    val sourceStartMs: Long = 0L,
    val sourceEndMs: Long = 0L,
    val targetStartMs: Long = 0L,
    val targetEndMs: Long = 0L,
    val generationId: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
)
