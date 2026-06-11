package com.bocatta.pos.data.local.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity para el control de folios de tickets offline por sucursal.
 * Mapeo 1:1 con la tabla `folios_offline` de OfflineDatabase.
 */
@Entity(tableName = "folios_offline", primaryKeys = ["tenantId", "sucursal"])
data class FolioEntity(
    val tenantId: String,
    val sucursal: String,
    @ColumnInfo(name = "ultimoTicket", defaultValue = "0")
    val ultimoTicket: Long = 0L,
    @ColumnInfo(name = "updatedAt", defaultValue = "0")
    val updatedAt: Long = 0L
)
