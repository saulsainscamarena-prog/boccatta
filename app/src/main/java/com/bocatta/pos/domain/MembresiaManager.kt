package com.bocatta.pos.domain

object MembresiaManager {
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
            visitasActuales >= 50 -> "PLATINO"
            visitasActuales >= 20 -> "ORO"
            else -> "PLATA"
        }
        return EstadoMembresia(
            esElegiblePremio = elegible,
            visitasRestantesParaPremio = if (elegible) ciclo else restantes,
            nivel = nivel
        )
    }
}
