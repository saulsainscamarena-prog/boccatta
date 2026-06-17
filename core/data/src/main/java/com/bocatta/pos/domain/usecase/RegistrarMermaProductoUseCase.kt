package com.bocatta.pos.domain.usecase

import android.content.Context
import com.bocatta.pos.data.local.OfflineDatabase
import com.bocatta.pos.domain.model.OperacionOffline
import com.bocatta.pos.data.repository.InventoryRepository
import com.bocatta.pos.data.sync.SyncScheduler
import com.bocatta.pos.domain.model.ItemCarritoV2
import com.bocatta.pos.domain.model.SalesInventoryProductV2
import com.bocatta.pos.domain.model.StockAdjustmentEntity
import com.bocatta.pos.domain.repository.IStockAdjustmentQueue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.math.BigDecimal
import java.util.Locale
import java.util.UUID

data class MermaProductoResult(
    val operacionId: String,
    val deducciones: Map<String, Double>
)

class RegistrarMermaProductoUseCase(
    private val appContext: Context,
    private val offlineDb: OfflineDatabase,
    private val adjustmentQueue: IStockAdjustmentQueue
) {
    suspend operator fun invoke(
        producto: SalesInventoryProductV2,
        cantidad: Int,
        motivo: String,
        sucursal: String,
        usuarioNombre: String
    ): Result<MermaProductoResult> = withContext(Dispatchers.IO) {
        runCatching {
            require(cantidad > 0) { "Cantidad de merma invalida" }
            require(motivo.isNotBlank()) { "Motivo de merma requerido" }

            val operacionId = "merma_${UUID.randomUUID()}"
            val sucursalId = sucursal.lowercase(Locale.ROOT)
            val itemSimulado = ItemCarritoV2(
                cartId = operacionId,
                producto = producto,
                precioFinal = BigDecimal.ZERO,
                cantidad = cantidad,
                nota = "MERMA: ${motivo.trim()}",
                nombre = producto.nombre
            )
            val deducciones = InventoryRepository(offlineDb).calcularDeduccionesItemOffline(itemSimulado)
            val ahora = System.currentTimeMillis()

            deducciones.forEach { (insumoId, cantidadDeducida) ->
                val actual = offlineDb.obtenerStockInsumo(insumoId)
                if (actual > 0.0) {
                    offlineDb.actualizarStockInsumo(insumoId, (actual - cantidadDeducida).coerceAtLeast(0.0))
                }
                adjustmentQueue.enqueue(
                    StockAdjustmentEntity(
                        branchId = sucursalId,
                        productId = insumoId,
                        quantity = -cantidadDeducida,
                        unit = "base",
                        reason = "WASTE",
                        timestamp = ahora
                    )
                )
            }

            offlineDb.guardarOperacion(
                OperacionOffline(
                    id = operacionId,
                    tipo = "merma",
                    ventaId = null,
                    motivo = motivo.trim(),
                    usuarioId = usuarioNombre,
                    sucursal = sucursalId,
                    fecha = ahora,
                    requiereAprobacion = false,
                    dataJson = buildMermaJson(
                        operacionId = operacionId,
                        producto = producto,
                        cantidad = cantidad,
                        motivo = motivo.trim(),
                        sucursal = sucursalId,
                        usuarioNombre = usuarioNombre,
                        fecha = ahora,
                        deducciones = deducciones
                    ),
                    estado = OperacionOffline.ESTADO_PENDIENTE,
                    intentos = 0
                )
            )

            SyncScheduler.scheduleImmediateSync(appContext)
            MermaProductoResult(operacionId = operacionId, deducciones = deducciones)
        }
    }

    private fun buildMermaJson(
        operacionId: String,
        producto: SalesInventoryProductV2,
        cantidad: Int,
        motivo: String,
        sucursal: String,
        usuarioNombre: String,
        fecha: Long,
        deducciones: Map<String, Double>
    ): String {
        val deduccionesJson = JSONObject()
        deducciones.forEach { (insumoId, cantidadDeducida) ->
            deduccionesJson.put(insumoId, cantidadDeducida)
        }
        return JSONObject()
            .put("id", operacionId)
            .put("productoId", producto.id)
            .put("productoNombre", producto.nombre)
            .put("insumoId", producto.id)
            .put("cantidad", cantidad.toDouble())
            .put("motivo", motivo)
            .put("sucursal", sucursal)
            .put("fecha", fecha)
            .put("usuario", usuarioNombre)
            .put("deducciones", deduccionesJson)
            .toString()
    }
}
