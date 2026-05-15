package com.bocatta.pos.domain.engine

import com.bocatta.pos.domain.model.ConfigResult

object PricingEngine {

    private const val precioExtraTresMas = 10.0

    fun calcularPrecioCrepa(precioBase: Double, config: ConfigResult, preciosExtra: Map<String, Double> = emptyMap()): Double {
        val toppings = (config["toppings"] ?: emptyList()).map { it.lowercase() }
        val bases = (config["base"] ?: emptyList()).map { it.lowercase() }
        val totalItems = bases.size + toppings.size

        val extraPorCantidad = if (totalItems >= 3) precioExtraTresMas else 0.0
        val extraPremium = toppings.sumOf { t -> preciosExtra.entries.find { it.key.lowercase() == t }?.value ?: 0.0 }

        return precioBase + extraPorCantidad + extraPremium
    }

    fun calcularPrecioFrappe(precioBase: Double, config: ConfigResult): Double {
        val tieneBase = (config["base"]?.firstOrNull())
            ?.let { it.lowercase() !in listOf("sin base", "") } == true
        return if (tieneBase) precioBase + 5.0 else precioBase
    }

    fun calcularPrecioProducto(precioBase: Double, categoria: String, config: ConfigResult, preciosExtra: Map<String, Double> = emptyMap()): Double {
        val cat = categoria.lowercase()
        return when {
            cat.contains("crepa") || cat == "combos" -> calcularPrecioCrepa(precioBase, config, preciosExtra)
            cat.contains("frape") || cat.contains("frappe") -> calcularPrecioFrappe(precioBase, config)
            else -> {
                val extras = config.entries.flatMap { (key, values) ->
                    values.map { v -> preciosExtra.entries.find { it.key.lowercase() == v.lowercase() }?.value ?: 0.0 }
                }.sum()
                precioBase + extras
            }
        }
    }
}
