package dev.anilbeesetti.nextplayer.core.media

import dev.anilbeesetti.nextplayer.core.database.dao.LanThumbnailCacheDao
import dev.anilbeesetti.nextplayer.core.database.entities.LanThumbnailCacheEntity
import dev.anilbeesetti.nextplayer.core.model.LanMediaItem
import dev.anilbeesetti.nextplayer.core.model.LanMediaItemType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LanThumbnailCacheTest {

    @Test
    fun lazyLoadReturnsNullOnCacheMiss() = runTest {
        val cache = LanThumbnailCache(FakeLanThumbnailCacheDao())

        assertNull(cache.get(sampleItem))
    }

    @Test
    fun cacheHitReturnsStoredEntry() = runTest {
        val cache = LanThumbnailCache(FakeLanThumbnailCacheDao())

        cache.put(sampleItem, thumbnailFilePath = "/thumbs/a.jpg", now = 1)

        assertEquals("/thumbs/a.jpg", cache.get(sampleItem)?.thumbnailFilePath)
    }

    @Test
    fun cacheInvalidatesWhenMetadataChanges() = runTest {
        val cache = LanThumbnailCache(FakeLanThumbnailCacheDao())

        cache.put(sampleItem, thumbnailFilePath = "/thumbs/a.jpg", now = 1)

        assertNull(cache.get(sampleItem.copy(sizeBytes = 20)))
    }

    @Test
    fun explicitInvalidationRemovesEntry() = runTest {
        val cache = LanThumbnailCache(FakeLanThumbnailCacheDao())

        cache.put(sampleItem, thumbnailFilePath = "/thumbs/a.jpg", now = 1)
        cache.invalidate(sampleItem)

        assertNull(cache.get(sampleItem))
    }
}

private val sampleItem = LanMediaItem(
    serverId = "server-1",
    path = "clip.mp4",
    name = "clip.mp4",
    type = LanMediaItemType.Video,
    sizeBytes = 10,
    modifiedAt = 1,
)

private class FakeLanThumbnailCacheDao : LanThumbnailCacheDao {
    private val entries = mutableMapOf<String, LanThumbnailCacheEntity>()

    override suspend fun get(cacheKey: String): LanThumbnailCacheEntity? = entries[cacheKey]

    override suspend fun upsert(entry: LanThumbnailCacheEntity) {
        entries[entry.cacheKey] = entry
    }

    override suspend fun delete(cacheKey: String) {
        entries.remove(cacheKey)
    }
}
