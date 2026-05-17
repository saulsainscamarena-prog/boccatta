package com.bocatta.pos.domain.usecase

import com.bocatta.pos.domain.model.*

class PromocionesEngine {
    fun calcular(
        carrito: List<ItemCarritoV2>,
        promociones: List<PromocionUniversal>
    ): Double {
        var totalDesc = 0.0
        val subtotal = carrito.sumOf { it.precioFinal.toDouble() * it.cantidad }

        for (promo in promociones) {
            if (!promo.activa) continue
            if (subtotal < promo.montoMinimoTicket) continue
            val cantidadTotalItems = carrito.sumOf { it.cantidad }
            if (cantidadTotalItems < promo.cantidadMinimaItems) continue

            var descAplicable = 0.0

            when (promo.alcance) {
                AlcancePromo.TICKET_COMPLETO -> {
                    if (promo.tipoDescuento == TipoDescuentoPromo.PORCENTAJE) {
                        descAplicable = subtotal * (promo.valorDescuento / 100.0)
                    } else if (promo.tipoDescuento == TipoDescuentoPromo.MONTO_FIJO_TICKET) {
                        descAplicable = promo.valorDescuento
                    }
                }
                AlcancePromo.PRODUCTOS_ESPECIFICOS -> {
                    val itemsValidos = carrito.filter { it.producto.id in promo.itemsIncluidosIds }
                    val subtotalValidos = itemsValidos.sumOf { it.precioFinal.toDouble() * it.cantidad }
                    if (promo.tipoDescuento == TipoDescuentoPromo.PORCENTAJE) {
                        descAplicable = subtotalValidos * (promo.valorDescuento / 100.0)
                    } else if (promo.tipoDescuento == TipoDescuentoPromo.MONTO_FIJO_ITEM) {
                        descAplicable = promo.valorDescuento * itemsValidos.sumOf { it.cantidad }
                    } else if (promo.tipoDescuento == TipoDescuentoPromo.PRECIO_FIJO_ITEM) {
                        val dif = itemsValidos.sumOf { (it.precioFinal.toDouble() - promo.valorDescuento) * it.cantidad }
                        if (dif > 0) descAplicable = dif
                    }
                }
                AlcancePromo.CATEGORIAS_ESPECIFICAS -> {
                    val itemsValidos = carrito.filter { it.producto.categoria in promo.itemsIncluidosIds }
                    val subtotalValidos = itemsValidos.sumOf { it.precioFinal.toDouble() * it.cantidad }
                    if (promo.tipoDescuento == TipoDescuentoPromo.PORCENTAJE) {
                        descAplicable = subtotalValidos * (promo.valorDescuento / 100.0)
                    } else if (promo.tipoDescuento == TipoDescuentoPromo.MONTO_FIJO_ITEM) {
                        descAplicable = promo.valorDescuento * itemsValidos.sumOf { it.cantidad }
                    }
                }
            }

            if (promo.descuentoMaximoTicket != null && promo.descuentoMaximoTicket > 0) {
                if (descAplicable > promo.descuentoMaximoTicket) descAplicable = promo.descuentoMaximoTicket
            }

            totalDesc += descAplicable
            if (!promo.combinable) break
        }
        return totalDesc
    }
}

