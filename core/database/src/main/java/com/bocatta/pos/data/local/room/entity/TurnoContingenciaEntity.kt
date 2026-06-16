package com.bocatta.pos.data.local.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "turnos_contingencia")
data class TurnoContingenciaEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "tenantId")
    val tenantId: String = "tenant_pionero",
    @ColumnInfo(name = "sucursal")
    val sucursal: String,
    @ColumnInfo(name = "usuarioId")
    val usuarioId: String,
    @ColumnInfo(name = "usuarioNombre")
    val usuarioNombre: String,
    @ColumnInfo(name = "rol")
    val rol: String,
    @ColumnInfo(name = "fondoInicial")
    val fondoInicial: Double,
    @ColumnInfo(name = "fechaApertura")
    val fechaApertura: Long,
    @ColumnInfo(name = "fechaCierre")
    val fechaCierre: Long?,
    @ColumnInfo(name = "estado")
    val estado: String = "abierto",
    @ColumnInfo(name = "efectivoContado")
    val efectivoContado: Double = 0.0,
    @ColumnInfo(name = "tarjetaContada")
    val tarjetaContada: Double = 0.0,
    @ColumnInfo(name = "syncPendiente")
    val syncPendiente: Int = 1
)