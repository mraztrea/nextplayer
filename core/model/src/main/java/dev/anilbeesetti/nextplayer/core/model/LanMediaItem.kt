package dev.anilbeesetti.nextplayer.core.model

data class LanMediaItem(
    val serverId: String,
    val path: String,
    val name: String,
    val type: LanMediaItemType,
    val sizeBytes: Long? = null,
    val modifiedAt: Long? = null,
    val thumbnailState: LanThumbnailState = LanThumbnailState.NotRequested,
    val playbackUri: String? = null,
)

enum class LanMediaItemType {
    Folder,
    Video,
}

enum class LanThumbnailState {
    NotRequested,
    Loading,
    Ready,
    Failed,
}
