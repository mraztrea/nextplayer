package dev.anilbeesetti.nextplayer.core.subtitle.storage

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.anilbeesetti.nextplayer.core.subtitle.model.SubtitleProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lưu trữ provider API key an toàn sử dụng EncryptedSharedPreferences.
 * API key KHÔNG được lưu trong DataStore để tránh lộ dữ liệu nhạy cảm.
 */
@Singleton
class SecureApiKeyStorage @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val FILE_NAME = "subtitle_secure_prefs"
        private const val KEY_SONIOX_API_KEY = "soniox_api_key"
        private const val KEY_GOOGLE_API_KEY = "google_api_key"
    }

    private val prefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun saveApiKey(apiKey: String) {
        saveApiKey(SubtitleProvider.SONIOX, apiKey)
    }

    fun saveApiKey(provider: SubtitleProvider, apiKey: String) {
        prefs.edit().putString(keyFor(provider), apiKey).apply()
    }

    fun getApiKey(): String? {
        return getApiKey(SubtitleProvider.SONIOX)
    }

    fun getApiKey(provider: SubtitleProvider): String? {
        return prefs.getString(keyFor(provider), null)?.takeIf { it.isNotBlank() }
    }

    fun hasApiKey(): Boolean = getApiKey() != null

    fun hasApiKey(provider: SubtitleProvider): Boolean = getApiKey(provider) != null

    fun clearApiKey() {
        clearApiKey(SubtitleProvider.SONIOX)
    }

    fun clearApiKey(provider: SubtitleProvider) {
        prefs.edit().remove(keyFor(provider)).apply()
    }

    private fun keyFor(provider: SubtitleProvider): String {
        return when (provider) {
            SubtitleProvider.SONIOX -> KEY_SONIOX_API_KEY
            SubtitleProvider.GEMINI_LIVE -> KEY_GOOGLE_API_KEY
        }
    }
}
