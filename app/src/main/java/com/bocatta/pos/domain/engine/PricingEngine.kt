package com.bocatta.pos.domain.engine

import com.bocatta.pos.domain.model.ConfigResult
import java.math.BigDecimal
import java.math.RoundingMode

object PricingEngine {

    private val PRECIO_EXTRA_TRES_MAS = BigDecimal("10.00")
    private val PRECIO_EXTRA_BASE_FRAPPE = BigDecimal("5.00")

    fun calcularPrecioCrepa(precioBase: Double, config: ConfigResult, preciosExtra: Map<String, Double> = emptyMap()): Double {
        val base = BigDecimal.valueOf(precioBase)
        val toppings = (config["toppings"] ?: emptyList()).map { it.lowercase() }
        val bases = (config["base"] ?: emptyList()).map { it.lowercase() }
        val totalItems = bases.size + toppings.size

        val extraPorCantidad = if (totalItems >= 3) PRECIO_EXTRA_TRES_MAS else BigDecimal.ZERO
        val extraPremium = toppings.fold(BigDecimal.ZERO) { acc, t ->
            val v = preciosExtra.entries.find { it.key.lowercase() == t }?.value ?: 0.0
            acc + BigDecimal.valueOf(v)
        }

        return (base + extraPorCantidad + extraPremium).setScale(2, RoundingMode.HALF_UP).toDouble()
    }

    fun calcularPrecioFrappe(precioBase: Double, config: ConfigResult): Double {
        val base = BigDecimal.valueOf(precioBase)
        val tieneBase = (config["base"]?.firstOrNull())
            ?.let { it.lowercase() !in listOf("sin base", "") } == true
        val total = if (tieneBase) base + PRECIO_EXTRA_BASE_FRAPPE else base
        return total.setScale(2, RoundingMode.HALF_UP).toDouble()
    }

    fun calcularPrecioProducto(precioBase: Double, categoria: String, config: ConfigResult, preciosExtra: Map<String, Double> = emptyMap()): Double {
        val cat = categoria.lowercase()
        return when {
            cat.contains("crepa") || cat == "combos" -> calcularPrecioCrepa(precioBase, config, preciosExtra)
            cat.contains("frape") || cat.contains("frappe") -> calcularPrecioFrappe(precioBase, config)
            else -> {
                val base = BigDecimal.valueOf(precioBase)
                val extras = config.entries.fold(BigDecimal.ZERO) { acc, (_, values) ->
                    acc + values.fold(BigDecimal.ZERO) { inner, v ->
                        val extra = preciosExtra.entries.find { it.key.lowercase() == v.lowercase() }?.value ?: 0.0
                        inner + BigDecimal.valueOf(extra)
                    }
                }
                (base + extras).setScale(2, RoundingMode.HALF_UP).toDouble()
            }
        }
    }
}

