package com.bocatta.pos.domain

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs

object CuadreCajaManager {
    data class ResultadoCuadre(
        val totalEsperado: BigDecimal,
        val totalReal: BigDecimal,
        val diferencia: BigDecimal,
        val esCorrecto: Boolean
    )

    fun calcularCuadre(
        ventasTotal: BigDecimal,
        gastosTotal: BigDecimal,
        fondoInicial: BigDecimal,
        efectivoEnCaja: BigDecimal,
        toleranciaEfectivo: BigDecimal = BigDecimal.ZERO,
        toleranciaTarjeta: BigDecimal = BigDecimal.ZERO
    ): ResultadoCuadre {
        val esperado = fondoInicial.add(ventasTotal).subtract(gastosTotal)
        val diferencia = efectivoEnCaja.subtract(esperado)
        
        // El cuadre es correcto si la diferencia absoluta es menor o igual a la tolerancia
        val esCorrecto = abs(diferencia.toDouble()) <= toleranciaEfectivo.toDouble()
        
        return ResultadoCuadre(
            totalEsperado = esperado.setScale(2, RoundingMode.HALF_UP),
            totalReal = efectivoEnCaja.setScale(2, RoundingMode.HALF_UP),
            diferencia = diferencia.setScale(2, RoundingMode.HALF_UP),
            esCorrecto = esCorrecto
        )
    }
}

