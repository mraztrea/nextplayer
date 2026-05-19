package dev.anilbeesetti.nextplayer.core.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "lan_servers",
    indices = [
        Index(
            value = ["host", "share_name", "initial_path", "username"],
            unique = true,
        ),
    ],
)
data class LanServerEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "display_name")
    val displayName: String,
    @ColumnInfo(name = "host")
    val host: String,
    @ColumnInfo(name = "share_name")
    val shareName: String,
    @ColumnInfo(name = "initial_path")
    val initialPath: String,
    @ColumnInfo(name = "username")
    val username: String,
    @ColumnInfo(name = "credential_key")
    val credentialKey: String?,
    @ColumnInfo(name = "has_password")
    val hasPassword: Boolean,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
    @ColumnInfo(name = "last_connected_at")
    val lastConnectedAt: Long?,
)
