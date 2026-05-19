package dev.anilbeesetti.nextplayer.core.model

data class LanFolder(
    val serverId: String,
    val path: String,
    val displayPath: String,
    val parentPath: String? = null,
    val items: List<LanMediaItem> = emptyList(),
    val loadedAt: Long,
)
