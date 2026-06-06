package com.bocatta.pos.security

import com.bocatta.pos.domain.usecase.AuthorizationManager
import com.bocatta.pos.data.security.SharedPreferencesPinRateLimitStore
import com.bocatta.pos.domain.usecase.PinRateLimitStore
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PinAuthorizationRegressionTest {

    @Test
    fun generatedPinHashIsDocumentSafeAndNotPlaintext() {
        val hash = AuthorizationManager.hashPinForStorage("123456")

        assertFalse(hash.contains("123456"))
        assertTrue(hash.matches(Regex("[a-f0-9]{64}")))
    }

    @Test
    fun authorizationManagerHasValidarPinDesbloqueo() {
        val methods = AuthorizationManager::class.java.methods
        val encontrado = methods.any { it.name == "validarPinDesbloqueo" }
        assertTrue("AuthorizationManager debe tener método validarPinDesbloqueo", encontrado)
    }

    @Test
    fun authorizationManagerHasVerificarPermiso() {
        val methods = AuthorizationManager::class.java.methods
        val encontrado = methods.any { it.name == "verificarPermiso" }
        assertTrue("AuthorizationManager debe tener método verificarPermiso", encontrado)
    }

    @Test
    fun pinRateLimitStoreInterfaceExists() {
        assertNotNull(PinRateLimitStore::class.java)
    }

    @Test
    fun sharedPrefsRateLimitStoreExists() {
        assertNotNull(SharedPreferencesPinRateLimitStore::class.java)
    }

    @Test
    fun authorizationManagerHasHashPinForStorage() {
        val method = AuthorizationManager.Companion::class.java.getDeclaredMethod(
            "hashPinForStorage", String::class.java
        )
        assertNotNull(method)
    }
}
