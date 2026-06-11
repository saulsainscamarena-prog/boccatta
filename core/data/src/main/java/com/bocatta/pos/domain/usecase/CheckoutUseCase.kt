package com.bocatta.pos.domain.usecase

import android.content.Context
import com.bocatta.pos.data.local.OfflineDatabase
import com.bocatta.pos.data.sync.OfflineManager
import com.bocatta.pos.data.repository.InventoryDeductions
import com.bocatta.pos.domain.model.*
import com.bocatta.pos.domain.repository.SalesRepository
import com.bocatta.pos.domain.repository.IInventoryRepository
import com.bocatta.pos.domain.repository.ResultadoVenta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Locale

/**
 * Caso de uso para gestionar la finalización de ventas (checkout) de Bocatta POS,
 * orquestando validaciones de stock local y guardado online/offline.
 */
class CheckoutUseCase(
    private val context: Context,
    private val repository: SalesRepository,
    private val inventoryRepo: IInventoryRepository,
    private val offlineDb: OfflineDatabase,
    private val generarTicketWhatsAppUseCase: GenerarTicketWhatsAppUseCase
) {

    /**
     * Valida si hay stock suficiente en la sucursal local para todos los insumos implicados en el carrito.
     */
    suspend fun validarStockCarrito(carrito: List<ItemCarritoV2>, sucursal: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val sucursalId = sucursal.trim().lowercase(Locale.ROOT).replace(" ", "_")
        val consolidado = linkedMapOf<String, Double>()

        for (item in carrito) {
            val recetaId = item.producto.recetaId
            val receta = if (!recetaId.isNullOrEmpty()) {
                offlineDb.obtenerRecetaPorId(recetaId)
            } else {
                null
            }

            val deds = InventoryDeductions.calcularParaItem(item, receta?.ingredientes ?: emptyList())
            if (deds.isNotEmpty()) {
                deds.forEach { (insumoId, cantidad) ->
                    consolidado[insumoId] = (consolidado[insumoId] ?: 0.0) + cantidad
                }
                if ((item.producto.id.contains("crepa", ignoreCase = true) ||
                    item.producto.categoria.uppercase(Locale.ROOT).contains("COMBO")) &&
                    !deds.containsKey("masa_crepa")) {
                    consolidado["masa_crepa"] = (consolidado["masa_crepa"] ?: 0.0) + item.cantidad.toDouble()
                }
            } else {
                val insumoId = if (item.producto.id.contains("crepa", ignoreCase = true) ||
                    item.producto.categoria.uppercase(Locale.ROOT).contains("COMBO")) {
                    "masa_crepa"
                } else {
                    item.producto.id
                }
                val insumoLocal = offlineDb.obtenerInsumos().find { it.id == insumoId }
                var factor = 1.0
                if (insumoLocal != null) {
                    val pres = insumoLocal.presentaciones.find {
                        it.unidadEquivalente.lowercase(Locale.ROOT).startsWith("pz")
                    }
                    if (pres != null) {
                        factor = pres.factorConversionABase
                    }
                }
                val baseQty = item.cantidad.toDouble() * factor
                consolidado[insumoId] = (consolidado[insumoId] ?: 0.0) + baseQty
            }
        }

        Timber.tag("SALE_FLOW").i("stock_validation_start items=${consolidado.size} sucursal=$sucursalId")

        for ((insumoId, cantidadRequerida) in consolidado) {
            Timber.tag("SALE_FLOW").i("stock_check_start insumo=$insumoId qty=$cantidadRequerida")
            val suficiente = inventoryRepo.hasSufficientStock(
                branchId = sucursalId,
                productId = insumoId,
                requiredQty = cantidadRequerida,
                unit = "pza"
            )
            Timber.tag("SALE_FLOW").i("stock_check_result insumo=$insumoId suficiente=$suficiente")

            if (!suficiente) {
                val nombreInsumo = if (insumoId == "masa_crepa") {
                    "MASA DE CREPA"
                } else {
                    offlineDb.obtenerInsumos().find { it.id == insumoId }?.nombre
                        ?: offlineDb.obtenerProductoPorId(insumoId)?.nombre
                        ?: insumoId
                }

                val msg = if (insumoId == "masa_crepa") {
                    "Sin stock de MASA DE CREPA en $sucursalId. Registra una tanda en Inventario > Produccion."
                } else {
                    "Stock insuficiente de $nombreInsumo en $sucursalId. Requerido: $cantidadRequerida."
                }
                Timber.tag("INVENTORY").w(msg)
                return@withContext Pair(false, msg)
            }
        }
        Timber.tag("SALE_FLOW").i("stock_validation_ok sucursal=$sucursalId")
        Pair(true, "")
    }

    /**
     * Finaliza la venta procesando la deducción de inventario, guardando local u online según conectividad
     * y retornando el resultado del checkout y texto para el ticket.
     */
    suspend fun finalizarVenta(
        carrito: List<ItemCarritoV2>,
        sucursal: String,
        usuarioNombre: String,
        clienteSeleccionado: ClienteV2?,
        descuentoLealtad: Double,
        descuentoPromociones: Double,
        descuentoManual: Double,
        propina: Double,
        notaOrden: String,
        esConsumoEmpleado: Boolean,
        splitActivo: Boolean,
        splitPartes: List<com.bocatta.pos.domain.model.SplitParte>,
        metodoPagoSeleccionado: String,
        forcedVentaId: String?
    ): CheckoutResult {
        val online = withContext(Dispatchers.IO) {
            OfflineManager.isNetworkAvailable(context)
        }

        // 1. Validar Stock Pre-flight
        val (stockValido, errorMsg) = validarStockCarrito(carrito, sucursal)
        if (!stockValido) {
            return CheckoutResult(success = false, online = online, ticketText = null, error = errorMsg)
        }

        val propinaSnapshot = if (esConsumoEmpleado) 0.0 else propina.coerceAtLeast(0.0)
        val totalSinPropinaSnapshot = (carrito.sumOf { it.precioFinal.toDouble() * it.cantidad } - descuentoLealtad - descuentoPromociones - descuentoManual).coerceAtLeast(0.0)
        val totalVenta = if (esConsumoEmpleado) 0.0 else totalSinPropinaSnapshot + propinaSnapshot

        val metodoPago = if (esConsumoEmpleado) {
            "Cortesia"
        } else if (splitActivo) {
            "Dividido: [" + splitPartes.joinToString(", ") { "${it.metodoPago.valor} $${"%.2f".format(it.monto)}" } + "]"
        } else {
            metodoPagoSeleccionado
        }

        return try {
            val resultado = if (!online) {
                Timber.tag("SALE_FLOW").i("offline_save_start")
                withContext(Dispatchers.IO) {
                    OfflineManager.guardarVentaOffline(
                        context = context,
                        carrito = carrito,
                        sucursal = sucursal,
                        usuarioNombre = usuarioNombre,
                        total = totalVenta,
                        descuentoLealtad = descuentoLealtad,
                        descuentoPromociones = descuentoPromociones,
                        descuentoManual = descuentoManual,
                        clienteSeleccionado = clienteSeleccionado,
                        metodoPago = metodoPago,
                        esConsumoEmpleado = esConsumoEmpleado,
                        propina = propinaSnapshot,
                        notaOrden = notaOrden,
                        forcedVentaId = forcedVentaId
                    )
                }
            } else {
                Timber.tag("SALE_FLOW").i("online_save_start")
                repository.finalizarVentaConInventario(
                    carrito = carrito,
                    sucursal = sucursal,
                    usuarioNombre = usuarioNombre,
                    clienteSeleccionado = clienteSeleccionado,
                    descuentoLealtad = descuentoLealtad,
                    metodoPagoSeleccionado = metodoPago,
                    esConsumoEmpleado = esConsumoEmpleado,
                    descuentoPromociones = descuentoPromociones,
                    descuentoManual = descuentoManual,
                    propina = propinaSnapshot,
                    notaOrden = notaOrden,
                    splitPartes = splitPartes,
                    forcedVentaId = forcedVentaId
                )
            }

            val ticketText = generarTicketWhatsAppUseCase(
                sucursal = sucursal,
                items = carrito,
                codigoTicket = resultado.codigoTicket,
                total = totalVenta,
                descuentoLealtad = descuentoLealtad,
                descuentoPromociones = descuentoPromociones,
                descuentoManual = descuentoManual,
                metodoPago = metodoPago,
                propina = propinaSnapshot,
                notaOrden = notaOrden,
                modoOperacion = if (online) "" else "Offline local"
            )

            CheckoutResult(
                success = true,
                online = online,
                ticketText = ticketText,
                numeroTicket = resultado.numeroTicket,
                codigoTicket = resultado.codigoTicket
            )
        } catch (e: Exception) {
            Timber.tag("SALE_FLOW").e(e, "Error finalizando venta")
            CheckoutResult(
                success = false,
                online = online,
                ticketText = null,
                error = e.message ?: "Error desconocido"
            )
        }
    }
}

/**
 * Resultado estructurado del proceso de checkout.
 */
data class CheckoutResult(
    val success: Boolean,
    val online: Boolean,
    val ticketText: String?,
    val error: String? = null,
    val numeroTicket: Long? = null,
    val codigoTicket: String? = null
)
