// MembresiaDiscountCalculator.kt
package com.bocatta.pos.domain

/**
 * Utility object for calculating loyalty discounts based on the average purchase amount.
 */
object MembresiaDiscountCalculator {
    /**
     * Returns the discount percentage according to the business rules.
     *
     * - >= 500  -> 20%
     * - >= 300  -> 15%
     * - >= 100  -> 10%
     * - else    -> 5%
     */
    fun calcularPorcentajeDescuento(promedioCompras: Double): Double {
        return when {
            promedioCompras >= 500.0 -> 20.0
            promedioCompras >= 300.0 -> 15.0
            promedioCompras >= 100.0 -> 10.0
            else -> 5.0
        }
    }

    /**
     * Calculates the arithmetic mean of a list of purchase amounts.
     * Returns 0.0 for an empty list.
     */
    fun calcularPromedio(compras: List<Double>): Double {
        if (compras.isEmpty()) return 0.0
        return compras.average()
    }
}
