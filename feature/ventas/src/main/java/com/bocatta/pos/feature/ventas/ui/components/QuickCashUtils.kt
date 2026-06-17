package com.bocatta.pos.feature.ventas.ui.components

/**
 * Generates recommended cash amounts for a given total.
 * Useful for quick-payment buttons on the POS checkout screen.
 *
 * @param total the sale total
 * @return a sorted list of up to 7 unique cash amounts ≥ total,
 *         or an empty list when [total] is invalid (≤ 0, NaN, or Infinite).
 */
fun quickCashAmounts(total: Double): List<Double> {
    if (total <= 0 || total.isNaN() || total.isInfinite()) return emptyList()

    val seen = linkedSetOf(total)
    val denominations = listOf(20.0, 50.0, 100.0, 200.0, 500.0, 1000.0)

    // Add the exact total first, then round-ups to each denomination
    for (denom in denominations) {
        if (seen.size >= 7) break
        if (total <= denom) {
            seen.add(denom)
        } else {
            val roundedUp = kotlin.math.ceil(total / denom) * denom
            seen.add(roundedUp)
        }
    }

    // Ensure at least one clean denomination above the total
    for (denom in denominations) {
        if (seen.size >= 7) break
        if (denom > total) seen.add(denom)
    }

    return seen.take(7).sorted()
}
