package com.bocatta.pos.presentation.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush

object BocattaDesign {

    /**
     * Devuelve el color corporativo asociado a cada categoria de menu.
     */
    fun getColorPorCategoria(categoria: String?): Color {
        return when (categoria?.lowercase()) {
            "crepas", "crepa" -> Color(0xFFFF2D55)
            "snacks", "snack" -> Color(0xFFFF5E3A)
            "postres", "postre" -> Color(0xFFBF5AF2)
            "combos", "combo", "paquetes" -> Color(0xFF0A84FF)
            "bebidas", "bebida", "frappes" -> Color(0xFF64D2FF)
            else -> Color(0xFF8E8E93)
        }
    }

    /**
     * Genera un gradiente suave basado en un color base para efectos premium.
     */
    fun getGradient(baseColor: Color): Brush {
        return Brush.verticalGradient(
            colors = listOf(baseColor.copy(alpha = 0.8f), baseColor)
        )
    }

    /**
     * Genera un color de alerta basado en el nivel de stock.
     */
    fun getStockColor(actual: Double, critico: Double): Color {
        return when {
            actual <= 0 -> Color(0xFFE53935)
            actual <= critico -> Color(0xFFFB8C00)
            else -> Color(0xFF43A047)
        }
    }
}
