package com.bocatta.pos.presentation.viewmodel

import com.bocatta.pos.core.ui.viewmodel.BaseViewModel
import kotlinx.coroutines.Dispatchers
import com.bocatta.pos.core.ui.viewmodel.BaseViewModel
import kotlinx.coroutines.test.StandardTestDispatcher
import com.bocatta.pos.core.ui.viewmodel.BaseViewModel
import kotlinx.coroutines.test.resetMain
import com.bocatta.pos.core.ui.viewmodel.BaseViewModel
import kotlinx.coroutines.test.runTest
import com.bocatta.pos.core.ui.viewmodel.BaseViewModel
import kotlinx.coroutines.test.setMain
import com.bocatta.pos.core.ui.viewmodel.BaseViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import com.bocatta.pos.core.ui.viewmodel.BaseViewModel
import org.junit.After
import com.bocatta.pos.core.ui.viewmodel.BaseViewModel
import org.junit.Before
import com.bocatta.pos.core.ui.viewmodel.BaseViewModel
import org.junit.Test
import com.bocatta.pos.core.ui.viewmodel.BaseViewModel
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)

class BaseViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class TestViewModel : BaseViewModel()

    @Test
    fun estadoInicial_cargandoEsFalse() {
        val vm = TestViewModel()
        assertFalse(vm.cargando)
    }

    @Test
    fun estadoInicial_mensajeExitoEsNull() {
        val vm = TestViewModel()
        assertNull(vm.mensajeExito)
    }

    @Test
    fun estadoInicial_mensajeErrorEsNull() {
        val vm = TestViewModel()
        assertNull(vm.mensajeError)
    }

    @Test
    fun cargando_cambiaATrue_y_VuelveAFalse() {
        val vm = TestViewModel()
        vm.cargando = true
        assertTrue(vm.cargando)
        vm.cargando = false
        assertFalse(vm.cargando)
    }

    @Test
    fun mensajeExito_seAsignaCorrectamente() {
        val vm = TestViewModel()
        vm.mensajeExito = "Operación exitosa ✓"
        assertEquals("Operación exitosa ✓", vm.mensajeExito)
    }

    @Test
    fun mensajeError_seAsignaCorrectamente() {
        val vm = TestViewModel()
        vm.mensajeError = "Error: algo falló"
        assertEquals("Error: algo falló", vm.mensajeError)
    }

    @Test
    fun limpiarMensajes_limpiaAmbos() {
        val vm = TestViewModel()
        vm.mensajeExito = "Éxito"
        vm.mensajeError = "Error"
        vm.limpiarMensajes()
        assertNull(vm.mensajeExito)
        assertNull(vm.mensajeError)
    }

    @Test
    fun limpiarMensajes_noAfectaCargando() {
        val vm = TestViewModel()
        vm.cargando = true
        vm.mensajeExito = "Éxito"
        vm.limpiarMensajes()
        assertTrue(vm.cargando)
    }
}

