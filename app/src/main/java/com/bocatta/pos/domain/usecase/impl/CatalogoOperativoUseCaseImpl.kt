package com.bocatta.pos.domain.usecase.impl

import com.bocatta.pos.domain.model.SalesInventoryProductV2
import com.bocatta.pos.domain.usecase.CatalogoOperativoUseCase
import com.bocatta.pos.domain.usecase.CatalogoProcesado
import java.util.Locale

class CatalogoOperativoUseCaseImpl : CatalogoOperativoUseCase {

    override fun clasificarYOrdenar(
        menuOriginal: List<SalesInventoryProductV2>,
        ventasHistorial: Map<String, Int>,
        sucursal: String
    ): CatalogoProcesado {
        val sucursalKey = sucursal.lowercase(Locale.ROOT).trim()

        // 1. Reglas de filtrado y clasificacion usando campos reales y normalizados
        val (vendibles, excluidos) = menuOriginal.partition { prod ->
            prod.activo &&
            prod.tipoProducto.trim().uppercase(Locale.ROOT) != "MATERIA_PRIMA" &&
            !prod.esProductoTopping &&
            prod.precioVenta.containsKey(sucursalKey) &&
            (prod.precioVenta[sucursalKey] ?: 0.0) > 0.0
        }

        // 2. Frecuentes: todos los vendibles ordenados por historial y nombre
        val frecuentes = vendibles.sortedWith(
            compareByDescending<SalesInventoryProductV2> { ventasHistorial[it.id] ?: 0 }
                .thenBy { it.nombre.lowercase(Locale.ROOT) }
        )

        // 3. Categorias visibles
        val categoriasVisibles = vendibles
            .map { it.categoria.trim().uppercase(Locale.ROOT) }
            .filter { it.isNotEmpty() }
            .distinct()
            .sorted()

        return CatalogoProcesado(
            vendibles = vendibles,
            frecuentes = frecuentes,
            categoriasVisibles = categoriasVisibles,
            excluidos = excluidos
        )
    }
}
