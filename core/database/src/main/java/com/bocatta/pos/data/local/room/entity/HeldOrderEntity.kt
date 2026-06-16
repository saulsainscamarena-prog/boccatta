package com.bocatta.pos.data.local.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "held_orders")
data class HeldOrderEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "carritoJson")
    val carritoJson: String,
    @ColumnInfo(name = "clienteJson")
    val clienteJson: String?,
    @ColumnInfo(name = "nota")
    val nota: String = "",
    @ColumnInfo(name = "fecha")
    val fecha: Long,
    @ColumnInfo(name = "sucursal")
    val sucursal: String,
    @ColumnInfo(name = "total")
    val total: Double = 0.0,
    @ColumnInfo(name = "modalidad")
    val modalidad: String = "LOCAL",
    @ColumnInfo(name = "mesaId")
    val mesaId: String?
)