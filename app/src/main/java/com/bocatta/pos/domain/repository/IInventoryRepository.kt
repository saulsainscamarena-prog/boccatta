package com.bocatta.pos.domain.repository

import com.bocatta.pos.domain.model.InventoryItem
import kotlinx.coroutines.flow.Flow
import com.bocatta.pos.domain.model.StockAdjustmentEntity

/**
 * Contrato para la gestión de inventario.
 * Centraliza las operaciones de stock para garantizar trazabilidad y consistencia.
 * Todas las deducciones y ajustes deben pasar por esta interfaz para registrar movimientos.
 */
interface IInventoryRepository {

    /**
     * Obtiene el flujo de inventario completo para una sucursal.
     */
    fun getStockForBranch(branchId: String): Flow<List<InventoryItem>>

    /**
     * Obtiene un ítem específico de inventario por sucursal y producto.
     */
    fun getStockItem(branchId: String, productId: String): Flow<InventoryItem?>

    /**
     * Consulta el stock actual disponible para un producto en una sucursal.
     * @return Cantidad actual en la unidad base del producto.
     */
    suspend fun getCurrentStock(branchId: String, productId: String): Double

    /**
     * Ajusta el stock de forma atómica y registra el movimiento en la traza de auditoría.
     * @param branchId ID de la sucursal.
     * @param productId ID del producto/insumo.
     * @param quantity Cantidad a ajustar (positivo para entrada, negativo para salida).
     * @param unit Unidad en la que se expresa la cantidad (debe coincidir con baseUnit del producto).
     * @param reason Motivo del movimiento: "SALE", "PRODUCTION", "PURCHASE", "WASTE", "ADJUSTMENT".
     * @param referenceId ID de la transacción o proceso que originó el movimiento.
     * @param userId ID del usuario que realizó la operación.
     * @return true si el ajuste fue exitoso.
     */
    suspend fun adjustStock(
        branchId: String,
        productId: String,
        quantity: Double,
        unit: String,
        reason: String,
        referenceId: String,
        userId: String
    ): Boolean

    /**
     * Valida si hay stock suficiente para una operación sin realizar el descuento.
     * Útil para pre-validar carritos de compra antes de procesar la venta.
     * @param branchId ID de la sucursal.
     * @param productId ID del producto.
     * @param requiredQty Cantidad requerida.
     * @param unit Unidad de la cantidad requerida.
     * @return true si el stock disponible es mayor o igual a la cantidad requerida.
     */
    suspend fun hasSufficientStock(
        branchId: String,
        productId: String,
        requiredQty: Double,
        unit: String
    ): Boolean

    /**
     * Ejecuta múltiples ajustes de stock en UNA sola transacción atómica de Firestore.
     * @param branchId ID de la sucursal.
     * @param userId ID del usuario.
     * @param referenceId ID de referencia (transacción/proceso).
     * @param deductions Lista de deducciones a aplicar.
     * @return true si TODOS los ajustes se completaron exitosamente.
     */
    suspend fun adjustStockBatch(
        branchId: String,
        userId: String,
        referenceId: String,
        deductions: List<StockDeduction>
    ): Boolean

    /**
     * Obtiene el historial de movimientos para un producto en un rango de fechas.
     * Útil para reportes de auditoría y análisis de mermas.
     */
    fun getMovementHistory(
        branchId: String,
        productId: String,
        startDate: Long,
        endDate: Long
    ): Flow<List<InventoryMovement>>

    /**
     * Aplica una única entrada de la cola offline y la registra como movimiento.
     * Las implementaciones pueden usar {@code adjustStock} con valores de placeholder
     * para los campos que no están presentes en la tabla SQLite.
     */
    suspend fun syncOfflineAdjustment(adjustment: StockAdjustmentEntity): Boolean
}

data class StockDeduction(
    val productId: String,
    val quantity: Double,
    val unit: String,
    val reason: String = "SALE_DEDUCTION"
)

/**
 * Modelo simple para representar un movimiento de inventario en la traza de auditoría.
 * Este modelo es de solo lectura y se usa para reportes.
 */
data class InventoryMovement(
    val id: String,
    val branchId: String,
    val productId: String,
    val type: String, // SALE_DEDUCTION, PRODUCTION_INPUT, etc.
    val quantity: Double,
    val unit: String,
    val userId: String,
    val referenceId: String,
    val timestamp: Long,
    val metadata: Map<String, Any>? = null
)