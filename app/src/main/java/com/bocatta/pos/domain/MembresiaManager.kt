package com.bocatta.pos.domain

object MembresiaManager {
    const val UMBRAL_PLATINO = 50
    const val UMBRAL_ORO = 20
    data class EstadoMembresia(
        val esElegiblePremio: Boolean,
        val visitasRestantesParaPremio: Int,
        val nivel: String
    )

    fun verificarEstadoMembresia(visitasActuales: Int): EstadoMembresia {
        val ciclo = com.bocatta.pos.core.constants.FirestoreCollections.MEMBRESIA_CICLO_VISITAS
        val restantes = ciclo - (visitasActuales % ciclo)
        val elegible = visitasActuales > 0 && visitasActuales % ciclo == 0
        val nivel = when {
            visitasActuales >= UMBRAL_PLATINO -> "PLATINO"
            visitasActuales >= UMBRAL_ORO -> "ORO"
            else -> "PLATA"
        }
        return EstadoMembresia(
            esElegiblePremio = elegible,
            visitasRestantesParaPremio = if (elegible) ciclo else restantes,
            nivel = nivel
        )
    }
}
