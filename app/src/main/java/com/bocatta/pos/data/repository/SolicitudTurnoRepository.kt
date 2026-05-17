package com.bocatta.pos.data.repository

import com.bocatta.pos.domain.model.SolicitudTurnoExtra
import com.bocatta.pos.domain.repository.ISolicitudTurnoRepository
import com.bocatta.pos.network.firebase.FirebaseFirestoreProvider
import kotlinx.coroutines.tasks.await
import timber.log.Timber

class SolicitudTurnoRepository : ISolicitudTurnoRepository {
    private val col = FirebaseFirestoreProvider.db.collection("v2_solicitudes_turno")

    override suspend fun getAll(): List<SolicitudTurnoExtra> = try {
        col.orderBy("horaSolicitada", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get().await().documents.mapNotNull {
                it.toObject(SolicitudTurnoExtra::class.java)?.copy(id = it.id)
            }
    } catch (e: Exception) { Timber.e(e, "Error getAll solicitudes"); emptyList() }

    override suspend fun guardar(solicitud: SolicitudTurnoExtra): Boolean = try {
        val doc = col.document()
        doc.set(solicitud.copy(id = doc.id)).await(); true
    } catch (e: Exception) { Timber.e(e, "Error guardar solicitud"); false }

    override suspend fun responder(id: String, estado: String, respondidoPor: String): Boolean = try {
        col.document(id).update(
            "estado", estado,
            "respondidoEn", System.currentTimeMillis(),
            "respondidoPor", respondidoPor
        ).await(); true
    } catch (e: Exception) { Timber.e(e, "Error responder solicitud"); false }
}

