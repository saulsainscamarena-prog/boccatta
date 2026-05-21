package com.bocatta.pos.data.repository

import com.bocatta.pos.domain.model.IngredienteReceta
import com.bocatta.pos.domain.model.ItemCarritoV2

object InventoryDeductions {
    val TOPPINGS_SALADOS = listOf("Jamón", "Queso Manchego", "Pepperoni", "Piña", "Champiñones", "Bbq", "Buffalo", "Blue Cheese")
    val TOPPINGS_DULCES = listOf("Fresa", "Durazno", "Plátano", "Oreo", "Nuez", "Bombón", "Philadelphia", "Nutella", "Coco", "Chispas")

    fun calcularParaItem(
        item: ItemCarritoV2,
        recetaIngredientes: List<IngredienteReceta>
    ): Map<String, Double> {
        val qty = item.cantidad.toDouble()
        val deducciones = linkedMapOf<String, Double>()

        recetaIngredientes.forEach { ing ->
            deducciones.add(ing.insumoId, convertirAUnidadBase(ing.cantidad, ing.unidad) * qty)
        }

        item.base?.let { base ->
            base.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach {
                mapearBaseAInsumo(it)?.let { (id, cantidad) -> deducciones.add(id, cantidad * qty) }
            }
        }

        item.toppings.forEach { topping ->
            mapearToppingOAderezoAInsumo(topping)?.let { (id, cantidad) -> deducciones.add(id, cantidad * qty) }
        }

        item.aderezos.forEach { aderezo ->
            mapearToppingOAderezoAInsumo(aderezo)?.let { (id, cantidad) -> deducciones.add(id, cantidad * qty) }
        }

        item.componentesCombo.forEach { componente ->
            calcularParaItem(
                componente.copy(cantidad = componente.cantidad * item.cantidad),
                emptyList()
            ).forEach { (id, cantidad) -> deducciones.add(id, cantidad) }
        }

        if (item.esSeparado && item.producto.esCombo) {
            deducciones.add("charola", qty)
            deducciones.add("papel_hamburguesero", qty)
        }

        return deducciones
    }

    fun calcularPrecioCrepa(precioBase: Double, base: String?, toppings: List<String>, extra: Double = 10.0): Double {
        val basesNormales = base
            ?.split(",")
            ?.map { it.trim() }
            ?.count { it.isNotBlank() }
            ?: 0
        val normales = toppings.count { !esPremium(it) }
        val ingredientesNormales = normales + basesNormales
        val cargoNormal = if (ingredientesNormales >= 3) extra else 0.0
        val cargoPremium = if (toppings.any { esPremium(it) }) extra else 0.0
        return precioBase + cargoNormal + cargoPremium
    }

    fun esPremium(nombre: String): Boolean {
        val t = nombre.normalizado()
        return t.contains("oreo") || t.contains("nuez") || t.contains("bombon")
    }

    fun mapearBaseAInsumo(base: String): Pair<String, Double>? {
        val b = base.normalizado()
        return when {
            b.contains("nutella") -> "nutella_kg" to 20.0
            b.contains("philadelphia") || b.contains("queso crema") -> "queso_crema_kg" to 20.0
            b.contains("zarzamora") -> "zarzamora_kg" to 20.0
            b.contains("lechera") -> "lechera_kg" to 20.0
            b.contains("mermelada") -> "mermelada_fresa" to 20.0
            b.contains("tomate") -> "salsa_tomate_lt" to 20.0
            else -> null
        }
    }

    fun mapearToppingOAderezoAInsumo(nombre: String): Pair<String, Double>? {
        val t = nombre.normalizado()
        return when {
            t.contains("fresa") -> "fresas" to 50.0
            t.contains("durazno") -> "durazno_kg" to 50.0
            t.contains("oreo") -> "oreo" to 6.0
            t.contains("nuez") -> "nuez_kg" to 15.0
            t.contains("bombon") -> "bombon_kg" to 20.0
            t.contains("coco") -> "coco_rayado" to 30.0
            t.contains("colores") -> "granillo_colores" to 20.0
            t.contains("granillo") || t.contains("chocolate") -> "granillo_chocolate" to 20.0
            t.contains("jamon") -> "jamon_kg" to 50.0
            t.contains("pina", true) || t.contains("piña", true) || t.contains("hawaiana", true) -> "pina_kg" to 40.0
            t.contains("pepperoni") || t.contains("peperoni") -> "peperoni_kg" to 40.0
            t.contains("bbq") || t.contains("bqq") -> "bbq" to 20.0
            t.contains("buffalo") -> "buffalo" to 20.0
            t.contains("blue cheese") || t.contains("blue chesse") -> "blue_cheese" to 20.0
            t.contains("valentina") -> "valentina" to 20.0
            t.contains("queso amarillo") -> "queso_amarillo" to 20.0
            t.contains("catsup") || t.contains("ketchup") -> "catsup" to 20.0
            t.contains("mayonesa") -> "mayonesa" to 20.0
            else -> null
        }
    }

    fun convertirAUnidadBase(cantidad: Double, unidad: String): Double {
        return when (unidad.lowercase()) {
            "kg" -> cantidad * 1000.0
            "l", "lt" -> cantidad * 1000.0
            "tz" -> cantidad * 240.0
            "cd" -> cantidad * 15.0
            else -> cantidad
        }
    }

    fun getProductionDeductions(insumoId: String, tandas: Double): Map<String, Double> {
        return when (insumoId) {
            "masa_crepa" -> mapOf(
                "harina_kg" to 2.0 * tandas,
                "leche_lt" to 3.0 * tandas,
                "mantequilla_kg" to 0.125 * tandas,
                "huevos" to 14.0 * tandas
            )
            "helado_vainilla", "helado_chocolate" -> mapOf(
                "base_helado_lt" to 4.0 * tandas,
                "esencia_kg" to 0.2 * tandas
            )
            "fresas_lavadas" -> mapOf(
                "fresa_fresca_kg" to 1.0 * tandas,
                "desinfectante_ml" to 5.0 * tandas
            )
            else -> emptyMap()
        }
    }

    private fun MutableMap<String, Double>.add(id: String, cantidad: Double) {
        this[id] = (this[id] ?: 0.0) + cantidad
    }

    private fun String.normalizado(): String {
        return java.text.Normalizer.normalize(this, java.text.Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}".toRegex(), "")
            .lowercase()
    }
}

