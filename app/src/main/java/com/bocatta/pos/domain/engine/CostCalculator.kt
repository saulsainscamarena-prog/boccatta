package com.bocatta.pos.domain.engine

import com.bocatta.pos.domain.model.RecetaV2
import com.bocatta.pos.domain.model.SalesInventoryProductV2
import com.bocatta.pos.domain.model.ItemCombo

data class CostoProducto(
    val productoId: String = "",
    val nombre: String = "",
    val costoIngredientes: Double = 0.0,
    val costoConsumibles: Double = 0.0,
    val costoGastoOperativo: Double = 0.0,
    val costoTotal: Double = 0.0,
    val precioVenta: Double = 0.0,
    val margen: Double = 0.0,
    val margenPorcentaje: Double = 0.0
)

object CostCalculator {

    fun calcularCostoProducto(
        producto: SalesInventoryProductV2,
        receta: RecetaV2?,
        costoInsumos: Map<String, Double>,
        costoConsumiblesPorUnidad: Map<String, Double>,
        gastoOperativoPorVenta: Double
    ): CostoProducto {
        val precio = producto.precioVenta.values.firstOrNull() ?: 0.0

        val costoIngredientes = receta?.ingredientes?.sumOf { ing ->
            val costoUnitario = costoInsumos[ing.insumoId] ?: 0.0
            val factor = when {
                ing.unidad == "g" || ing.unidad == "ml" -> 1.0 // ya está la cantidad en gramos/ml
                else -> 1.0
            }
            ing.cantidad * (costoUnitario / 1000.0) * factor
        } ?: 0.0

        val costoConsumibles = producto.consumiblesAsociados.sumOf { c ->
            (costoConsumiblesPorUnidad[c.consumibleId] ?: 0.0) * c.cantidad
        }

        val costoCostoOperativo = gastoOperativoPorVenta
        val total = costoIngredientes + costoConsumibles + costoCostoOperativo

        return CostoProducto(
            productoId = producto.id,
            nombre = producto.nombre,
            costoIngredientes = costoIngredientes,
            costoConsumibles = costoConsumibles,
            costoGastoOperativo = costoCostoOperativo,
            costoTotal = total,
            precioVenta = precio,
            margen = precio - total,
            margenPorcentaje = if (precio > 0) ((precio - total) / precio * 100) else 0.0
        )
    }

    fun calcularCostoCombo(
        precioCombo: Double,
        costosProductos: List<CostoProducto>,
        items: List<ItemCombo>
    ): CostoProducto {
        val costoTotal = items.sumOf { item ->
            costosProductos.find { it.productoId == item.productoId }?.costoTotal?.times(item.cantidad) ?: 0.0
        }
        return CostoProducto(
            nombre = "Combo",
            costoTotal = costoTotal,
            precioVenta = precioCombo,
            margen = precioCombo - costoTotal,
            margenPorcentaje = if (precioCombo > 0) ((precioCombo - costoTotal) / precioCombo * 100) else 0.0
        )
    }

    fun sugerirPrecioRentable(costoTotal: Double, margenDeseado: Double = 30.0): Double {
        return costoTotal / (1 - margenDeseado / 100.0)
    }
}
