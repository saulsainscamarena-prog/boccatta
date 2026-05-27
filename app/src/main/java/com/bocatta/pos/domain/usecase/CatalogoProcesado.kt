package com.bocatta.pos.domain.usecase

import com.bocatta.pos.domain.model.SalesInventoryProductV2

/**
 * Representa la estructura de datos procesada lista para ser renderizada en la UI de ventas.
 */
data class CatalogoProcesado(
    val vendibles: List<SalesInventoryProductV2>,      // Todos los productos activos aptos para venta directa en la sucursal
    val frecuentes: List<SalesInventoryProductV2>,     // Todos los productos vendibles ordenados por historial de ventas y alfabeticamente
    val categoriasVisibles: List<String>,              // Lista ordenada de nombres de categorias que contienen productos vendibles
    val excluidos: List<SalesInventoryProductV2>       // Productos filtrados (materia prima, toppings sueltos, inactivos o sin precio)
)
