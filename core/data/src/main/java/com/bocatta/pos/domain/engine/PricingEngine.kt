package com.bocatta.pos.domain.engine

import com.bocatta.pos.domain.model.ConfigResult
import java.math.BigDecimal
import java.math.RoundingMode

object PricingEngine {

    private val PRECIO_EXTRA_TRES_MAS = BigDecimal("10.00")
    private val PRECIO_EXTRA_BASE_FRAPPE = BigDecimal("5.00")
    val PRECIO_TOPPING_PREMIUM_DEFAULT = 10.0
    private val TOPPINGS_PREMIUM = listOf("oreo", "nuez", "bombon")
    private val INGREDIENTES_NORMALES_UMBRAL = 3
    private val BASES_IGNORADAS = listOf("sin base", "")

    fun calcularPrecioCrepa(precioBase: Double, config: ConfigResult, preciosExtra: Map<String, Double> = emptyMap()): Double {
        val base = BigDecimal.valueOf(precioBase)
        val toppings = (config["toppings"] ?: emptyList()).map { it.lowercase(java.util.Locale.getDefault()) }
        val bases = (config["base"] ?: emptyList())
            .map { it.lowercase(java.util.Locale.getDefault()) }
            .filter { it !in BASES_IGNORADAS }

        val premiumToppings = toppings.filter { t ->
            preciosExtra.keys.any { k -> k.lowercase(java.util.Locale.getDefault()) == t } || TOPPINGS_PREMIUM.any { t.contains(it) }
        }
        val normalToppingsCount = toppings.size - premiumToppings.size
        val normalIngredientsCount = bases.size + normalToppingsCount

        val extraPorCantidad = if (normalIngredientsCount >= INGREDIENTES_NORMALES_UMBRAL) PRECIO_EXTRA_TRES_MAS else BigDecimal.ZERO
        val extraPremium = premiumToppings.fold(BigDecimal.ZERO) { acc, t ->
            val v = preciosExtra.entries.find { it.key.lowercase(java.util.Locale.getDefault()) == t }?.value ?: PRECIO_TOPPING_PREMIUM_DEFAULT
            acc + BigDecimal.valueOf(v)
        }

        return (base + extraPorCantidad + extraPremium).setScale(2, RoundingMode.HALF_UP).toDouble()
    }

    fun calcularPrecioFrappe(precioBase: Double, config: ConfigResult): Double {
        val base = BigDecimal.valueOf(precioBase)
        val tieneBase = (config["base"]?.firstOrNull())
            ?.let { it.lowercase(java.util.Locale.getDefault()) !in listOf("sin base", "") } == true
        val total = if (tieneBase) base + PRECIO_EXTRA_BASE_FRAPPE else base
        return total.setScale(2, RoundingMode.HALF_UP).toDouble()
    }

    fun calcularPrecioProducto(precioBase: Double, categoria: String, config: ConfigResult, preciosExtra: Map<String, Double> = emptyMap()): Double {
        val cat = categoria.lowercase(java.util.Locale.getDefault())
        return when {
            cat.contains("crepa") || cat == "combos" -> calcularPrecioCrepa(precioBase, config, preciosExtra)
            cat.contains("frape") || cat.contains("frappe") -> calcularPrecioFrappe(precioBase, config)
            else -> {
                val base = BigDecimal.valueOf(precioBase)
                val extras = config.entries.fold(BigDecimal.ZERO) { acc, (_, values) ->
                    acc + values.fold(BigDecimal.ZERO) { inner, v ->
                        val extra = preciosExtra.entries.find { it.key.lowercase(java.util.Locale.getDefault()) == v.lowercase(java.util.Locale.getDefault()) }?.value ?: 0.0
                        inner + BigDecimal.valueOf(extra)
                    }
                }
                (base + extras).setScale(2, RoundingMode.HALF_UP).toDouble()
            }
        }
    }

    /**
     * Calcula el precio de un producto usando parámetros directos (sin ConfigResult).
     * Fuente de verdad única para el path de agregarAlCarrito() sin config schema.
     * Usa el mismo umbral (>= 3 ingredientes normales) que calcularPrecioCrepa().
     */
    fun calcularPrecioDirecto(
        precioBase: Double,
        categoria: String,
        base: String?,
        toppings: List<String>,
        costoToppingExtra: Double = 10.0,
        @Suppress("UNUSED_PARAMETER") preciosExtra: Map<String, Double> = emptyMap()
    ): Double {
        val cat = categoria.lowercase(java.util.Locale.getDefault())
        return when {
            cat.contains("crepa") || cat == "combos" -> {
                val basesCount = base?.split(",")
                    ?.map { it.trim().lowercase(java.util.Locale.getDefault()) }
                    ?.count { it.isNotBlank() && it != "sin base" }
                    ?: 0
                val normales = toppings.count { !esPremiumTopping(it) }
                val premiumCount = toppings.count { esPremiumTopping(it) }
                val ingredientesNormales = normales + basesCount
                val cargoNormal = if (ingredientesNormales >= INGREDIENTES_NORMALES_UMBRAL) costoToppingExtra else 0.0
                val cargoPremium = premiumCount * costoToppingExtra
                BigDecimal.valueOf(precioBase + cargoNormal + cargoPremium)
                    .setScale(2, RoundingMode.HALF_UP).toDouble()
            }
            cat.contains("frape") || cat.contains("frappe") -> {
                val tieneBase = base?.let { it.lowercase(java.util.Locale.getDefault()) !in listOf("sin base", "") } == true
                val total = if (tieneBase) precioBase + PRECIO_EXTRA_BASE_FRAPPE.toDouble() else precioBase
                BigDecimal.valueOf(total).setScale(2, RoundingMode.HALF_UP).toDouble()
            }
            else -> {
                val premiumCount = toppings.count { esPremiumTopping(it) }
                val normalesCount = toppings.count { !esPremiumTopping(it) }
                val cargoNormal = if (normalesCount >= 3) costoToppingExtra else 0.0
                val cargoPremium = premiumCount * costoToppingExtra
                BigDecimal.valueOf(precioBase + cargoNormal + cargoPremium)
                    .setScale(2, RoundingMode.HALF_UP).toDouble()
            }
        }
    }

    /** Determina si un topping es premium (oreo, nuez, bombón). */
    fun esPremiumTopping(nombre: String): Boolean {
        val t = nombre.lowercase(java.util.Locale.getDefault())
        return TOPPINGS_PREMIUM.any { t.contains(it) }
    }
}
