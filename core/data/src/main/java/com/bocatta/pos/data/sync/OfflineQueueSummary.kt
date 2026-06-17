package com.bocatta.pos.data.sync

/**
 * Resumen de la cola offline para diagnósticos y UI.
 *
 * @property pendingSales ventas offline pendientes de sincronizar
 * @property failedSales ventas offline que fallaron tras reintentos
 * @property pendingOperations operaciones offline (devoluciones/cancelaciones) pendientes
 * @property pendingStockAdjustments ajustes de stock pendientes
 * @property pendingContingencyShifts turnos de contingencia pendientes de sincronizar
 */
data class OfflineQueueSummary(
    val pendingSales: Int,
    val failedSales: Int,
    val pendingOperations: Int,
    val pendingStockAdjustments: Int,
    val pendingContingencyShifts: Int
) {
    /** Total de elementos pendientes (excluye fallidos). */
    val totalPending: Int
        get() = pendingSales + pendingOperations + pendingStockAdjustments + pendingContingencyShifts

    /** Total de elementos pendientes + fallidos. */
    val totalOutstanding: Int
        get() = totalPending + failedSales
}
