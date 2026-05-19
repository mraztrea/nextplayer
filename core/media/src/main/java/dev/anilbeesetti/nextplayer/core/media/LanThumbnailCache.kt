package dev.anilbeesetti.nextplayer.core.media

import dev.anilbeesetti.nextplayer.core.database.dao.LanThumbnailCacheDao
import dev.anilbeesetti.nextplayer.core.database.entities.LanThumbnailCacheEntity
import dev.anilbeesetti.nextplayer.core.model.LanMediaItem
import dev.anilbeesetti.nextplayer.core.model.LanThumbnailCacheEntry
import java.security.MessageDigest
import javax.inject.Inject

class LanThumbnailCache @Inject constructor(
    private val lanThumbnailCacheDao: LanThumbnailCacheDao,
) {
    suspend fun get(item: LanMediaItem): LanThumbnailCacheEntry? {
        val cacheKey = item.cacheKey()
        val entity = lanThumbnailCacheDao.get(cacheKey) ?: return null
        return entity.takeIf {
            it.sizeBytes == item.sizeBytes && it.modifiedAt == item.modifiedAt
        }?.asExternalModel()
    }

    suspend fun put(
        item: LanMediaItem,
        thumbnailFilePath: String,
        now: Long = System.currentTimeMillis(),
    ): LanThumbnailCacheEntry {
        val entry = LanThumbnailCacheEntry(
            cacheKey = item.cacheKey(),
            serverId = item.serverId,
            mediaPath = item.path,
            thumbnailFilePath = thumbnailFilePath,
            sizeBytes = item.sizeBytes,
            modifiedAt = item.modifiedAt,
            createdAt = now,
            lastUsedAt = now,
        )
        lanThumbnailCacheDao.upsert(entry.asEntity())
        return entry
    }

    suspend fun invalidate(item: LanMediaItem) {
        lanThumbnailCacheDao.delete(item.cacheKey())
    }

    private fun LanMediaItem.cacheKey(): String {
        val raw = "$serverId|$path|$sizeBytes|$modifiedAt"
        val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun LanThumbnailCacheEntity.asExternalModel(): LanThumbnailCacheEntry =
        LanThumbnailCacheEntry(
            cacheKey = cacheKey,
            serverId = serverId,
            mediaPath = mediaPath,
            thumbnailFilePath = thumbnailFilePath,
            sizeBytes = sizeBytes,
            modifiedAt = modifiedAt,
            createdAt = createdAt,
            lastUsedAt = lastUsedAt,
        )

    private fun LanThumbnailCacheEntry.asEntity(): LanThumbnailCacheEntity =
        LanThumbnailCacheEntity(
            cacheKey = cacheKey,
            serverId = serverId,
            mediaPath = mediaPath,
            thumbnailFilePath = thumbnailFilePath,
            sizeBytes = sizeBytes,
            modifiedAt = modifiedAt,
            createdAt = createdAt,
            lastUsedAt = lastUsedAt,
        )
}
