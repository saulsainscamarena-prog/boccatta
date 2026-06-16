package com.bocatta.pos.data.local.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "productos_v2")
data class ProductoV2Entity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "tenantId")
    val tenantId: String = "",
    @ColumnInfo(name = "businessType")
    val businessType: String = "RESTAURANT",
    @ColumnInfo(name = "nombre")
    val nombre: String,
    @ColumnInfo(name = "emoji")
    val emoji: String? = "🍽",
    @ColumnInfo(name = "categoria")
    val categoria: String?,
    @ColumnInfo(name = "precioVenta")
    val precioVenta: String?, // JSON string
    @ColumnInfo(name = "esCombo")
    val esCombo: Int = 0,
    @ColumnInfo(name = "recetaId")
    val recetaId: String?,
    @ColumnInfo(name = "toppingsIncluidos")
    val toppingsIncluidos: Int = 2,
    @ColumnInfo(name = "costoToppingExtra")
    val costoToppingExtra: Double = 10.0,
    @ColumnInfo(name = "esProductoTopping")
    val esProductoTopping: Int = 0,
    @ColumnInfo(name = "consumiblesJson")
    val consumiblesJson: String?,
    @ColumnInfo(name = "requiresStock")
    val requiresStock: Int = 0,
    @ColumnInfo(name = "hasVariants")
    val hasVariants: Int = 0,
    @ColumnInfo(name = "barcode")
    val barcode: String?,
    @ColumnInfo(name = "activo")
    val activo: Int = 1
)