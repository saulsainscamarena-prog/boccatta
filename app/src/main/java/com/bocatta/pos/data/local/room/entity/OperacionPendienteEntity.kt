package com.bocatta.pos.data.local.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity para operaciones offline pendientes (devoluciones, cancelaciones, mermas).
 * Mapeo 1:1 con la tabla `operaciones_pendientes` de OfflineDatabase.
 */
@Entity(tableName = "operaciones_pendientes")
data class OperacionPendienteEntity(
    @PrimaryKey
    val id: String,
    val tipo: String,
    @ColumnInfo(name = "ventaId")
    val ventaId: String? = null,
    val motivo: String,
    @ColumnInfo(name = "usuarioId")
    val usuarioId: String,
    val sucursal: String,
    val fecha: Long,
    @ColumnInfo(name = "requiereAprobacion", defaultValue = "1")
    val requiereAprobacion: Boolean = true,
    @ColumnInfo(name = "dataJson")
    val dataJson: String,
    @ColumnInfo(defaultValue = "pendiente")
    val estado: String = ESTADO_PENDIENTE,
    @ColumnInfo(defaultValue = "0")
    val intentos: Int = 0
) {
    companion object {
        const val TIPO_DEVOLUCION = "devolucion"
        const val TIPO_CANCELACION = "cancelacion"
        const val TIPO_MERMA = "merma"
        const val ESTADO_PENDIENTE = "pendiente"
        const val ESTADO_SINCRONIZADA = "sincronizada"
        const val ESTADO_FALLIDA = "fallida"
        const val MAX_INTENTOS = 3
    }
}
