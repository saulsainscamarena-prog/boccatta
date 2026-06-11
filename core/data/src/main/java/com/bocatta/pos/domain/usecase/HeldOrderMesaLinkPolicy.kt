package com.bocatta.pos.domain.usecase

import com.bocatta.pos.domain.model.HeldOrder

data class MesaLinkAction(
    val mesaId: String,
    val estado: String,
    val ordenId: String?
)

class HeldOrderMesaLinkPolicy {
    fun alGuardar(order: HeldOrder): MesaLinkAction? {
        val mesaId = order.mesaId?.takeIf { it.isNotBlank() } ?: return null
        return MesaLinkAction(
            mesaId = mesaId,
            estado = "OCUPADA",
            ordenId = order.id
        )
    }

    fun alBorrar(order: HeldOrder?, liberarMesa: Boolean): MesaLinkAction? {
        if (!liberarMesa) return null
        val mesaId = order?.mesaId?.takeIf { it.isNotBlank() } ?: return null
        return MesaLinkAction(
            mesaId = mesaId,
            estado = "LIBRE",
            ordenId = null
        )
    }
}
