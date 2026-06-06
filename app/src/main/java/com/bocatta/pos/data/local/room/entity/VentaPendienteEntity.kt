package com.bocatta.pos.data.local.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity para ventas pendientes de sincronización.
 * Mapeo 1:1 con la tabla `ventas_pendientes` de OfflineDatabase.
 */
@Entity(tableName = "ventas_pendientes")
data class VentaPendienteEntity(
    @PrimaryKey
    val id: String,
    val ticket: Long,
    @ColumnInfo(name = "codigoTicket")
    val codigoTicket: String,
    val total: Double,
    @ColumnInfo(name = "descuentoLealtad")
    val descuentoLealtad: Double,
    @ColumnInfo(name = "descuentoPromociones", defaultValue = "0.0")
    val descuentoPromociones: Double = 0.0,
    @ColumnInfo(name = "descuentoManual", defaultValue = "0.0")
    val descuentoManual: Double = 0.0,
    @ColumnInfo(defaultValue = "0.0")
    val propina: Double = 0.0,
    @ColumnInfo(name = "notaOrden", defaultValue = "")
    val notaOrden: String = "",
    val fecha: Long,
    val sucursal: String,
    val atendio: String,
    @ColumnInfo(name = "metodoPago")
    val metodoPago: String,
    @ColumnInfo(name = "esConsumoEmpleado", defaultValue = "0")
    val esConsumoEmpleado: Boolean = false,
    @ColumnInfo(name = "clienteId")
    val clienteId: String? = null,
    @ColumnInfo(name = "carritoJson")
    val carritoJson: String,
    @ColumnInfo(defaultValue = "pendiente")
    val estado: String = ESTADO_PENDIENTE,
    @ColumnInfo(defaultValue = "0")
    val intentos: Int = 0,
    @ColumnInfo(name = "ultimoIntento")
    val ultimoIntento: Long? = null
) {
    companion object {
        const val ESTADO_PENDIENTE = "pendiente"
        const val ESTADO_SINCRONIZADA = "sincronizada"
        const val ESTADO_FALLIDA = "fallida"
        const val ESTADO_FALLIDA_CRITICA = "fallida_critica"
        const val MAX_INTENTOS = 3
    }
}
