package com.bocatta.pos.data.repository

import com.bocatta.pos.domain.model.HorarioEmpleado
import com.bocatta.pos.domain.model.JornadaLaboral
import com.bocatta.pos.domain.repository.IHorarioRepository
import com.bocatta.pos.network.firebase.FirebaseFirestoreProvider
import kotlinx.coroutines.tasks.await
import timber.log.Timber

class HorarioRepository : IHorarioRepository {
    private val horariosCol = FirebaseFirestoreProvider.db.collection("v2_horarios")
    private val jornadasCol = FirebaseFirestoreProvider.db.collection("v2_jornadas")

    override suspend fun getHorarios(empleadoId: String): List<HorarioEmpleado> = try {
        horariosCol.whereEqualTo("empleadoId", empleadoId)
            .orderBy("diaSemana").get().await().documents.mapNotNull {
                it.toObject(HorarioEmpleado::class.java)?.copy(id = it.id)
            }
    } catch (e: Exception) { Timber.e(e, "Error getHorarios"); emptyList() }

    override suspend fun guardarHorario(horario: HorarioEmpleado): Boolean = try {
        val doc = if (horario.id.isBlank()) horariosCol.document() else horariosCol.document(horario.id)
        doc.set(horario.copy(id = doc.id)).await(); true
    } catch (e: Exception) { Timber.e(e, "Error guardarHorario"); false }

    override suspend fun iniciarJornada(jornada: JornadaLaboral): Boolean = try {
        val doc = jornadasCol.document()
        doc.set(jornada.copy(id = doc.id)).await(); true
    } catch (e: Exception) { Timber.e(e, "Error iniciarJornada"); false }

    override suspend fun cerrarJornada(jornadaId: String, fin: Long, horas: Double): Boolean = try {
        jornadasCol.document(jornadaId).update(
            "finReal", fin,
            "horasEfectivas", horas,
            "estado", "CERRADA"
        ).await(); true
    } catch (e: Exception) { Timber.e(e, "Error cerrarJornada"); false }

    override suspend fun getJornadaActiva(empleadoId: String): JornadaLaboral? = try {
        val snap = jornadasCol.whereEqualTo("empleadoId", empleadoId)
            .whereIn("estado", listOf("ACTIVA", "EN_PAUSA"))
            .limit(1).get().await()
        snap.documents.firstOrNull()?.toObject(JornadaLaboral::class.java)?.copy(id = snap.documents.first().id)
    } catch (e: Exception) { Timber.e(e, "Error getJornadaActiva"); null }
}
