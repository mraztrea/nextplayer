package dev.anilbeesetti.nextplayer.core.data.mappers

import dev.anilbeesetti.nextplayer.core.database.entities.LanServerEntity
import dev.anilbeesetti.nextplayer.core.model.LanServerProfile

fun LanServerEntity.asExternalModel(): LanServerProfile = LanServerProfile(
    id = id,
    displayName = displayName,
    host = host,
    shareName = shareName,
    initialPath = initialPath,
    username = username,
    credentialKey = credentialKey,
    hasPassword = hasPassword,
    createdAt = createdAt,
    updatedAt = updatedAt,
    lastConnectedAt = lastConnectedAt,
)

fun LanServerProfile.asEntity(): LanServerEntity = LanServerEntity(
    id = id,
    displayName = displayName,
    host = host,
    shareName = shareName,
    initialPath = initialPath,
    username = username,
    credentialKey = credentialKey,
    hasPassword = hasPassword,
    createdAt = createdAt,
    updatedAt = updatedAt,
    lastConnectedAt = lastConnectedAt,
)
