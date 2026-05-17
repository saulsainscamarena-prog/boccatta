package com.bocatta.pos.domain.util

import com.bocatta.pos.domain.model.SalesInventoryProductV2

object ProductoValidator {
    fun esValido(producto: SalesInventoryProductV2): Boolean {
        return producto.nombre.isNotBlank() &&
                producto.precioVenta.values.any { it >= 0 } &&
                producto.categoria.isNotBlank() &&
                (!producto.esCombo || producto.productosCombo.isNotEmpty()) &&
                producto.consumiblesAsociados.all { it.cantidad > 0 }
    }
}
