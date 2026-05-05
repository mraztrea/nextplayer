package dev.anilbeesetti.nextplayer.core.subtitle.storage

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lưu trữ Soniox API key an toàn sử dụng EncryptedSharedPreferences.
 * API key KHÔNG được lưu trong DataStore để tránh lộ dữ liệu nhạy cảm.
 */
@Singleton
class SecureApiKeyStorage @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val FILE_NAME = "subtitle_secure_prefs"
        private const val KEY_SONIOX_API_KEY = "soniox_api_key"
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
        prefs.edit().putString(KEY_SONIOX_API_KEY, apiKey).apply()
    }

    fun getApiKey(): String? {
        return prefs.getString(KEY_SONIOX_API_KEY, null)?.takeIf { it.isNotBlank() }
    }

    fun hasApiKey(): Boolean = getApiKey() != null

    fun clearApiKey() {
        prefs.edit().remove(KEY_SONIOX_API_KEY).apply()
    }
}
