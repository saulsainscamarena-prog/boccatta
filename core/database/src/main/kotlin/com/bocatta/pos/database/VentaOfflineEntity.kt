package com.bocatta.pos.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "ventas_pendientes")
data class VentaOfflineEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: Long,
    @ColumnInfo(name = "tenant_id") val tenantId: String?,
    @ColumnInfo(name = "ticket") val ticket: String?,
    @ColumnInfo(name = "codigo_ticket") val codigoTicket: String?,
    @ColumnInfo(name = "total") val total: Double?,
    @ColumnInfo(name = "descuento_lealtad") val descuentoLealtad: Double?,
    @ColumnInfo(name = "descuento_promociones") val descuentoPromociones: Double?,
    @ColumnInfo(name = "descuento_manual") val descuentoManual: Double?,
    @ColumnInfo(name = "propina") val propina: Double?,
    @ColumnInfo(name = "nota_orden") val notaOrden: String?,
    @ColumnInfo(name = "fecha") val fecha: String?,
    @ColumnInfo(name = "sucursal") val sucursal: String?,
    @ColumnInfo(name = "atendio") val atendio: String?,
    @ColumnInfo(name = "metodo_pago") val metodoPago: String?,
    @ColumnInfo(name = "es_consumo_empleado") val esConsumoEmpleado: Boolean?,
    @ColumnInfo(name = "cliente_id") val clienteId: String?,
    @ColumnInfo(name = "carrito_json") val carritoJson: String?,
    @ColumnInfo(name = "estado") val estado: String?,
    @ColumnInfo(name = "intentos") val intentos: Int?,
    @ColumnInfo(name = "ultimo_intento") val ultimoIntento: String?
)