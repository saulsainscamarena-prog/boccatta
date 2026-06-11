package com.bocatta.pos.data.repository

import com.bocatta.pos.domain.engine.PricingEngine
import com.bocatta.pos.domain.model.IngredienteReceta
import com.bocatta.pos.domain.model.ItemCarritoV2
import java.util.Locale

object InventoryDeductions {
    val TOPPINGS_SALADOS = listOf("Jamón", "Queso Manchego", "Pepperoni", "Piña", "Champiñones", "Bbq", "Buffalo", "Blue Cheese")
    val TOPPINGS_DULCES = listOf("Fresa", "Durazno", "Plátano", "Oreo", "Nuez", "Bombón", "Philadelphia", "Nutella", "Coco", "Chispas")

    val CONSUMIBLES_EMPAQUE = setOf("charola", "domo", "papel_hamburguesero", "vaso", "tenedor", "cuchara")

    fun resolverDescuentoOpcion(
        producto: com.bocatta.pos.domain.model.SalesInventoryProductV2,
        opcionNombre: String,
        esBase: Boolean = false
    ): Pair<String, Double>? {
        val opcionNorm = opcionNombre.normalizado()

        // 1. Intentar buscar en el mapeo dinámico del producto (configSchema)
        producto.configSchema.forEach { grupo ->
            val keyCoincidente = grupo.descuentosInsumo.keys.find { it.normalizado() == opcionNorm }
            if (keyCoincidente != null) {
                val desc = grupo.descuentosInsumo[keyCoincidente]
                if (desc != null && desc.insumoId.isNotBlank()) {
                    val cantidadBase = convertirAUnidadBase(desc.cantidad, desc.unidad)
                    return desc.insumoId to cantidadBase
                }
            }
        }

        // 2. Fallback: Si no tiene mapeo dinámico, usa la lógica estática ya programada
        return if (esBase) {
            mapearBaseAInsumo(opcionNombre) ?: mapearToppingOAderezoAInsumo(opcionNombre)
        } else {
            mapearToppingOAderezoAInsumo(opcionNombre) ?: mapearBaseAInsumo(opcionNombre)
        }
    }

    fun calcularParaItem(
        item: ItemCarritoV2,
        recetaIngredientes: List<IngredienteReceta>
    ): Map<String, Double> {
        // Si el producto se vende por peso, qty es la fraccion de kg vendida.
        val gramos = item.cantidadGramos
        val qty = if (gramos != null) gramos / 1000.0
                  else item.cantidad.toDouble()
        val deducciones = linkedMapOf<String, Double>()

        recetaIngredientes.forEach { ing ->
            deducciones.add(ing.insumoId, convertirAUnidadBase(ing.cantidad, ing.unidad) * qty)
        }

        item.base?.let { base ->
            base.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach { baseSingular ->
                resolverDescuentoOpcion(item.producto, baseSingular, esBase = true)?.let { (id, cantidad) ->
                    deducciones.add(id, cantidad * qty)
                }
            }
        }

        item.toppings.forEach { topping ->
            resolverDescuentoOpcion(item.producto, topping, esBase = false)?.let { (id, cantidad) ->
                deducciones.add(id, cantidad * qty)
            }
        }

        item.aderezos.forEach { aderezo ->
            resolverDescuentoOpcion(item.producto, aderezo, esBase = false)?.let { (id, cantidad) ->
                deducciones.add(id, cantidad * qty)
            }
        }

        item.componentesCombo.forEach { componente ->
            calcularParaItem(
                componente.copy(cantidad = componente.cantidad * item.cantidad),
                emptyList()
            ).forEach { (id, cantidad) -> deducciones.add(id, cantidad) }
        }

        item.producto.consumiblesAsociados.forEach { consumible ->
            if (item.paraLlevar || consumible.consumibleId !in CONSUMIBLES_EMPAQUE) {
                deducciones.add(consumible.consumibleId, consumible.cantidad * qty)
            }
        }

        if (item.paraLlevar) {
            val cat = item.producto.categoria.lowercase(Locale.ROOT)
            when (cat) {
                "crepas" -> {
                    deducciones.add("servilletas", 2.0 * qty)
                    deducciones.add("papel_hamburguesero", qty)
                }
                "waffles" -> {
                    deducciones.add("tenedor", qty)
                    deducciones.add("servilletas", 2.0 * qty)
                    deducciones.add("charola", qty)
                }
                "frappes", "bebida" -> {
                    deducciones.add("vaso", qty)
                    deducciones.add("domo", qty)
                }
                else -> {
                    deducciones.add("servilletas", qty)
                }
            }
        } else {
            deducciones.add("servilletas", qty)
        }

        if (item.paraLlevar && item.esSeparado && item.producto.esCombo) {
            deducciones.add("charola", qty)
            deducciones.add("papel_hamburguesero", qty)
        }

        return deducciones
    }

    fun calcularPrecioCrepa(precioBase: Double, base: String?, toppings: List<String>, extra: Double = 10.0): Double {
        return PricingEngine.calcularPrecioDirecto(
            precioBase = precioBase,
            categoria = "crepas",
            base = base,
            toppings = toppings,
            costoToppingExtra = extra
        )
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
            t.contains("pina") || t.contains("hawaiana") -> "pina_kg" to 40.0
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
        return when (unidad.lowercase(Locale.ROOT)) {
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
            .lowercase(Locale.ROOT)
    }
}
