package com.bocatta.pos.domain.model

enum class TipoDuracion(val firestoreKey: String) { FIJA("fija"), ABIERTA("abierta") }

data class HorarioEmpleado(
    val id: String = "",
    val empleadoId: String = "",
    val diaSemana: Int = 0,
    val horaInicio: String = "",
    val horaFin: String = "",
    val tipoDuracion: TipoDuracion = TipoDuracion.FIJA,
    val activo: Boolean = true
)

enum class EstadoJornada(val firestoreKey: String) { ACTIVA("activa"), EN_PAUSA("en_pausa"), CERRADA("cerrada"), INCOMPLETA("incompleta"), ANOMALA("anomala") }

data class PausaJornada(
    val inicio: Long = 0L,
    val fin: Long? = null,
    val motivo: String = "",
    val automatica: Boolean = false
)

data class JornadaLaboral(
    val id: String = "",
    val empleadoId: String = "",
    val empleadoNombre: String = "",
    val dia: Long = 0L,
    val horarioId: String = "",
    val inicioReal: Long = 0L,
    val finReal: Long? = null,
    val pausas: List<PausaJornada> = emptyList(),
    val estado: EstadoJornada = EstadoJornada.ACTIVA,
    val horasEfectivas: Double = 0.0,
    val horasProgramadas: Double = 0.0,
    val dispositivo: String = "",
    val notas: String = ""
)

