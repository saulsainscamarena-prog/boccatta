package com.bocatta.pos.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.bocatta.pos.core.TicketUtils
import com.bocatta.pos.data.local.OfflineDatabase
import com.bocatta.pos.data.local.OperacionOffline
import com.bocatta.pos.data.local.VentaOffline
import com.bocatta.pos.domain.model.ClienteV2
import com.bocatta.pos.domain.model.ItemCarritoV2
import com.bocatta.pos.domain.repository.ResultadoVenta
import com.bocatta.pos.data.repository.InventoryRepository
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object OfflineManager {

    private val json = Json { ignoreUnknownKeys = true }

    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    @Deprecated("Use TicketUtils.generarCodigoTicket instead", ReplaceWith("TicketUtils.generarCodigoTicket(sucursal, numero)"))
    fun generarCodigoTicket(sucursal: String, numero: Long): String = TicketUtils.generarCodigoTicket(sucursal, numero)

    fun guardarVentaOffline(
        context: Context,
        carrito: List<ItemCarritoV2>,
        sucursal: String,
        usuarioNombre: String,
        total: Double,
        descuentoLealtad: Double,
        clienteSeleccionado: ClienteV2?,
        metodoPago: String,
        esConsumoEmpleado: Boolean
    ): ResultadoVenta {
        val ventaId = "offline_${System.currentTimeMillis()}"
        val sucursalId = sucursal.lowercase()
        val db = OfflineDatabase.getInstance(context)
        val inventoryRepo = InventoryRepository(db)

        // Calcular primero; venta y stock se confirman juntos en SQLite.
        val deducciones = linkedMapOf<String, Double>()
        carrito.forEach { item ->
            inventoryRepo.calcularDeduccionesItemOffline(item).forEach { (insumoId, cantidad) ->
                deducciones[insumoId] = (deducciones[insumoId] ?: 0.0) + cantidad
            }
        }

        val carritoJson = json.encodeToString(carrito.map { item ->
            mapOf(
                "nombre" to item.nombre,
                "cantidad" to item.cantidad.toDouble(),
                "precio" to item.precioFinal.toDouble(),
                "productoId" to item.producto.id,
                "recetaId" to (item.producto.recetaId ?: ""),
                "categoria" to item.producto.categoria,
                "esSeparado" to item.esSeparado,
                "base" to item.base,
                "aderezos" to item.aderezos,
                "toppings" to item.toppings,
                "paraLlevar" to item.paraLlevar,
                "cantidadGramos" to item.cantidadGramos,
                "componentesCombo" to item.componentesCombo.map { componente ->
                    mapOf(
                        "nombre" to componente.nombre,
                        "cantidad" to componente.cantidad.toDouble(),
                        "base" to componente.base,
                        "aderezos" to componente.aderezos,
                        "toppings" to componente.toppings,
                        "paraLlevar" to componente.paraLlevar
                    )
                }
            )
        })

        val ventaPendiente = VentaOffline(
            id = ventaId,
            ticket = 0L,
            codigoTicket = "",
            total = total,
            descuentoLealtad = descuentoLealtad,
            fecha = System.currentTimeMillis(),
            sucursal = sucursalId,
            atendio = usuarioNombre,
            metodoPago = metodoPago,
            esConsumoEmpleado = esConsumoEmpleado,
            clienteId = clienteSeleccionado?.telefono,
            carritoJson = carritoJson,
            estado = VentaOffline.ESTADO_PENDIENTE,
            intentos = 0,
            ultimoIntento = null
        )

        val legacyUltimoTicket = leerUltimoTicketLocalLegacy(context, sucursalId)
        val ventaConfirmada = db.guardarVentaYDescontarStockReservandoFolio(
            ventaBase = ventaPendiente,
            deducciones = deducciones,
            legacyUltimoTicket = legacyUltimoTicket
        )

        return ResultadoVenta(
            numeroTicket = ventaConfirmada.ticket,
            codigoTicket = ventaConfirmada.codigoTicket
        )
    }

    fun guardarOperacionOffline(
        context: Context,
        tipo: String,
        ventaId: String?,
        motivo: String,
        usuarioId: String,
        sucursal: String,
        dataJson: String,
        requiereAprobacion: Boolean = true
    ) {
        val operacionId = "offline_op_${System.currentTimeMillis()}"

        val operacion = OperacionOffline(
            id = operacionId,
            tipo = tipo,
            ventaId = ventaId,
            motivo = motivo,
            usuarioId = usuarioId,
            sucursal = sucursal.lowercase(),
            fecha = System.currentTimeMillis(),
            requiereAprobacion = requiereAprobacion,
            dataJson = dataJson,
            estado = OperacionOffline.ESTADO_PENDIENTE,
            intentos = 0
        )

        OfflineDatabase.getInstance(context).guardarOperacion(operacion)
    }

    @Deprecated("Legacy SharedPreferences counter is read only as a migration floor. Use SQLite folios in OfflineDatabase.")
    fun obtenerUltimoTicketLocal(context: Context, sucursalId: String): Long = leerUltimoTicketLocalLegacy(context, sucursalId)

    private fun leerUltimoTicketLocalLegacy(context: Context, sucursalId: String): Long {
        val prefs = context.getSharedPreferences("bocatta_offline_tickets", Context.MODE_PRIVATE)
        return prefs.getLong("last_ticket_$sucursalId", 0L)
    }

    @Deprecated("Legacy SharedPreferences counter is no longer updated by the checkout flow.")
    fun guardarUltimoTicketLocal(context: Context, sucursalId: String, ticket: Long) {
        val prefs = context.getSharedPreferences("bocatta_offline_tickets", Context.MODE_PRIVATE)
        prefs.edit().putLong("last_ticket_$sucursalId", ticket).apply()
    }
}


