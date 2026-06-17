package com.bocatta.pos.domain.usecase

import android.content.Context
import com.bocatta.pos.data.sync.OfflineManager
import com.bocatta.pos.domain.model.ItemCarritoV2
import java.util.Locale
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import timber.log.Timber

enum class CancellationScope {
    ITEM,
    CART
}

data class CancellationRequest(
    val items: List<ItemCarritoV2>,
    val scope: CancellationScope,
    val reason: String,
    val userId: String,
    val branch: String
)

sealed interface CancellationResult {
    data class Saved(val operationId: String) : CancellationResult
    data class Failed(val operatorMessage: String) : CancellationResult
}

class RegistrarCancelacionUseCase(
    private val context: Context,
    private val operationStore: (CancellationRequest, String, String) -> Unit =
        { request, operationId, dataJson ->
            OfflineManager.guardarOperacionOffline(
                context = context,
                tipo = "cancelacion",
                ventaId = null,
                motivo = request.reason,
                usuarioId = request.userId,
                sucursal = request.branch,
                dataJson = dataJson,
                requiereAprobacion = true,
                forcedOperationId = operationId
            )
        }
) {

    operator fun invoke(request: CancellationRequest): CancellationResult {
        if (request.items.isEmpty()) {
            return CancellationResult.Failed("No hay productos para cancelar.")
        }
        if (request.reason.isBlank()) {
            return CancellationResult.Failed("Captura el motivo de cancelacion.")
        }
        if (request.userId.isBlank() || request.branch.isBlank()) {
            return CancellationResult.Failed("No se pudo identificar al usuario o sucursal.")
        }

        val operationId = "cancel_${System.currentTimeMillis()}_${request.scope.name.lowercase(Locale.ROOT)}"
        val payload = buildPayload(request, operationId)

        return try {
            operationStore(request, operationId, payload)
            Timber.tag("CART").i(
                "Cancellation saved locally id=$operationId scope=${request.scope} items=${request.items.size}"
            )
            CancellationResult.Saved(operationId)
        } catch (error: Exception) {
            Timber.tag("CART").e(
                error,
                "Cancellation persistence failed scope=${request.scope} items=${request.items.size}"
            )
            CancellationResult.Failed(
                "No se pudo guardar la cancelacion. La orden se conserva para reintentar."
            )
        }
    }

    private fun buildPayload(request: CancellationRequest, operationId: String): String {
        val itemsJson = buildJsonArray {
            request.items.forEach { item ->
                add(
                    buildJsonObject {
                        put("cartId", item.cartId)
                        put("productoId", item.producto.id)
                        put("productoNombre", item.nombre)
                        put("cantidad", item.cantidad)
                        put("precioUnitario", item.precioFinal.toDouble())
                        put("totalLinea", item.precioFinal.toDouble() * item.cantidad)
                        put("nota", item.nota)
                        put("paraLlevar", item.paraLlevar)
                    }
                )
            }
        }

        return buildJsonObject {
            put("id", operationId)
            put("alcance", request.scope.name.lowercase(Locale.ROOT))
            put("estado", "pendiente_revision")
            put("usuario", request.userId)
            put("sucursal", request.branch.lowercase(Locale.ROOT))
            put("motivo", request.reason.trim())
            put("fecha", System.currentTimeMillis())
            put("total", request.items.sumOf { it.precioFinal.toDouble() * it.cantidad })
            put("items", itemsJson)
        }.toString()
    }
}
