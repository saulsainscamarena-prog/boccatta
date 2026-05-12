package com.bocatta.pos.domain.model

data class RegistroProduccion(
    val id: String = "",
    val insumoId: String = "", // ej: "ins_masa_crepa"
    val fecha: Long = 0L,
    val cantidadProducida: Double = 0.0,
    val sobranteAnterior: Double = 0.0,
    val sobranteFinal: Double = 0.0,
    val ventasRegistradas: Int = 0,
    val rendimientoReal: Double = 0.0 // Cantidad de ventas / (sobranteAnterior + cantidadProducida - sobranteFinal)
)

data class SnapshotDiario(
    val fecha: Long = 0L,
    val insumoId: String = "",
    val stockApertura: Double = 0.0,
    val stockCierre: Double = 0.0,
    val ventasSistema: Int = 0,
    val diferencia: Double = 0.0 // Discrepancia entre lo que dice el sistema y lo físico
)

