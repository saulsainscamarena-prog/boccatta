package com.bocatta.pos.domain.model

enum class TipoPago { DIARIO, POR_HORA, POR_SEMANA, POR_QUINCENA, POR_MES }
enum class FormaPago { EFECTIVO, TRANSFERENCIA, TARJETA, NOMINA }

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
