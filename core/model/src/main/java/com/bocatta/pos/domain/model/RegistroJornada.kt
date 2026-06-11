package com.bocatta.pos.domain.model

data class RegistroJornada(
    val id: String = "",
    val usuario: String = "",
    val sucursal: String = "",
    val accion: String = "",
    val rol: String = "",
    val sesionId: String = "",
    val timestamp: Long = 0L
)

