package dev.anilbeesetti.nextplayer.feature.videopicker.lan

import dev.anilbeesetti.nextplayer.core.data.repository.LanBookmarkRepository
import dev.anilbeesetti.nextplayer.core.domain.lan.ManageLanBookmarksUseCases
import dev.anilbeesetti.nextplayer.core.model.LanError
import dev.anilbeesetti.nextplayer.core.model.LanErrorType
import dev.anilbeesetti.nextplayer.core.model.LanFolderBookmark
import dev.anilbeesetti.nextplayer.core.model.LanResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class LanBookmarkViewModelTest {

    @get:Rule
    val mainDispatcherRule = LanBookmarkMainDispatcherRule()

    @Test
    fun addCurrentFolderCreatesBookmark() = runTest {
        val repository = FakeBookmarkRepository()
        val viewModel = LanBookmarkViewModel(ManageLanBookmarksUseCases(repository))

        viewModel.addCurrentFolder("server-1", "Movies", "Movies")
        advanceUntilIdle()

        assertEquals("Movies", viewModel.uiState.value.bookmarks.single().displayName)
    }

    @Test
    fun deleteBookmarkRemovesBookmark() = runTest {
        val repository = FakeBookmarkRepository()
        val viewModel = LanBookmarkViewModel(ManageLanBookmarksUseCases(repository))
        viewModel.addCurrentFolder("server-1", "Movies", "Movies")
        advanceUntilIdle()
        val id = viewModel.uiState.value.bookmarks.single().id

        viewModel.deleteBookmark(id)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.bookmarks.isEmpty())
    }

    @Test
    fun duplicateFeedbackSetsErrorMessage() = runTest {
        val repository = FakeBookmarkRepository(duplicate = true)
        val viewModel = LanBookmarkViewModel(ManageLanBookmarksUseCases(repository))

        viewModel.addCurrentFolder("server-1", "Movies", "Movies")
        advanceUntilIdle()

        assertEquals(LanErrorType.Duplicate.name, (viewModel.uiState.value.message as LanBookmarkMessage.Error).typeName)
    }
}

private class FakeBookmarkRepository(
    private val duplicate: Boolean = false,
) : LanBookmarkRepository {
    private val bookmarks = MutableStateFlow<List<LanFolderBookmark>>(emptyList())

    override fun observeBookmarks(): MutableStateFlow<List<LanFolderBookmark>> = bookmarks

    override suspend fun saveBookmark(bookmark: LanFolderBookmark): LanResult<LanFolderBookmark> {
        if (duplicate) return LanResult.Failure(LanError(LanErrorType.Duplicate))
        bookmarks.value = bookmarks.value + bookmark
        return LanResult.Success(bookmark)
    }

    override suspend fun deleteBookmark(id: String) {
        bookmarks.value = bookmarks.value.filterNot { it.id == id }
    }

    override suspend fun markOpened(id: String, openedAt: Long) = Unit
}

@OptIn(ExperimentalCoroutinesApi::class)
class LanBookmarkMainDispatcherRule(
    private val dispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
