package dev.anilbeesetti.nextplayer.core.data.mappers

import dev.anilbeesetti.nextplayer.core.database.entities.LanFolderBookmarkEntity
import dev.anilbeesetti.nextplayer.core.model.LanFolderBookmark

fun LanFolderBookmarkEntity.asExternalModel(): LanFolderBookmark =
    LanFolderBookmark(
        id = id,
        serverId = serverId,
        folderPath = folderPath,
        displayName = displayName,
        createdAt = createdAt,
        updatedAt = updatedAt,
        lastOpenedAt = lastOpenedAt,
    )

fun LanFolderBookmark.asEntity(): LanFolderBookmarkEntity =
    LanFolderBookmarkEntity(
        id = id,
        serverId = serverId,
        folderPath = folderPath,
        displayName = displayName,
        createdAt = createdAt,
        updatedAt = updatedAt,
        lastOpenedAt = lastOpenedAt,
    )
