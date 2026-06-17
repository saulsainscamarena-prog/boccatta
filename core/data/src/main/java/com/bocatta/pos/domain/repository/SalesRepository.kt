package com.bocatta.pos.domain.repository

import com.bocatta.pos.domain.model.ItemCarritoV2
import com.bocatta.pos.domain.model.VentaV2
import kotlinx.coroutines.flow.Flow

data class ResultadoVenta(
    val numeroTicket: Long,
    val codigoTicket: String // ATL3004-001
)

interface SalesRepository {
    suspend fun finalizarVentaConInventario(
        carrito: List<ItemCarritoV2>,
        sucursal: String,
        usuarioNombre: String,
        clienteSeleccionado: com.bocatta.pos.domain.model.ClienteV2?,
        descuentoLealtad: Double,
        metodoPagoSeleccionado: String,
        esConsumoEmpleado: Boolean,
        descuentoPromociones: Double = 0.0,
        descuentoManual: Double = 0.0,
        propina: Double = 0.0,
        notaOrden: String = "",
        splitPartes: List<com.bocatta.pos.domain.model.SplitParte> = emptyList(),
        forcedVentaId: String? = null
    ): ResultadoVenta

    suspend fun registrarGastoValidado(
        monto: Double,
        motivo: String,
        sucursal: String,
        usuarioId: String
    ): Boolean

    fun getActiveKdsOrders(sucursal: String): Flow<List<VentaV2>>

    suspend fun updateKdsOrderStatus(ventaId: String, estado: String, sucursal: String): Boolean
}
