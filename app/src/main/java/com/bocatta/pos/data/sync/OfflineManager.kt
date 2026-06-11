package com.bocatta.pos.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.bocatta.pos.core.TicketUtils
import com.bocatta.pos.data.local.OfflineDatabase
import com.bocatta.pos.data.local.OfflineStorage
import com.bocatta.pos.data.local.OperacionOffline
import com.bocatta.pos.data.local.VentaOffline
import com.bocatta.pos.domain.model.ClienteV2
import com.bocatta.pos.domain.model.ItemCarritoV2
import com.bocatta.pos.domain.repository.ResultadoVenta
import com.bocatta.pos.data.repository.InventoryRepository
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

object OfflineManager {

    /** For testing — set to a mock/fake [OfflineStorage]. */
    @kotlin.jvm.JvmStatic
    var testStorage: OfflineStorage? = null

    /** For testing — override JSON instance. */
    @kotlin.jvm.JvmStatic
    var testJson: Json? = null

    private val json: Json
        get() = testJson ?: Json { ignoreUnknownKeys = true }

    private fun storage(context: Context): OfflineStorage =
        testStorage ?: OfflineDatabase.getInstance(context)

    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }


    @androidx.room.Transaction
    suspend fun guardarVentaOffline(
        context: Context,
        carrito: List<ItemCarritoV2>,
        sucursal: String,
        usuarioNombre: String,
        total: Double,
        descuentoLealtad: Double,
        descuentoPromociones: Double = 0.0,
        descuentoManual: Double = 0.0,
        clienteSeleccionado: ClienteV2?,
        metodoPago: String,
        esConsumoEmpleado: Boolean,
        propina: Double = 0.0,
        notaOrden: String = "",
        forcedVentaId: String? = null
    ): ResultadoVenta {
        val ventaId = forcedVentaId ?: "offline_${System.currentTimeMillis()}"
        val sucursalId = sucursal.lowercase(java.util.Locale.getDefault())
        val db = OfflineDatabase.getInstance(context)
        val store = storage(context)
        val inventoryRepo = InventoryRepository(db)

        // Calcular primero; venta y stock se confirman juntos en SQLite.
        val deducciones = linkedMapOf<String, Double>()
        carrito.forEach { item ->
            inventoryRepo.calcularDeduccionesItemOffline(item).forEach { (insumoId, cantidad) ->
                deducciones[insumoId] = (deducciones[insumoId] ?: 0.0) + cantidad
            }
        }

        val carritoJson = json.encodeToString(carrito.map { item ->
            JsonObject(
                buildMap {
                    put("nombre", JsonPrimitive(item.nombre))
                    put("cantidad", JsonPrimitive(item.cantidad.toDouble()))
                    put("precio", JsonPrimitive(item.precioFinal.toDouble()))
                    put("productoId", JsonPrimitive(item.producto.id))
                    put("recetaId", JsonPrimitive(item.producto.recetaId ?: ""))
                    put("categoria", JsonPrimitive(item.producto.categoria))
                    put("esSeparado", JsonPrimitive(item.esSeparado))
                    put("base", item.base?.let { JsonPrimitive(it) } ?: JsonNull)
                    put("aderezos", JsonArray(item.aderezos.map { JsonPrimitive(it) }))
                    put("toppings", JsonArray(item.toppings.map { JsonPrimitive(it) }))
                    put("paraLlevar", JsonPrimitive(item.paraLlevar))
                    put("cantidadGramos", item.cantidadGramos?.let { JsonPrimitive(it) } ?: JsonNull)
                    put("componentesCombo", JsonArray(
                        item.componentesCombo.map { componente ->
                            JsonObject(
                                buildMap {
                                    put("nombre", JsonPrimitive(componente.nombre))
                                    put("cantidad", JsonPrimitive(componente.cantidad.toDouble()))
                                    put("base", componente.base?.let { JsonPrimitive(it) } ?: JsonNull)
                                    put("aderezos", JsonArray(componente.aderezos.map { JsonPrimitive(it) }))
                                    put("toppings", JsonArray(componente.toppings.map { JsonPrimitive(it) }))
                                    put("paraLlevar", JsonPrimitive(componente.paraLlevar))
                                }
                            )
                        }
                    ))
                }
            )
        })

        val ventaPendiente = VentaOffline(
            id = ventaId,
            ticket = 0L,
            codigoTicket = "",
            total = total,
            descuentoLealtad = descuentoLealtad,
            descuentoPromociones = descuentoPromociones,
            descuentoManual = descuentoManual,
            propina = propina,
            notaOrden = notaOrden,
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

        val ventaConfirmada = store.guardarVentaYDescontarStockReservandoFolio(
            ventaBase = ventaPendiente,
            deducciones = deducciones
        )
        SyncScheduler.scheduleImmediateSync(context)

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
            sucursal = sucursal.lowercase(java.util.Locale.getDefault()),
            fecha = System.currentTimeMillis(),
            requiereAprobacion = requiereAprobacion,
            dataJson = dataJson,
            estado = OperacionOffline.ESTADO_PENDIENTE,
            intentos = 0
        )

        storage(context).guardarOperacion(operacion)
        SyncScheduler.scheduleImmediateSync(context)
    }


}
