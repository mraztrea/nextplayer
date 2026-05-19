package dev.anilbeesetti.nextplayer.core.lan.security

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EncryptedLanCredentialStoreTest {

    @Test
    fun saveUpdateAndClearPassword() = runTest {
        val store = InMemoryLanCredentialStore()

        store.savePassword("credential-1", "old")
        assertTrue(store.hasPassword("credential-1"))
        assertEquals("old", store.getPassword("credential-1"))

        store.savePassword("credential-1", "new")
        assertEquals("new", store.getPassword("credential-1"))

        store.clearPassword("credential-1")
        assertFalse(store.hasPassword("credential-1"))
    }
}
