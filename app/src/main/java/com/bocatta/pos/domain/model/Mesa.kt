package com.bocatta.pos.domain.model

enum class EstadoMesa { LIBRE, OCUPADA, RESERVADA, INACTIVA }

data class Zona(
    val id: String = "",
    val nombre: String = "",
    val activo: Boolean = true,
    val orden: Int = 0
)

data class Mesa(
    val id: String = "",
    val numero: Int = 0,
    val capacidad: Int = 4,
    val zonaId: String = "general",
    val estado: EstadoMesa = EstadoMesa.LIBRE,
    val ordenActual: String? = null
)

enum class ModalidadOrden { LOCAL, PARA_LLEVAR, DELIVERY }

data class OrdenMesa(
    val id: String = "",
    val mesaId: String = "",
    val zonaId: String = "",
    val empleadoId: String = "",
    val empleadoNombre: String = "",
    val modalidad: ModalidadOrden = ModalidadOrden.LOCAL,
    val clienteId: String? = null,
    val nota: String = "",
    val iniciadoEn: Long = 0L
)

data class TransferenciaMesa(
    val id: String = "",
    val mesaId: String = "",
    val ordenId: String = "",
    val zonaOrigenId: String = "",
    val zonaDestinoId: String = "",
    val empleadoOrigenId: String = "",
    val empleadoDestinoId: String = "",
    val motivo: String = "",
    val fecha: Long = System.currentTimeMillis()
)
