package dev.anilbeesetti.nextplayer.core.domain.lan

import dev.anilbeesetti.nextplayer.core.data.repository.LanFolderRepository
import dev.anilbeesetti.nextplayer.core.model.LanFolder
import dev.anilbeesetti.nextplayer.core.model.LanMediaItem
import dev.anilbeesetti.nextplayer.core.model.LanMediaItemType
import dev.anilbeesetti.nextplayer.core.model.LanResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowseLanFolderUseCaseTest {

    @Test
    fun loadsRootFolder() = runTest {
        val useCase = BrowseLanFolderUseCase(FakeLanFolderRepository(sampleFolder(path = "")))

        val folder = (useCase(serverId = "server-1") as LanResult.Success).value

        assertEquals("", folder.path)
        assertEquals(2, folder.items.size)
    }

    @Test
    fun loadsChildPath() = runTest {
        val useCase = BrowseLanFolderUseCase(FakeLanFolderRepository(sampleFolder(path = "Movies/Action")))

        val folder = (useCase(serverId = "server-1", folderPath = "Movies/Action") as LanResult.Success).value

        assertEquals("Movies/Action", folder.path)
    }

    @Test
    fun returnsLoadedEmptyFolder() = runTest {
        val useCase = BrowseLanFolderUseCase(FakeLanFolderRepository(sampleFolder(path = "", items = emptyList())))

        val folder = (useCase(serverId = "server-1") as LanResult.Success).value

        assertTrue(folder.items.isEmpty())
    }

    @Test
    fun keepsFoldersBeforeVideos() = runTest {
        val useCase = BrowseLanFolderUseCase(
            FakeLanFolderRepository(
                sampleFolder(
                    items = listOf(
                        LanMediaItem("server-1", "a.mp4", "a.mp4", LanMediaItemType.Video),
                        LanMediaItem("server-1", "Folder", "Folder", LanMediaItemType.Folder),
                    ),
                ),
            ),
        )

        val folder = (useCase(serverId = "server-1") as LanResult.Success).value

        assertEquals(LanMediaItemType.Folder, folder.items.first().type)
    }
}

private fun sampleFolder(
    path: String = "",
    items: List<LanMediaItem> = listOf(
        LanMediaItem("server-1", "Movies", "Movies", LanMediaItemType.Folder),
        LanMediaItem("server-1", "clip.mp4", "clip.mp4", LanMediaItemType.Video),
    ),
) = LanFolder(
    serverId = "server-1",
    path = path,
    displayPath = path.ifBlank { "NAS phim" },
    items = items,
    loadedAt = 1,
)

private class FakeLanFolderRepository(
    private val folder: LanFolder,
) : LanFolderRepository {
    override suspend fun listFolder(serverId: String, folderPath: String): LanResult<LanFolder> =
        LanResult.Success(folder.copy(serverId = serverId, path = folderPath))
}
