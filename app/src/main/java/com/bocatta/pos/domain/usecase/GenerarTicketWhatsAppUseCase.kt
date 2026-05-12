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
        metodoPago: MetodoPago
    ): String {
        val localeMX = java.util.Locale.forLanguageTag("es-MX")
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", localeMX)
        val sb = StringBuilder()
        sb.append("🍩 *BOCATTA - TICKET DIGITAL* 🍩\n")
        sb.append(" Ticket: #$codigoTicket\n")
        sb.append(" Fecha: ${sdf.format(java.util.Date())}\n")
        sb.append(" Sucursal: ${sucursal.uppercase()}\n")
        sb.append("─────────────────────\n")
        
        items.forEach { item ->
            sb.append("• ${item.cantidad}x ${item.nombre}\n")
            if (item.nota.isNotBlank()) sb.append("  ↳ _${item.nota}_\n")
            sb.append("  *$${"%.2f".format(item.precioFinal.multiply(BigDecimal(item.cantidad)).toDouble())}*\n")
        }
        
        sb.append("─────────────────────\n")
        if (descuentoLealtad > 0) {
            sb.append("🎁 Desc. Lealtad: -$${"%.2f".format(descuentoLealtad)}\n")
        }
        if (descuentoPromociones > 0) {
            sb.append("🏷️ Promo Aplicada: -$${"%.2f".format(descuentoPromociones)}\n")
        }
        sb.append("*TOTAL: $${"%.2f".format(total)}*\n")
        sb.append(" Pago: ${metodoPago.valor}\n")
        sb.append("─────────────────────\n")
        sb.append("_¡Gracias por ser parte de la familia Bocatta!_")
        
        return sb.toString()
    }
}
