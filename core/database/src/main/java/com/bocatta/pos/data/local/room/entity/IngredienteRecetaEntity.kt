package com.bocatta.pos.data.local.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ingredientes_receta")
data class IngredienteRecetaEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "recetaId")
    val recetaId: String,
    @ColumnInfo(name = "insumoId")
    val insumoId: String,
    @ColumnInfo(name = "nombreInsumo")
    val nombreInsumo: String?,
    @ColumnInfo(name = "cantidad")
    val cantidad: Double,
    @ColumnInfo(name = "unidad")
    val unidad: String = "g"
)