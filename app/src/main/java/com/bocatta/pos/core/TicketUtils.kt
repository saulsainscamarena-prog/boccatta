package com.bocatta.pos.core

import com.bocatta.pos.core.constants.SucursalConfig
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TicketUtils {
    fun generarCodigoTicket(sucursal: String, numero: Long): String {
        val prefijo = SucursalConfig.prefijoPorSucursal(sucursal)
        val fecha = SimpleDateFormat("ddMM", Locale.forLanguageTag("es-MX")).format(Date())
        return "$prefijo$fecha-${numero.toString().padStart(3, '0')}"
    }
}
