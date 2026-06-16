package com.bocatta.pos.data.local.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recetas_v2")
data class RecetaV2Entity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "nombre")
    val nombre: String,
    @ColumnInfo(name = "productoId")
    val productoId: String?,
    @ColumnInfo(name = "rendimientoPorcion")
    val rendimientoPorcion: Double = 1.0
)