package dev.anilbeesetti.nextplayer.settings.screens.subtitle

import dev.anilbeesetti.nextplayer.core.data.repository.fake.FakePreferencesRepository
import dev.anilbeesetti.nextplayer.core.subtitle.model.GeminiLanguageMapper
import dev.anilbeesetti.nextplayer.core.subtitle.model.SubtitleDisplayMode
import dev.anilbeesetti.nextplayer.core.subtitle.model.SubtitleProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SubtitlePreferencesViewModelTest {
    @Test
    fun geminiProviderKeyLanguageAndDisplayModePersistSeparatelyFromSoniox() = runBlocking {
        val repository = FakePreferencesRepository()

        repository.updatePlayerPreferences {
            it.copy(
                liveSubtitleProvider = SubtitleProvider.GEMINI_LIVE.name,
                geminiTargetLanguage = GeminiLanguageMapper.toTargetLanguageCode(
                    SubtitlePreferencesTestFixtures.vietnameseAlias,
                ),
                geminiDisplayMode = SubtitleDisplayMode.BILINGUAL.name,
                hasGoogleApiKeyConfigured = true,
                hasApiKeyConfigured = false,
            )
        }

        val preferences = repository.playerPreferences.value
        assertEquals(SubtitleProvider.GEMINI_LIVE.name, preferences.liveSubtitleProvider)
        assertEquals(SubtitlePreferencesTestFixtures.vietnameseLanguageCode, preferences.geminiTargetLanguage)
        assertEquals(SubtitleDisplayMode.BILINGUAL.name, preferences.geminiDisplayMode)
        assertTrue(preferences.hasGoogleApiKeyConfigured)
        assertFalse(preferences.hasApiKeyConfigured)
    }
}
