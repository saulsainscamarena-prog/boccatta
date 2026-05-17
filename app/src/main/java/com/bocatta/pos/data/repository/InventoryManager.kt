package com.bocatta.pos.data.repository

import com.bocatta.pos.domain.model.*
import java.lang.IllegalArgumentException

/**
 * Motor de Deducción de Inventario.
 * Gestiona la lógica de restar stock basado en recetas y ventas.
 */
class InventoryManager {

    /**
     * Calcula la lista de ingredientes a descontar basado en un producto vendido.
     * @return Lista de Pares (InsumoId, Cantidad a descontar en unidad base)
     */
    fun calcularDeducciones(
        producto: SalesInventoryProductV2,
        receta: RecetaV2?,
        cantidadVendida: Int = 1
    ): List<Pair<String, Double>> {
        val deducciones = mutableListOf<Pair<String, Double>>()

        if (receta == null) return deducciones

        for (ingrediente in receta.ingredientes) {
            val cantidadBase = convertirAUnidadBase(ingrediente.cantidad, ingrediente.unidad)
            deducciones.add(Pair(ingrediente.insumoId, cantidadBase * cantidadVendida))
        }

        return deducciones
    }

    /**
     * Calcula el descuento de consumibles asociados al producto.
     */
    fun calcularConsumibles(
        producto: SalesInventoryProductV2,
        cantidadVendida: Int = 1
    ): List<Pair<String, Double>> {
        return producto.consumiblesAsociados.map { req ->
            // Asumimos que los consumibles están en unidad "pz" (piezas)
            Pair(req.consumibleId, req.cantidad * cantidadVendida)
        }
    }

    /**
     * Convierte cualquier unidad a su equivalente en la unidad base del insumo (g, ml, pz).
     * Ej: 1 "kg" -> 1000 "g"
     */
    private fun convertirAUnidadBase(cantidad: Double, unidadOrigen: String): Double {
        return when (unidadOrigen) {
            "kg" -> cantidad * 1000.0 // kg -> g
            "L" -> cantidad * 1000.0  // L -> ml
            "tz" -> cantidad * 240.0   // Taza -> ml
            "cd" -> cantidad * 15.0    // Cucharada -> ml
            else -> cantidad // "g", "ml", "pz" ya son base
        }
    }

    /**
     * Valida si hay stock suficiente para una venta.
     * @return True si hay stock, False si falta alguno.
     */
    fun validarStockDisponible(
        insumosActuales: Map<String, Double>, // Map<InsumoId, StockEnBase>
        deducciones: List<Pair<String, Double>>
    ): Boolean {
        for ((insumoId, cantidadRequerida) in deducciones) {
            val stockActual = insumosActuales[insumoId] ?: 0.0
            if (stockActual < cantidadRequerida) {
                return false
            }
        }
        return true
    }

    /**
     * Calcula el costo de producción de una receta.
     */
    fun calcularCostoReceta(
        receta: RecetaV2,
        insumos: Map<String, InsumoV2>
    ): Double {
        var costoTotal = 0.0
        for (ingrediente in receta.ingredientes) {
            val insumo = insumos[ingrediente.insumoId]
            if (insumo != null) {
                val cantidadBase = convertirAUnidadBase(ingrediente.cantidad, ingrediente.unidad)
                costoTotal += (cantidadBase * insumo.costoUnitarioBase)
            }
        }
        return costoTotal
    }
}


