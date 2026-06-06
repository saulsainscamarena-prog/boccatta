package com.bocatta.pos.presentation.utils

import java.util.Calendar

object BocattaUtilsV2 {
    /**
     * Determina la sucursal sugerida según el día de la semana.
     * Atlixco: Lunes a Viernes
     * Metepec: Sábados y Domingos
     */
    fun sucursalSugeridaPorDia(): String {
        val dia = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        return if (dia in Calendar.MONDAY..Calendar.FRIDAY) "Atlixco" else "Metepec"
    }

    /**
     * Formatea timestamp a fecha legible local.
     */
    fun formatFecha(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.forLanguageTag("es-MX"))
        return sdf.format(java.util.Date(timestamp))
    }
}
