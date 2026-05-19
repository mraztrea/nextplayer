package dev.anilbeesetti.nextplayer.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import dev.anilbeesetti.nextplayer.core.database.entities.LanThumbnailCacheEntity

@Dao
interface LanThumbnailCacheDao {

    @Query("SELECT * FROM lan_thumbnail_cache WHERE cache_key = :cacheKey")
    suspend fun get(cacheKey: String): LanThumbnailCacheEntity?

    @Upsert
    suspend fun upsert(entry: LanThumbnailCacheEntity)

    @Query("DELETE FROM lan_thumbnail_cache WHERE cache_key = :cacheKey")
    suspend fun delete(cacheKey: String)
}
