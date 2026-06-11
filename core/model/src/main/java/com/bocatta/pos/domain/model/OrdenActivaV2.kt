package com.bocatta.pos.domain.model

import java.math.BigDecimal

data class OrdenActivaV2(
    val id: String = "",
    val modalidad: ModalidadOrden = ModalidadOrden.LOCAL,
    val mesaId: String? = null,
    val cliente: ClienteV2? = null,
    val items: List<ItemCarritoV2> = emptyList(),
    val nota: String = "",
    val descuentoManual: BigDecimal = BigDecimal.ZERO,
    val descuentoLealtad: BigDecimal = BigDecimal.ZERO,
    val esConsumoEmpleado: Boolean = false,
    val iniciadoEn: Long = System.currentTimeMillis(),
    val empleadoId: String = "",
    val empleadoNombre: String = ""
)
