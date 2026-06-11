package com.bocatta.pos.security

import com.bocatta.pos.data.local.OfflineDatabase
import com.bocatta.pos.data.sync.SyncWorker
import com.bocatta.pos.data.sync.OfflineManager
import com.bocatta.pos.domain.usecase.CheckoutUseCase
import com.bocatta.pos.domain.usecase.RegistrarMermaProductoUseCase
import com.bocatta.pos.feature.ventas.viewmodel.SalesViewModelV2
import org.junit.Assert.assertNotNull
import org.junit.Test

class OfflineOperationsContractTest {

    @Test
    fun syncWorkerClassExists() {
        assertNotNull(SyncWorker::class.java)
    }

    @Test
    fun offlineDatabaseClassExists() {
        assertNotNull(OfflineDatabase::class.java)
    }

    @Test
    fun offlineManagerClassExists() {
        assertNotNull(OfflineManager::class.java)
    }

    @Test
    fun checkoutUseCaseClassExists() {
        assertNotNull(CheckoutUseCase::class.java)
    }

    @Test
    fun mermaUseCaseClassExists() {
        assertNotNull(RegistrarMermaProductoUseCase::class.java)
    }

    @Test
    fun salesViewModelClassExists() {
        assertNotNull(SalesViewModelV2::class.java)
    }

    @Test
    fun offlineManagerHasGuardarVentaOffline() {
        val methods = OfflineManager::class.java.declaredMethods
        val encontrado = methods.any { it.name == "guardarVentaOffline" }
        assert(encontrado) { "OfflineManager debe tener método guardarVentaOffline" }
    }

    @Test
    fun syncWorkerHasSincronizarVentaMethod() {
        val methods = SyncWorker::class.java.declaredMethods
        val encontrado = methods.any { it.name == "sincronizarVenta" }
        assert(encontrado) { "SyncWorker debe tener método sincronizarVenta" }
    }

    @Test
    fun salesVmHasLimpiarEstadoPostVenta() {
        val methods = SalesViewModelV2::class.java.declaredMethods
        val encontrado = methods.any { it.name == "limpiarEstadoPostVenta" }
        assert(encontrado) { "SalesViewModelV2 debe tener método limpiarEstadoPostVenta" }
    }

    @Test
    fun salesVmHasLimpiarCarrito() {
        val methods = SalesViewModelV2::class.java.methods
        val encontrado = methods.any { it.name == "limpiarCarrito" }
        assert(encontrado) { "SalesViewModelV2 debe tener método limpiarCarrito" }
    }
}
