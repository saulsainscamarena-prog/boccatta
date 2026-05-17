package com.bocatta.pos.domain.model

enum class EstadoSolicitudTurno { PENDIENTE, APROBADA, RECHAZADA }

data class SolicitudTurnoExtra(
    val id: String = "",
    val empleadoId: String = "",
    val empleadoNombre: String = "",
    val zonaId: String = "",
    val zonaNombre: String = "",
    val horarioProgramado: String = "",
    val horaSolicitada: Long = 0L,
    val motivo: String = "",
    val estado: EstadoSolicitudTurno = EstadoSolicitudTurno.PENDIENTE,
    val respondidoEn: Long? = null,
    val respondidoPor: String? = null
)

