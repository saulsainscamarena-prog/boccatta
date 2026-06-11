package com.bocatta.pos.domain.util

import com.bocatta.pos.domain.model.ItemCarritoV2
import java.math.BigDecimal

object CarritoCalculator {

    fun calcularSubtotal(items: List<ItemCarritoV2>): BigDecimal {
        return items.fold(BigDecimal.ZERO) { acc, item ->
            acc.add(item.precioFinal.multiply(BigDecimal(item.cantidad)))
        }
    }

    fun calcularTotalVenta(
        subtotal: BigDecimal,
        descuentoLealtad: Double,
        descuentoPromociones: Double,
        descuentoManual: Double
    ): Double {
        val total = subtotal
            .subtract(BigDecimal.valueOf(descuentoLealtad))
            .subtract(BigDecimal.valueOf(descuentoPromociones))
            .subtract(BigDecimal.valueOf(descuentoManual))
            .toDouble()
        return if (total < 0) 0.0 else total
    }

    fun calcularItemsCount(items: List<ItemCarritoV2>): Int {
        return items.sumOf { it.cantidad }
    }
}
