package com.bocatta.pos.data.local.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "presentaciones_insumo")
data class PresentacionInsV2Entity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "insumoId")
    val insumoId: String,
    @ColumnInfo(name = "nombre")
    val nombre: String,
    @ColumnInfo(name = "unidadEquivalente")
    val unidadEquivalente: String = "pz",
    @ColumnInfo(name = "factorConversionABase")
    val factorConversionABase: Double = 1.0,
    @ColumnInfo(name = "cantidadDisponible")
    val cantidadDisponible: Double = 0.0,
    @ColumnInfo(name = "ultimoPrecioPagado")
    val ultimoPrecioPagado: Double = 0.0
)
