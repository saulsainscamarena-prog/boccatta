package com.bocatta.pos.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class HeldOrder(
    val id: String = "",
    val carritoJson: String = "",
    val clienteJson: String? = null,
    val nota: String = "",
    val fecha: Long = 0L,
    val sucursal: String = "",
    val total: Double = 0.0
)
