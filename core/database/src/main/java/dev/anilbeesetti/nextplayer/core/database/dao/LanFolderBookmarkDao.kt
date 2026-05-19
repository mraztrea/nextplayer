package dev.anilbeesetti.nextplayer.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import dev.anilbeesetti.nextplayer.core.database.entities.LanFolderBookmarkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LanFolderBookmarkDao {

    @Query("SELECT * FROM lan_folder_bookmarks ORDER BY last_opened_at DESC, updated_at DESC")
    fun observeAll(): Flow<List<LanFolderBookmarkEntity>>

    @Query("SELECT * FROM lan_folder_bookmarks WHERE id = :id")
    suspend fun get(id: String): LanFolderBookmarkEntity?

    @Query(
        """
        SELECT * FROM lan_folder_bookmarks
        WHERE server_id = :serverId AND folder_path = :folderPath AND id != :excludeId
        LIMIT 1
        """,
    )
    suspend fun findDuplicate(
        serverId: String,
        folderPath: String,
        excludeId: String,
    ): LanFolderBookmarkEntity?

    @Upsert
    suspend fun upsert(bookmark: LanFolderBookmarkEntity)

    @Query("DELETE FROM lan_folder_bookmarks WHERE id = :id")
    suspend fun delete(id: String)

    @Query("UPDATE lan_folder_bookmarks SET last_opened_at = :openedAt WHERE id = :id")
    suspend fun markOpened(id: String, openedAt: Long)
}
