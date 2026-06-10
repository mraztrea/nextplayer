package dev.anilbeesetti.nextplayer.core.subtitle.engine

import dev.anilbeesetti.nextplayer.core.data.repository.PreferencesRepository
import dev.anilbeesetti.nextplayer.core.model.ApplicationPreferences
import dev.anilbeesetti.nextplayer.core.model.PlayerPreferences
import dev.anilbeesetti.nextplayer.core.subtitle.model.OfflineModel
import dev.anilbeesetti.nextplayer.core.subtitle.model.OfflineModelState
import dev.anilbeesetti.nextplayer.core.subtitle.model.SubtitleEngineStatus
import dev.anilbeesetti.nextplayer.core.subtitle.storage.OfflineModelReadiness
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PrototypeOfflineSubtitleEngineTest {
    @Test
    fun startFailsWhenOfflineModelIsMissing() = runBlocking {
        val engine = PrototypeOfflineSubtitleEngine(
            modelReadiness = FakeModelReadiness(model = null),
            preferencesRepository = FakePreferencesRepository(),
        )

        val result = engine.start()

        assertTrue(result is SubtitleStartResult.Failed)
        assertEquals(SubtitleEngineStatus.ERROR, engine.status.value)
    }

    @Test
    fun startEmitsEnglishPrototypeTranslationWhenModelIsReady() = runBlocking {
        val engine = PrototypeOfflineSubtitleEngine(
            modelReadiness = FakeModelReadiness(model = readyModel()),
            preferencesRepository = FakePreferencesRepository(
                player = PlayerPreferences(
                    offlineTargetLanguage = PlayerPreferences.OFFLINE_SUBTITLE_TARGET_ENGLISH,
                ),
            ),
        )

        val result = engine.start()

        assertEquals(SubtitleStartResult.Started, result)
        assertEquals(SubtitleEngineStatus.ACTIVE, engine.status.value)
        assertEquals("This is an offline subtitle prototype.", engine.displaySegments.value.single().translationText)
    }

    @Test
    fun stopAndResetAreIdempotent() = runBlocking {
        val engine = PrototypeOfflineSubtitleEngine(
            modelReadiness = FakeModelReadiness(model = readyModel()),
            preferencesRepository = FakePreferencesRepository(),
        )

        engine.start()
        engine.stop()
        engine.stop()
        engine.resetSession()

        assertTrue(engine.displaySegments.value.isEmpty())
        assertEquals("", engine.provisionalText.value)
    }

    private class FakeModelReadiness(
        model: OfflineModel?,
    ) : OfflineModelReadiness {
        override val installedModel: StateFlow<OfflineModel?> = MutableStateFlow(model)
        override fun refreshInstalledModel() = Unit
        override fun isModelReady(): Boolean = installedModel.value?.isReady == true
    }

    private class FakePreferencesRepository(
        player: PlayerPreferences = PlayerPreferences(),
    ) : PreferencesRepository {
        override val applicationPreferences: StateFlow<ApplicationPreferences> =
            MutableStateFlow(ApplicationPreferences())
        override val playerPreferences: StateFlow<PlayerPreferences> = MutableStateFlow(player)

        override suspend fun updateApplicationPreferences(
            transform: suspend (ApplicationPreferences) -> ApplicationPreferences,
        ) = Unit

        override suspend fun updatePlayerPreferences(
            transform: suspend (PlayerPreferences) -> PlayerPreferences,
        ) = Unit

        override suspend fun resetPreferences() = Unit
    }

    private fun readyModel(): OfflineModel {
        return OfflineModel(
            id = "ja-vi-en-prototype",
            displayName = "Japanese Offline Prototype",
            version = "1",
            manifestVersion = 1,
            sourceLanguage = OfflineModel.JAPANESE_LANGUAGE,
            targetLanguages = setOf(
                OfflineModel.VIETNAMESE_LANGUAGE,
                OfflineModel.ENGLISH_LANGUAGE,
            ),
            capabilities = setOf(
                OfflineModel.JAPANESE_TRANSCRIPT_CAPABILITY,
                OfflineModel.JAPANESE_TO_VIETNAMESE_CAPABILITY,
                OfflineModel.JAPANESE_TO_ENGLISH_CAPABILITY,
            ),
            sizeBytes = 1,
            installedPath = "/tmp/model",
            state = OfflineModelState.READY,
        )
    }
}
