package dev.anilbeesetti.nextplayer.core.lan.security

class InMemoryLanCredentialStore : LanCredentialStore {
    private val passwords = mutableMapOf<String, String>()

    override suspend fun savePassword(key: String, password: String) {
        passwords[key] = password
    }

    override suspend fun getPassword(key: String): String? = passwords[key]

    override suspend fun hasPassword(key: String): Boolean = passwords.containsKey(key)

    override suspend fun clearPassword(key: String) {
        passwords.remove(key)
    }
}
