package dev.anilbeesetti.nextplayer.core.lan.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

interface LanCredentialStore {
    suspend fun savePassword(key: String, password: String)
    suspend fun getPassword(key: String): String?
    suspend fun hasPassword(key: String): Boolean
    suspend fun clearPassword(key: String)
}

@Singleton
class EncryptedLanCredentialStore @Inject constructor(
    @ApplicationContext context: Context,
) : LanCredentialStore {

    private val preferences = EncryptedSharedPreferences.create(
        context,
        FILE_NAME,
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    override suspend fun savePassword(key: String, password: String) {
        preferences.edit().putString(key, password).apply()
    }

    override suspend fun getPassword(key: String): String? = preferences.getString(key, null)

    override suspend fun hasPassword(key: String): Boolean = preferences.contains(key)

    override suspend fun clearPassword(key: String) {
        preferences.edit().remove(key).apply()
    }

    private companion object {
        const val FILE_NAME = "lan_credentials"
    }
}

@Module
@InstallIn(SingletonComponent::class)
interface LanCredentialModule {
    @Binds
    fun bindLanCredentialStore(
        store: EncryptedLanCredentialStore,
    ): LanCredentialStore
}
