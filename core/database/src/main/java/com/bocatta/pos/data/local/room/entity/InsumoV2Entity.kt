package com.bocatta.pos.data.local.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "insumos_v2")
data class InsumoV2Entity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "nombre")
    val nombre: String,
    @ColumnInfo(name = "categoria")
    val categoria: String?,
    @ColumnInfo(name = "unidadBase")
    val unidadBase: String = "g",
    @ColumnInfo(name = "costoUnitarioBase")
    val costoUnitarioBase: Double = 0.0,
    @ColumnInfo(name = "cantidadEnBase")
    val cantidadEnBase: Double = 0.0,
    @ColumnInfo(name = "stockMinimo")
    val stockMinimo: Double = 10.0
)