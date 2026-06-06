package com.bocatta.pos.presentation.ui.components

import java.text.Normalizer
import java.util.Locale

object SearchHelper {

    /**
     * Calcula la distancia Levenshtein entre dos cadenas de texto.
     * Representa la cantidad mínima de operaciones (inserción, eliminación o sustitución)
     * requeridas para transformar s1 en s2.
     */
    fun calcularDistanciaLevenshtein(s1: String, s2: String): Int {
        val len1 = s1.length
        val len2 = s2.length
        val dp = Array(len1 + 1) { IntArray(len2 + 1) }

        for (i in 0..len1) dp[i][0] = i
        for (j in 0..len2) dp[0][j] = j

        for (i in 1..len1) {
            for (j in 1..len2) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,       // Eliminación
                    dp[i][j - 1] + 1,       // Inserción
                    dp[i - 1][j - 1] + cost  // Sustitución
                )
            }
        }
        return dp[len1][len2]
    }

    /**
     * Quita acentos, convierte a minúsculas y limpia espacios en blanco.
     */
    fun normalizar(text: String): String {
        return Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}".toRegex(), "")
            .lowercase(Locale.ROOT)
            .trim()
    }

    /**
     * Calcula un puntaje de relevancia (score) para ordenar coincidencias estilo WhatsApp:
     * - Coincidencia exacta: 100 puntos.
     * - Comienza con (prefijo): 80 puntos.
     * - Contiene la palabra (subcadena): 50 puntos.
     * - Similitud difusa (Levenshtein <= 2 para palabras de más de 3 letras): 30 puntos.
     * - Sin similitud: 0 puntos.
     */
    fun calcularScoreRelevancia(query: String, text: String): Int {
        val q = normalizar(query)
        val t = normalizar(text)

        if (q.isBlank() || t.isBlank()) return 0
        if (q == t) return 100
        if (t.startsWith(q)) return 80
        if (t.contains(q)) return 50

        // Búsqueda difusa Levenshtein (tolerancia a errores ortográficos de dedo)
        if (q.length > 3) {
            val dist = calcularDistanciaLevenshtein(q, t)
            // Si la distancia es pequeña y la diferencia de longitud es menor a 3
            if (dist <= 2 && Math.abs(q.length - t.length) <= 2) {
                return 30
            }
        }

        return 0
    }

    /**
     * Determina si una consulta y un texto son similares.
     */
    fun sonSimilares(query: String, text: String): Boolean {
        return calcularScoreRelevancia(query, text) > 0
    }
}
