package dev.anilbeesetti.nextplayer.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import dev.anilbeesetti.nextplayer.core.database.entities.LanServerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LanServerDao {

    @Query("SELECT * FROM lan_servers ORDER BY display_name COLLATE NOCASE")
    fun observeAll(): Flow<List<LanServerEntity>>

    @Query("SELECT * FROM lan_servers WHERE id = :id")
    suspend fun get(id: String): LanServerEntity?

    @Query(
        """
        SELECT * FROM lan_servers
        WHERE host = :host
            AND share_name = :shareName
            AND initial_path = :initialPath
            AND username = :username
            AND id != :excludeId
        LIMIT 1
        """,
    )
    suspend fun findDuplicate(
        host: String,
        shareName: String,
        initialPath: String,
        username: String,
        excludeId: String,
    ): LanServerEntity?

    @Upsert
    suspend fun upsert(server: LanServerEntity)

    @Query("DELETE FROM lan_servers WHERE id = :id")
    suspend fun delete(id: String)
}
