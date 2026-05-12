package com.bocatta.pos.data.repository

import com.bocatta.pos.domain.model.*
import timber.log.Timber

/**
 * Gestión de Producción (Yield Management).
 * Permite registrar tandas de producción (masa, postres) y actualizar inventarios.
 */
class ProductionManager(private val inventoryRepository: InventoryRepository) {

    /**
     * Registra una tanda de producción.
     */
    fun registrarTandaProduccion(
        recetaProduccionId: String,
        cantidadProducida: Double,
        cantidadEsperada: Double = 0.0
    ): Boolean {
        val ingredientes = inventoryRepository.obtenerIngredientesReceta(recetaProduccionId)
        if (ingredientes.isEmpty()) return false
        
        return try {
            val rendimiento = if (cantidadEsperada > 0.0) cantidadEsperada else cantidadProducida.coerceAtLeast(1.0)
            ingredientes.forEach { ingrediente ->
                val cantidadRequerida = ingrediente.cantidad * (cantidadProducida / rendimiento)
                inventoryRepository.deductStock(ingrediente.insumoId, cantidadRequerida)
            }
            
            inventoryRepository.addStock(recetaProduccionId, cantidadProducida)
            true
        } catch (e: Exception) {
            Timber.tag("PRODUCTION").e(e, "Error al registrar tanda de producción")
            false
        }
    }

    fun registrarProduccionPostre(recetaProduccionId: String, porcionesProducidas: Int): Boolean {
        return registrarTandaProduccion(recetaProduccionId, porcionesProducidas.toDouble(), porcionesProducidas.toDouble())
    }
}

