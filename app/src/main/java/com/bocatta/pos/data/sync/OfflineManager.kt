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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
        esConsumoEmpleado: Boolean,
        ticketNumber: Long,
        codigoTicket: String
    ): ResultadoVenta {
        val ventaId = "offline_${System.currentTimeMillis()}_${ticketNumber}"
        val sucursalId = sucursal.lowercase()
        val db = OfflineDatabase.getInstance(context)
        val inventoryRepo = InventoryRepository(db)

        // Procesar deducción de inventario local INMEDIATAMENTE
        var stockSuficiente = true
        for (item in carrito) {
            val exito = inventoryRepo.descontarVentaCompleta(
                productoId = item.producto.id,
                recetaId = item.producto.recetaId,
                toppings = item.toppings,
                base = item.base,
                aderezos = item.aderezos,
                esSeparado = item.esSeparado,
                cantidad = item.cantidad
            )
            if (!exito) {
                stockSuficiente = false
                break
            }
        }

        if (!stockSuficiente) {
            throw IllegalStateException("Stock local insuficiente")
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
                "toppings" to item.toppings
            )
        })

        val ventaPendiente = VentaOffline(
            id = ventaId,
            ticket = ticketNumber,
            codigoTicket = codigoTicket,
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

        db.guardarVenta(ventaPendiente)

        return ResultadoVenta(numeroTicket = ticketNumber, codigoTicket = codigoTicket)
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

    fun obtenerUltimoTicketLocal(context: Context, sucursalId: String): Long {
        val prefs = context.getSharedPreferences("bocatta_offline_tickets", Context.MODE_PRIVATE)
        val lastTicket = prefs.getLong("last_ticket_$sucursalId", 0L)
        return lastTicket + 1
    }

    fun guardarUltimoTicketLocal(context: Context, sucursalId: String, ticket: Long) {
        val prefs = context.getSharedPreferences("bocatta_offline_tickets", Context.MODE_PRIVATE)
        prefs.edit().putLong("last_ticket_$sucursalId", ticket).apply()
    }
}

