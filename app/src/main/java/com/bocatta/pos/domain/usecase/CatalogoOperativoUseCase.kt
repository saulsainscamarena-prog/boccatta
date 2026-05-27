package com.bocatta.pos.domain.usecase

import com.bocatta.pos.domain.model.SalesInventoryProductV2

/**
 * Caso de Uso responsable de procesar el catalogo de productos en memoria aplicando las reglas de
 * negocio operativa de Bocatta POS para el mostrador.
 */
interface CatalogoOperativoUseCase {
    /**
     * Clasifica y ordena los productos basandose en las reglas de negocio, el historial y la sucursal activa.
     *
     * @param menuOriginal Lista maestra de productos recuperada desde la base de datos o red.
     * @param ventasHistorial Mapa con el historial de ventas por producto (ID_Producto -> Cantidad Vendida).
     * @param sucursal Nombre/ID de la sucursal activa.
     */
    fun clasificarYOrdenar(
        menuOriginal: List<SalesInventoryProductV2>,
        ventasHistorial: Map<String, Int>,
        sucursal: String
    ): CatalogoProcesado
}
