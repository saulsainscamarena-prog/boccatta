package com.bocatta.pos.domain.repository

import com.bocatta.pos.domain.model.ItemCarritoV2

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
        descuentoManual: Double = 0.0
    ): ResultadoVenta

    suspend fun registrarGastoValidado(
        monto: Double,
        motivo: String,
        sucursal: String,
        usuarioId: String
    ): Boolean
}


