package dev.anilbeesetti.nextplayer.core.model

data class LanThumbnailCacheEntry(
    val cacheKey: String,
    val serverId: String,
    val mediaPath: String,
    val thumbnailFilePath: String,
    val sizeBytes: Long? = null,
    val modifiedAt: Long? = null,
    val createdAt: Long,
    val lastUsedAt: Long,
)
