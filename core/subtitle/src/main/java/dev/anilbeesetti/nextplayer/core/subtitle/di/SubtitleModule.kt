package dev.anilbeesetti.nextplayer.core.subtitle.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.anilbeesetti.nextplayer.core.subtitle.audio.LookaheadAudioPipeline
import dev.anilbeesetti.nextplayer.core.subtitle.audio.MediaCodecLookaheadAudioPipeline
import dev.anilbeesetti.nextplayer.core.subtitle.engine.SubtitleEngine
import dev.anilbeesetti.nextplayer.core.subtitle.engine.SubtitleEngineImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SubtitleScope

@Module
@InstallIn(SingletonComponent::class)
object SubtitleModule {

    @Provides
    @Singleton
    @SubtitleScope
    fun provideSubtitleOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.MILLISECONDS) // No timeout for WebSocket
            .writeTimeout(10, TimeUnit.SECONDS)
            .connectTimeout(10, TimeUnit.SECONDS)
            .pingInterval(15, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @SubtitleScope
    fun provideSubtitleCoroutineScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class SubtitleBindsModule {

    @Binds
    @Singleton
    abstract fun bindSubtitleEngine(impl: SubtitleEngineImpl): SubtitleEngine

    @Binds
    @Singleton
    abstract fun bindLookaheadAudioPipeline(impl: MediaCodecLookaheadAudioPipeline): LookaheadAudioPipeline
}
