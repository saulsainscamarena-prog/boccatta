package com.bocatta.pos.domain.model

data class PermisoEmpleado(
    val empleadoId: String = "",
    val puedeTomarOrden: Boolean = true,
    val puedeCobrarEfectivo: Boolean = false,
    val puedeCobrarTarjeta: Boolean = true,
    val puedePreparar: Boolean = false,
    val puedeGestionarMesas: Boolean = false,
    val puedeTransferirMesas: Boolean = false,
    val puedeAprobarCambiosZona: Boolean = false,
    val puedeCerrarTurnoAjeno: Boolean = false,
    val puedeVerSueldos: Boolean = false,
    val puedeGestionarEmpleados: Boolean = false,
    val zonasAsignadas: List<String> = emptyList(),
    val puesto: String = "Mesero"
)
