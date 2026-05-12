package com.bocatta.pos.core.util

import com.bocatta.pos.domain.model.ItemCarritoV2
import java.text.SimpleDateFormat
import java.util.*

object TicketGeneratorV2 {
    
    fun generarTextoTicket(
        ordenNum: Int,
        sucursal: String,
        items: List<ItemCarritoV2>,
        total: Double,
        descuento: Double,
        visitaNum: Int
    ): String {
        val localeMX = java.util.Locale.forLanguageTag("es-MX")
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", localeMX)
        val fecha = sdf.format(Date())

        val sb = StringBuilder()
        sb.append("✨ *BOCATTA POS* ✨\n")
        sb.append("------------------------------\n")
        sb.append("📍 Sucursal: ${sucursal.uppercase()}\n")
        sb.append("🛒 Orden: #$ordenNum\n")
        sb.append("🕒 Fecha: $fecha\n")
        sb.append("------------------------------\n\n")

        items.forEach { item ->
            sb.append("🍴 *${item.producto.nombre}*\n")
            if (item.toppings.isNotEmpty()) {
                sb.append("  └ Extras: ${item.toppings.joinToString()}\n")
            }
            if (item.nota.isNotEmpty()) {
                sb.append("  📝 Nota: ${item.nota}\n")
            }
            sb.append("  💰 $${String.format("%.2f", item.precioFinal)}\n\n")
        }

        sb.append("------------------------------\n")
        if (descuento > 0) {
            sb.append("🎁 Descuento Lealtad: -$${String.format("%.2f", descuento)}\n")
        }
        sb.append("💵 *TOTAL A PAGAR: $${String.format("%.2f", total)}*\n")
        sb.append("------------------------------\n\n")
        
        if (visitaNum > 0) {
            sb.append("💎 *Programa de Lealtad*\n")
            if (visitaNum == 5) {
                val promedio = total
                sb.append("🎉 ¡WOW! Completaste 5 visitas.\n")
                sb.append("🎁 En tu PRÓXIMA COMPRA tienes un\n")
                sb.append("DESCUENTO de: *$${String.format("%.2f", promedio)}*\n")
            } else {
                sb.append("¡Esta fue tu visita $visitaNum de 6! 🏃‍♂️\n")
            }
        } else if (descuento > 0) {
            sb.append("💎 *Programa de Lealtad*\n")
            sb.append("¡Felicidades! Disfrutaste tu regalo.\n")
            sb.append("Tu ciclo se ha reiniciado. 🥳\n")
        }
        
        sb.append("\n¡Gracias por tu preferencia! 🙏")
        
        return sb.toString()
    }
}
