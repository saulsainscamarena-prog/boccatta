package com.bocatta.pos.domain.engine

import com.bocatta.pos.domain.model.ConfigResult

object PricingEngine {

    private val premiumToppings = setOf("oreo", "bombon", "nuez")
    private val precioExtraTresMas = 10.0
    private val precioExtraPremium = 10.0

    fun calcularPrecioCrepa(precioBase: Double, config: ConfigResult): Double {
        val toppings = (config["toppings"] ?: emptyList()).map { it.lowercase() }
        val bases = (config["base"] ?: emptyList()).map { it.lowercase() }
        val totalItems = bases.size + toppings.size

        val extraPorCantidad = if (totalItems >= 3) precioExtraTresMas else 0.0
        val tienePremium = toppings.any { it in premiumToppings }
        val extraPremium = if (tienePremium) precioExtraPremium else 0.0

        return precioBase + extraPorCantidad + extraPremium
    }

    fun calcularPrecioFrappe(precioBase: Double, config: ConfigResult): Double {
        val tieneBase = (config["base"]?.firstOrNull())
            ?.let { it.lowercase() !in listOf("sin base", "") } == true
        return if (tieneBase) precioBase + 5.0 else precioBase
    }

    fun calcularPrecioProducto(precioBase: Double, categoria: String, config: ConfigResult): Double {
        val cat = categoria.lowercase()
        return when {
            cat.contains("crepa") || cat == "combos" -> calcularPrecioCrepa(precioBase, config)
            cat.contains("frape") || cat.contains("frappe") -> calcularPrecioFrappe(precioBase, config)
            else -> precioBase
        }
    }

    fun esPremium(topping: String): Boolean = topping.lowercase() in premiumToppings
}
