package dev.anilbeesetti.nextplayer.core.database

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.anilbeesetti.nextplayer.core.database.dao.DirectoryDao
import dev.anilbeesetti.nextplayer.core.database.dao.LanFolderBookmarkDao
import dev.anilbeesetti.nextplayer.core.database.dao.LanServerDao
import dev.anilbeesetti.nextplayer.core.database.dao.LanThumbnailCacheDao
import dev.anilbeesetti.nextplayer.core.database.dao.MediumDao

@Module
@InstallIn(SingletonComponent::class)
object DaoModule {

    @Provides
    fun provideMediumDao(db: MediaDatabase): MediumDao = db.mediumDao()

    @Provides
    fun provideMediumStateDao(db: MediaDatabase) = db.mediumStateDao()

    @Provides
    fun provideDirectoryDao(db: MediaDatabase): DirectoryDao = db.directoryDao()

    @Provides
    fun provideLanServerDao(db: MediaDatabase): LanServerDao = db.lanServerDao()

    @Provides
    fun provideLanThumbnailCacheDao(db: MediaDatabase): LanThumbnailCacheDao = db.lanThumbnailCacheDao()

    @Provides
    fun provideLanFolderBookmarkDao(db: MediaDatabase): LanFolderBookmarkDao = db.lanFolderBookmarkDao()
}
