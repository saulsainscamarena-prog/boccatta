package com.bocatta.pos.security

import com.bocatta.pos.data.local.OfflineDatabase
import com.bocatta.pos.data.sync.SyncWorker
import com.bocatta.pos.data.sync.OfflineManager
import com.bocatta.pos.domain.usecase.CheckoutUseCase
import com.bocatta.pos.domain.usecase.RegistrarMermaProductoUseCase
import com.bocatta.pos.presentation.viewmodel.SalesViewModelV2
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
        val method = OfflineManager::class.java.getDeclaredMethod(
            "guardarVentaOffline",
            android.content.Context::class.java,
            List::class.java,
            String::class.java,
            String::class.java,
            Double::class.javaPrimitiveType,
            Double::class.javaPrimitiveType,
            Double::class.javaPrimitiveType,
            Double::class.javaPrimitiveType,
            com.bocatta.pos.domain.model.ClienteV2::class.java,
            String::class.java,
            Boolean::class.javaPrimitiveType,
            Double::class.javaPrimitiveType,
            String::class.java,
            String::class.java
        )
        assertNotNull(method)
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
