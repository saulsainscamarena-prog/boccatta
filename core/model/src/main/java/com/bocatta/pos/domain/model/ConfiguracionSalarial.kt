package com.bocatta.pos.domain.model

enum class TipoPago(val firestoreKey: String) { DIARIO("diario"), POR_HORA("por_hora"), POR_SEMANA("por_semana"), POR_QUINCENA("por_quincena"), POR_MES("por_mes") }
enum class FormaPago(val firestoreKey: String) { EFECTIVO("efectivo"), TRANSFERENCIA("transferencia"), TARJETA("tarjeta"), NOMINA("nomina") }

data class ConfiguracionSalarial(
    val empleadoId: String = "",
    val tipoPago: TipoPago = TipoPago.DIARIO,
    val salarioBase: Double = 0.0,
    val formaPago: FormaPago = FormaPago.EFECTIVO,
    val banco: String = "",
    val clabe: String = "",
    val cuenta: String = "",
    val diaPago: Int = 15,
    val toleranciaMinutos: Int = 15,
    val deduccionIsr: Double = 0.0,
    val deduccionImss: Double = 0.0,
    val deduccionPrestamo: Double = 0.0,
    val activo: Boolean = true
)

data class RegistroPago(
    val id: String = "",
    val empleadoId: String = "",
    val empleadoNombre: String = "",
    val periodoInicio: Long = 0L,
    val periodoFin: Long = 0L,
    val totalHoras: Double = 0.0,
    val totalDias: Int = 0,
    val salarioBruto: Double = 0.0,
    val deducciones: Double = 0.0,
    val salarioNeto: Double = 0.0,
    val formaPago: FormaPago = FormaPago.EFECTIVO,
    val pagado: Boolean = false,
    val fechaPago: Long? = null,
    val comprobanteUrl: String? = null,
    val notas: String = ""
)



