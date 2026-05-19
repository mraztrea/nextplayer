package dev.anilbeesetti.nextplayer.core.data

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.anilbeesetti.nextplayer.core.data.repository.LocalMediaRepository
import dev.anilbeesetti.nextplayer.core.data.repository.LanBookmarkRepository
import dev.anilbeesetti.nextplayer.core.data.repository.LanFolderRepository
import dev.anilbeesetti.nextplayer.core.data.repository.LocalLanBookmarkRepository
import dev.anilbeesetti.nextplayer.core.data.repository.LocalLanServerRepository
import dev.anilbeesetti.nextplayer.core.data.repository.LocalLanFolderRepository
import dev.anilbeesetti.nextplayer.core.data.repository.LocalPreferencesRepository
import dev.anilbeesetti.nextplayer.core.data.repository.LocalSearchHistoryRepository
import dev.anilbeesetti.nextplayer.core.data.repository.LanServerRepository
import dev.anilbeesetti.nextplayer.core.data.repository.MediaRepository
import dev.anilbeesetti.nextplayer.core.data.repository.PreferencesRepository
import dev.anilbeesetti.nextplayer.core.data.repository.SearchHistoryRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface DataModule {

    @Binds
    fun bindsMediaRepository(
        videoRepository: LocalMediaRepository,
    ): MediaRepository

    @Binds
    @Singleton
    fun bindsPreferencesRepository(
        preferencesRepository: LocalPreferencesRepository,
    ): PreferencesRepository

    @Binds
    @Singleton
    fun bindsSearchHistoryRepository(
        searchHistoryRepository: LocalSearchHistoryRepository,
    ): SearchHistoryRepository

    @Binds
    @Singleton
    fun bindsLanServerRepository(
        repository: LocalLanServerRepository,
    ): LanServerRepository

    @Binds
    @Singleton
    fun bindsLanFolderRepository(
        repository: LocalLanFolderRepository,
    ): LanFolderRepository

    @Binds
    @Singleton
    fun bindsLanBookmarkRepository(
        repository: LocalLanBookmarkRepository,
    ): LanBookmarkRepository
}
