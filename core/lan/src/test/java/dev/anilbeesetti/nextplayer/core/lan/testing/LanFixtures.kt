package dev.anilbeesetti.nextplayer.core.lan.testing

import dev.anilbeesetti.nextplayer.core.model.LanFolder
import dev.anilbeesetti.nextplayer.core.model.LanFolderBookmark
import dev.anilbeesetti.nextplayer.core.model.LanMediaItem
import dev.anilbeesetti.nextplayer.core.model.LanMediaItemType
import dev.anilbeesetti.nextplayer.core.model.LanServerProfile

object LanFixtures {
    const val now: Long = 1_774_000_000_000

    val server = LanServerProfile(
        id = "server-1",
        displayName = "NAS phim",
        host = "192.168.1.10",
        shareName = "Videos",
        username = "viewer",
        credentialKey = "lan-server-server-1",
        hasPassword = true,
        createdAt = now,
        updatedAt = now,
    )

    val folder = LanFolder(
        serverId = server.id,
        path = "Movies",
        displayPath = "Videos/Movies",
        items = listOf(
            LanMediaItem(
                serverId = server.id,
                path = "Movies/one.mp4",
                name = "one.mp4",
                type = LanMediaItemType.Video,
                sizeBytes = 1024,
                modifiedAt = now,
            ),
            LanMediaItem(
                serverId = server.id,
                path = "Movies/Series",
                name = "Series",
                type = LanMediaItemType.Folder,
            ),
        ),
        loadedAt = now,
    )

    val bookmark = LanFolderBookmark(
        id = "bookmark-1",
        serverId = server.id,
        folderPath = folder.path,
        displayName = "Movies",
        createdAt = now,
        updatedAt = now,
    )
}
