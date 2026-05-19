package dev.anilbeesetti.nextplayer.core.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "lan_thumbnail_cache",
    indices = [
        Index(value = ["server_id", "media_path"]),
    ],
)
data class LanThumbnailCacheEntity(
    @PrimaryKey
    @ColumnInfo(name = "cache_key")
    val cacheKey: String,
    @ColumnInfo(name = "server_id")
    val serverId: String,
    @ColumnInfo(name = "media_path")
    val mediaPath: String,
    @ColumnInfo(name = "thumbnail_file_path")
    val thumbnailFilePath: String,
    @ColumnInfo(name = "size_bytes")
    val sizeBytes: Long?,
    @ColumnInfo(name = "modified_at")
    val modifiedAt: Long?,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "last_used_at")
    val lastUsedAt: Long,
)
