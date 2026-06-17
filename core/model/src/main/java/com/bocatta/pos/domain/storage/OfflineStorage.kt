package com.bocatta.pos.domain.storage

import com.bocatta.pos.domain.model.OperacionOffline
import com.bocatta.pos.domain.model.VentaOffline

/**
 * Interface for offline storage operations used by OfflineManager.
 * Allows testing OfflineManager without static mocking of OfflineDatabase.
 */
interface OfflineStorage {
    fun guardarVentaYDescontarStockReservandoFolio(
        ventaBase: VentaOffline,
        deducciones: Map<String, Double>,
        legacyUltimoTicket: Long = 0L
    ): VentaOffline

    fun guardarOperacion(op: OperacionOffline)
}
