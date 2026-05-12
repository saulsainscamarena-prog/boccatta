package com.bocatta.pos.domain.model

import kotlin.math.roundToInt

sealed class UnidadMedida(val codigo: String, val nombre: String, val factorABase: Double) {
    // --- BASE (Métricas) ---
    object Gramo : UnidadMedida("g", "Gramos", 1.0)
    object Mililitro : UnidadMedida("ml", "Mililitros", 1.0)
    object Pieza : UnidadMedida("pz", "Piezas", 1.0)

    // --- DERIVADAS (Métricas) ---
    object Kilo : UnidadMedida("kg", "Kilogramos", 1000.0)
    object Litro : UnidadMedida("L", "Litros", 1000.0)

    // --- VOLUMEN CULINARIO ---
    object Taza : UnidadMedida("tz", "Tazas", 240.0) // 1 tz = 240 ml
    object Cucharada : UnidadMedida("cd", "Cucharadas", 15.0) // 1 cd = 15 ml

    // --- EMPAQUE / COMERCIALES ---
    object Paquete : UnidadMedida("pkg", "Paquete", 1.0) // Factor dinámico
    object Caja : UnidadMedida("box", "Caja", 1.0) // Factor dinámico

    companion object {
        fun fromCodigo(codigo: String): UnidadMedida {
            return when (codigo) {
                "g" -> Gramo
                "ml" -> Mililitro
                "pz" -> Pieza
                "kg" -> Kilo
                "L" -> Litro
                "tz" -> Taza
                "cd" -> Cucharada
                "pkg" -> Paquete
                "box" -> Caja
                else -> Gramo
            }
        }
    }
}

data class ConversionUnidad(
    val unidadOrigen: String, // Código de UnidadMedida
    val unidadDestino: String, // Código de UnidadMedida
    val factor: Double // Cuántas unidades destino hay en una origen
) {
    companion object {
        // Helper para convertir cantidades
        fun convertir(cantidad: Double, desde: UnidadMedida, hacia: UnidadMedida, conversiones: List<ConversionUnidad>): Double {
            if (desde.codigo == hacia.codigo) return cantidad

            // Convertir a base métrica primero
            val cantidadEnBase = cantidad * desde.factorABase

            // Si la destino es base métrica, dividir por su factor
            if (hacia.factorABase > 0 && hacia != UnidadMedida.Pieza) {
                return cantidadEnBase / hacia.factorABase
            }

            // Buscar conversión específica (ej. Caja -> Paquete)
            val conv = conversiones.find { it.unidadOrigen == desde.codigo && it.unidadDestino == hacia.codigo }
            return if (conv != null) {
                cantidad * conv.factor
            } else {
                cantidadEnBase / hacia.factorABase
            }
        }
    }
}

