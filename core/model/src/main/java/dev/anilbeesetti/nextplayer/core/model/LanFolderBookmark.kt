package dev.anilbeesetti.nextplayer.core.model

data class LanFolderBookmark(
    val id: String,
    val serverId: String,
    val folderPath: String,
    val displayName: String,
    val createdAt: Long,
    val updatedAt: Long,
    val lastOpenedAt: Long? = null,
)
