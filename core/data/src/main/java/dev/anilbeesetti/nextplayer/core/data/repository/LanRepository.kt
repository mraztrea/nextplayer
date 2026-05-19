package dev.anilbeesetti.nextplayer.core.data.repository

import dev.anilbeesetti.nextplayer.core.model.LanFolder
import dev.anilbeesetti.nextplayer.core.model.LanFolderBookmark
import dev.anilbeesetti.nextplayer.core.model.LanResult
import dev.anilbeesetti.nextplayer.core.model.LanServerProfile
import kotlinx.coroutines.flow.Flow

interface LanServerRepository {
    fun observeServers(): Flow<List<LanServerProfile>>
    suspend fun getServer(id: String): LanServerProfile?
    suspend fun saveServer(profile: LanServerProfile, password: String?): LanResult<LanServerProfile>
    suspend fun deleteServer(id: String)
}

interface LanFolderRepository {
    suspend fun listFolder(serverId: String, folderPath: String): LanResult<LanFolder>
}

interface LanBookmarkRepository {
    fun observeBookmarks(): Flow<List<LanFolderBookmark>>
    suspend fun saveBookmark(bookmark: LanFolderBookmark): LanResult<LanFolderBookmark>
    suspend fun deleteBookmark(id: String)
    suspend fun markOpened(id: String, openedAt: Long)
}
