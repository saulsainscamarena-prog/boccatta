package com.bocatta.pos.domain.model

import java.math.BigDecimal
import java.math.RoundingMode

data class SplitPayment(
    val personas: Int = 1,
    val partes: List<SplitParte> = emptyList()
)

data class SplitParte(
    val personaIndex: Int = 0,
    val monto: BigDecimal = BigDecimal.ZERO,
    val metodoPago: MetodoPago = MetodoPago.EFECTIVO
)

fun calcularSplit(total: Double, personas: Int, metodoPorDefecto: MetodoPago): List<SplitParte> {
    if (personas <= 1) return listOf(SplitParte(0, BigDecimal.valueOf(total), metodoPorDefecto))

    val totalBD = BigDecimal.valueOf(total)
    val porcion = totalBD.divide(BigDecimal.valueOf(personas.toLong()), 2, RoundingMode.HALF_UP)
    val partes = mutableListOf<SplitParte>()

    repeat(personas - 1) { i ->
        partes.add(SplitParte(i, porcion, metodoPorDefecto))
    }

    // Última persona absorbe el redondeo
    val suma = porcion.multiply(BigDecimal.valueOf((personas - 1).toLong()))
    val resto = totalBD.subtract(suma)
    partes.add(SplitParte(personas - 1, resto, metodoPorDefecto))

    return partes
}

