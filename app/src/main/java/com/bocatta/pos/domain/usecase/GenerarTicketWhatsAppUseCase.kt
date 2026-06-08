package com.bocatta.pos.domain.usecase

import com.bocatta.pos.domain.model.ItemCarritoV2
import com.bocatta.pos.domain.model.MetodoPago
import java.math.BigDecimal

class GenerarTicketWhatsAppUseCase {
    operator fun invoke(
        sucursal: String,
        items: List<ItemCarritoV2>,
        codigoTicket: String,
        total: Double,
        descuentoLealtad: Double,
        descuentoPromociones: Double,
        metodoPago: String,
        descuentoManual: Double = 0.0,
        propina: Double = 0.0,
        notaOrden: String = "",
        modoOperacion: String = ""
    ): String {
        val localeMX = java.util.Locale.forLanguageTag("es-MX")
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", localeMX)
        val sb = StringBuilder()
        sb.append("*BOCATTA - TICKET DIGITAL*\n")
        sb.append("Ticket: #$codigoTicket\n")
        sb.append("Fecha: ${sdf.format(java.util.Date())}\n")
        sb.append("Sucursal: ${sucursal.uppercase(java.util.Locale.getDefault())}\n")
        if (modoOperacion.isNotBlank()) {
            sb.append("Modo: ${modoOperacion.trim()}\n")
        }
        sb.append("---------------------\n")

        items.forEach { item ->
            sb.append("${item.cantidad}x ${item.nombre}\n")
            if (item.nota.isNotBlank()) sb.append("  Nota item: ${item.nota}\n")
            sb.append("  $${"%.2f".format(item.precioFinal.multiply(BigDecimal(item.cantidad)).toDouble())}\n")
        }

        sb.append("---------------------\n")
        if (descuentoLealtad > 0) {
            sb.append("Desc. lealtad: -$${"%.2f".format(descuentoLealtad)}\n")
        }
        if (descuentoPromociones > 0) {
            sb.append("Promo aplicada: -$${"%.2f".format(descuentoPromociones)}\n")
        }
        if (descuentoManual > 0) {
            sb.append("Desc. manual: -$${"%.2f".format(descuentoManual)}\n")
        }
        if (propina > 0) {
            sb.append("Propina: $${"%.2f".format(propina)}\n")
        }
        sb.append("*TOTAL: $${"%.2f".format(total)}*\n")
        sb.append("Pago: $metodoPago\n")
        if (notaOrden.isNotBlank()) {
            sb.append("Nota orden: ${notaOrden.trim()}\n")
        }
        sb.append("---------------------\n")
        sb.append("_Gracias por ser parte de la familia Bocatta!_")

        return sb.toString()
    }

    operator fun invoke(
        sucursal: String,
        items: List<ItemCarritoV2>,
        codigoTicket: String,
        total: Double,
        descuentoLealtad: Double,
        descuentoPromociones: Double,
        metodoPago: MetodoPago,
        descuentoManual: Double = 0.0,
        propina: Double = 0.0,
        notaOrden: String = "",
        modoOperacion: String = ""
    ): String {
        return invoke(
            sucursal = sucursal,
            items = items,
            codigoTicket = codigoTicket,
            total = total,
            descuentoLealtad = descuentoLealtad,
            descuentoPromociones = descuentoPromociones,
            descuentoManual = descuentoManual,
            metodoPago = metodoPago.valor,
            propina = propina,
            notaOrden = notaOrden,
            modoOperacion = modoOperacion
        )
    }
}
