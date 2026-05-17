package com.bocatta.pos.domain

import com.bocatta.pos.domain.model.SalesInventoryProductV2
import com.bocatta.pos.domain.model.RecetaV2

object StockManager {
    sealed class StockResult {
        object Suficiente : StockResult()
        data class Insuficiente(val faltante: Double, val insumo: String) : StockResult()
    }

    fun validarStockParaVenta(
        producto: SalesInventoryProductV2, 
        cantidad: Int, 
        receta: RecetaV2?, 
        stockActualSucursal: Map<String, Double>
    ): StockResult {
        if (receta == null) return StockResult.Suficiente // Si no hay receta, asumimos stock infinito o gestionado externo

        // Validar cada ingrediente de la receta
        for (ingrediente in receta.ingredientes) {
            val cantidadRequerida = ingrediente.cantidad * cantidad
            val stockDisponible = stockActualSucursal[ingrediente.insumoId] ?: 0.0
            
            if (stockDisponible < cantidadRequerida) {
                return StockResult.Insuficiente(
                    faltante = cantidadRequerida - stockDisponible,
                    insumo = ingrediente.nombreInsumo ?: ingrediente.insumoId
                )
            }
        }
        
        return StockResult.Suficiente
    }
}

