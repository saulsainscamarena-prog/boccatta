package com.bocatta.pos.domain.engine

import com.bocatta.pos.domain.model.SalesInventoryProductV2

data class SugerenciaPromocion(
    val tipo: String = "",
    val titulo: String = "",
    val descripcion: String = "",
    val sugerencia: String = "",
    val productosSugeridos: List<String> = emptyList(),
    val precioSugerido: Double? = null,
    val margenEstimado: Double = 0.0,
    val prioridad: Int = 0
)

object SuggestionEngine {

    fun generarSugerencias(
        productos: List<SalesInventoryProductV2>,
        alertasStock: Map<String, Double>,
        ventasPorProducto: Map<String, Int>,
        ventasPorHora: Map<Int, Int>,
        costosCalculados: Map<String, CostoProducto>,
        topVentas: List<String>
    ): List<SugerenciaPromocion> {
        val sugerencias = mutableListOf<SugerenciaPromocion>()

        alertasStock.entries.sortedBy { it.value }.take(3).forEach { (id, stock) ->
            val prod = productos.find { it.id == id } ?: return@forEach
            val costo = costosCalculados[id]
            if (stock <= 5 && stock > 0) {
                sugerencias.add(SugerenciaPromocion(
                    tipo = "MERMA",
                    titulo = "Producto próximo a agotarse",
                    descripcion = "${prod.nombre}: solo $stock unidades.",
                    sugerencia = "Ofrece ${prod.nombre} con descuento del 20%.",
                    productosSugeridos = listOf(id),
                    precioSugerido = costo?.precioVenta?.let { it * 0.8 },
                    prioridad = 1
                ))
            }
        }

        val ventaPromedio = if (ventasPorProducto.isNotEmpty()) ventasPorProducto.values.average() else 0.0
        ventasPorProducto.entries.filter { it.value < ventaPromedio * 0.3 }.take(3).forEach { (id, _) ->
            val prod = productos.find { it.id == id } ?: return@forEach
            sugerencias.add(SugerenciaPromocion(
                tipo = "BAJA_DEMANDA",
                titulo = "Baja demanda detectada",
                descripcion = "${prod.nombre}: ${ventasPorProducto[id]} ventas (promedio: ${"%.0f".format(ventaPromedio)}).",
                sugerencia = "Crea un combo con ${prod.nombre}.",
                productosSugeridos = listOf(id),
                prioridad = 2
            ))
        }

        if (topVentas.size >= 2) {
            val prod1 = productos.find { it.id == topVentas[0] }
            val prod2 = productos.find { it.id == topVentas[1] }
            if (prod1 != null && prod2 != null) {
                sugerencias.add(SugerenciaPromocion(
                    tipo = "CROSS_SELLING",
                    titulo = "Oportunidad de venta cruzada",
                    descripcion = "Clientes que compran ${prod1.nombre} NO suelen comprar ${prod2.nombre}.",
                    sugerencia = "Combo: ${prod1.nombre} + ${prod2.nombre} con 15% descuento.",
                    productosSugeridos = listOf(topVentas[0], topVentas[1]),
                    prioridad = 3
                ))
            }
        }

        val horaActual = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val ventasHoraActual = ventasPorHora[horaActual] ?: 0
        if (ventasHoraActual < 5) {
            sugerencias.add(SugerenciaPromocion(
                tipo = "HORA_MUERTA",
                titulo = "Hora de baja actividad",
                descripcion = "Solo $ventasHoraActual ventas en esta hora.",
                sugerencia = "Activa promoción flash 2x1.",
                prioridad = 4
            ))
        }

        return sugerencias.sortedBy { it.prioridad }
    }
}

