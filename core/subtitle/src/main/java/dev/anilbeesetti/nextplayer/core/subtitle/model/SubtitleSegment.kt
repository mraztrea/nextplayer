package dev.anilbeesetti.nextplayer.core.subtitle.model

data class SubtitleSegment(
    val id: Long,
    val originalText: String,
    val translationText: String? = null,
    val status: SegmentStatus = SegmentStatus.ORIGINAL,
    val provider: SubtitleProvider = SubtitleProvider.SONIOX,
    val mediaId: String? = null,
    val displayText: String = translationText ?: originalText,
    val targetLanguageCode: String? = null,
    val isProvisional: Boolean = status == SegmentStatus.PROVISIONAL,
    val speaker: String? = null,
    val language: String? = null,
    val confidence: Float? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
