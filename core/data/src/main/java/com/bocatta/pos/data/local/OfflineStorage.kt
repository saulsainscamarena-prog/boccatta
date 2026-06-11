package com.bocatta.pos.data.local

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
