package com.bocatta.pos.presentation.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush

object BocattaDesign {
    
    /**
     * Devuelve el color corporativo asociado a cada categor�a de men�.
     */
    fun getColorPorCategoria(categoria: String?): Color {
        return when (categoria?.lowercase()) {
            "crepas", "crepa" -> CatCrepa
            "snacks", "snack" -> CatSnack
            "postres", "postre" -> CatPostre
            "combos", "combo", "paquetes" -> CatCombo
            "bebidas", "bebida", "frappes" -> CatBebida
            else -> CatDefault
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
            actual <= 0 -> BocattaDanger
            actual <= critico -> BocattaWarning
            else -> BocattaSuccess
        }
    }
}

