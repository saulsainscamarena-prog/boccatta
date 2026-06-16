package com.bocatta.pos.data.local.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "consumibles_v2")
data class ConsumibleV2Entity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "nombre")
    val nombre: String,
    @ColumnInfo(name = "unidadBase")
    val unidadBase: String = "pz",
    @ColumnInfo(name = "stockActual")
    val stockActual: Double = 0.0,
    @ColumnInfo(name = "stockMinimo")
    val stockMinimo: Double = 50.0
)