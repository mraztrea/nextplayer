package dev.anilbeesetti.nextplayer.feature.videopicker.lan

import dev.anilbeesetti.nextplayer.core.data.repository.LanFolderRepository
import dev.anilbeesetti.nextplayer.core.domain.lan.BrowseLanFolderUseCase
import dev.anilbeesetti.nextplayer.core.model.LanError
import dev.anilbeesetti.nextplayer.core.model.LanErrorType
import dev.anilbeesetti.nextplayer.core.model.LanFolder
import dev.anilbeesetti.nextplayer.core.model.LanMediaItem
import dev.anilbeesetti.nextplayer.core.model.LanMediaItemType
import dev.anilbeesetti.nextplayer.core.model.LanResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class LanFolderBrowserViewModelTest {

    @get:Rule
    val mainDispatcherRule = LanFolderMainDispatcherRule()

    @Test
    fun loadFolderShowsLoadedState() = runTest {
        val viewModel = LanFolderBrowserViewModel(BrowseLanFolderUseCase(FakeFolderRepository(sampleFolder())))

        viewModel.load("server-1", "")
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.loading)
        assertEquals(2, viewModel.uiState.value.folder?.items?.size)
    }

    @Test
    fun loadEmptyFolderShowsLoadedEmptyState() = runTest {
        val viewModel = LanFolderBrowserViewModel(
            BrowseLanFolderUseCase(FakeFolderRepository(sampleFolder(items = emptyList()))),
        )

        viewModel.load("server-1", "Empty")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.folder!!.items.isEmpty())
    }

    @Test
    fun loadFolderShowsErrorState() = runTest {
        val viewModel = LanFolderBrowserViewModel(
            BrowseLanFolderUseCase(FakeFolderRepository(errorType = LanErrorType.PermissionDenied)),
        )

        viewModel.load("server-1", "Private")
        advanceUntilIdle()

        assertEquals(LanErrorType.PermissionDenied.name, viewModel.uiState.value.error)
    }
}

private fun sampleFolder(
    items: List<LanMediaItem> = listOf(
        LanMediaItem("server-1", "Movies", "Movies", LanMediaItemType.Folder),
        LanMediaItem("server-1", "clip.mp4", "clip.mp4", LanMediaItemType.Video),
    ),
) = LanFolder(
    serverId = "server-1",
    path = "",
    displayPath = "NAS phim",
    items = items,
    loadedAt = 1,
)

private class FakeFolderRepository(
    private val folder: LanFolder? = null,
    private val errorType: LanErrorType? = null,
) : LanFolderRepository {
    override suspend fun listFolder(serverId: String, folderPath: String): LanResult<LanFolder> {
        errorType?.let { return LanResult.Failure(LanError(it)) }
        return LanResult.Success(folder!!.copy(serverId = serverId, path = folderPath))
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class LanFolderMainDispatcherRule(
    private val dispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
