package com.bocatta.pos.data.local.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ventas_pendientes")
data class VentaOfflineEntity(
    @PrimaryKey val id: String,
    val ticket: Long,
    val codigoTicket: String,
    val total: Double,
    val descuentoLealtad: Double,
    val fecha: Long,
    val sucursal: String,
    val atendio: String,
    val metodoPago: String,
    val esConsumoEmpleado: Int,
    val clienteId: String?,
    val carritoJson: String,
    val estado: String,
    val intentos: Int,
    val ultimoIntento: Long?
)

@Entity(tableName = "operaciones_pendientes")
data class OperacionOfflineEntity(
    @PrimaryKey val id: String,
    val tipo: String,
    val ventaId: String?,
    val motivo: String,
    val usuarioId: String,
    val sucursal: String,
    val fecha: Long,
    val requiereAprobacion: Int,
    val dataJson: String,
    val estado: String,
    val intentos: Int
)

@Entity(tableName = "insumos_v2")
data class InsumoEntity(
    @PrimaryKey val id: String,
    val nombre: String,
    val categoria: String?,
    val unidadBase: String,
    val costoUnitarioBase: Double,
    val cantidadEnBase: Double,
    val stockMinimo: Double
)

@Entity(tableName = "consumibles_v2")
data class ConsumibleEntity(
    @PrimaryKey val id: String,
    val nombre: String,
    val unidadBase: String,
    val stockActual: Double,
    val stockMinimo: Double
)

@Entity(tableName = "productos_v2")
data class ProductoEntity(
    @PrimaryKey val id: String,
    val nombre: String,
    val emoji: String?,
    val categoria: String?,
    val precioVenta: String,
    val esCombo: Int,
    val recetaId: String?,
    val toppingsIncluidos: Int,
    val costoToppingExtra: Double,
    val esProductoTopping: Int,
    val consumiblesJson: String?
)

@Entity(tableName = "recetas_v2")
data class RecetaEntity(
    @PrimaryKey val id: String,
    val nombre: String,
    val productoId: String?,
    val rendimientoPorcion: Double
)

@Entity(
    tableName = "ingredientes_receta",
    foreignKeys = [androidx.room.ForeignKey(
        entity = RecetaEntity::class,
        parentColumns = ["id"],
        childColumns = ["recetaId"],
        onDelete = androidx.room.ForeignKey.CASCADE
    )]
)
data class IngredienteRecetaEntity(
    @PrimaryKey val id: String,
    val recetaId: String,
    val insumoId: String,
    val nombreInsumo: String?,
    val cantidad: Double,
    val unidad: String
)

@Entity(
    tableName = "presentaciones_insumo",
    foreignKeys = [androidx.room.ForeignKey(
        entity = InsumoEntity::class,
        parentColumns = ["id"],
        childColumns = ["insumoId"],
        onDelete = androidx.room.ForeignKey.CASCADE
    )]
)
data class PresentacionEntity(
    @PrimaryKey val id: String,
    val insumoId: String,
    val nombre: String,
    val unidadEquivalente: String,
    val factorConversionABase: Double,
    val cantidadDisponible: Double,
    val ultimoPrecioPagado: Double
)
