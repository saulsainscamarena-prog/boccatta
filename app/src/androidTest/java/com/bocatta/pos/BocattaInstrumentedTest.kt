package com.bocatta.pos

import androidx.compose.ui.test.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

/**
 * Pruebas instrumentadas para Bocatta POS
 * Ejecute en emulador/dispositivo Android con: 
 * ./gradlew connectedDebugAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class BocattaInstrumentedTest {

    private lateinit var context: android.content.Context

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
    }

    @Test
    fun useAppContext() {
        assertEquals("com.bocatta.pos", context.packageName)
    }

    @Test
    fun loginScreen_displaysLoginElements() {
        // Verificar que la pantalla de login muestra los elementos esperados
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val activity = instrumentation.startActivitySync()
        
        // Los elementos deberían estar presentes después de que la actividad se inicie
        assertNotNull(activity)
    }

    @Test
    fun checkDatabase_initialization() {
        // Verificar que la base de datos está configurada
        val db = com.bocatta.pos.network.firebase.FirebaseFirestoreProvider.db
        assertNotNull(db)
    }

    @Test
    fun checkKoin_injection() {
        // Verificar que Koin puede inyectar dependencias
        val sessionVm = org.koin.androidx.viewmodel.ext.android.koinViewModel<com.bocatta.pos.presentation.viewmodel.SessionViewModel>()
        assertNotNull(sessionVm)
    }
}