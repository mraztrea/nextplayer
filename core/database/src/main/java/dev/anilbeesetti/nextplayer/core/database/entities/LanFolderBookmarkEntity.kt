package dev.anilbeesetti.nextplayer.core.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "lan_folder_bookmarks",
    foreignKeys = [
        ForeignKey(
            entity = LanServerEntity::class,
            parentColumns = ["id"],
            childColumns = ["server_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["server_id"]),
        Index(value = ["server_id", "folder_path"], unique = true),
    ],
)
data class LanFolderBookmarkEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "server_id")
    val serverId: String,
    @ColumnInfo(name = "folder_path")
    val folderPath: String,
    @ColumnInfo(name = "display_name")
    val displayName: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
    @ColumnInfo(name = "last_opened_at")
    val lastOpenedAt: Long?,
)
