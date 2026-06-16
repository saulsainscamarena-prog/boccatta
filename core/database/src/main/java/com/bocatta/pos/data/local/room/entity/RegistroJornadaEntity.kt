package com.bocatta.pos.data.local.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "registro_jornadas")
data class RegistroJornadaEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "usuario")
    val usuario: String,
    @ColumnInfo(name = "sucursal")
    val sucursal: String,
    @ColumnInfo(name = "accion")
    val accion: String,
    @ColumnInfo(name = "rol")
    val rol: String = "",
    @ColumnInfo(name = "sesionId")
    val sesionId: String = "",
    @ColumnInfo(name = "timestamp")
    val timestamp: Long
)