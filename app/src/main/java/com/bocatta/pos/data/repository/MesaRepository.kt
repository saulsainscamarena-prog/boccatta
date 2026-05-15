package com.bocatta.pos.data.repository

import com.bocatta.pos.domain.model.Mesa
import com.bocatta.pos.domain.repository.IMesaRepository
import com.bocatta.pos.network.firebase.FirebaseFirestoreProvider
import kotlinx.coroutines.tasks.await
import timber.log.Timber

class MesaRepository : IMesaRepository {
    private val col = FirebaseFirestoreProvider.db.collection("v2_mesas")

    override suspend fun getAll(): List<Mesa> = try {
        col.orderBy("numero").get().await().documents.mapNotNull {
            it.toObject(Mesa::class.java)?.copy(id = it.id)
        }
    } catch (e: Exception) { Timber.e(e, "Error mesas"); emptyList() }

    override suspend fun getByZona(zonaId: String): List<Mesa> = try {
        col.whereEqualTo("zonaId", zonaId).orderBy("numero").get().await().documents.mapNotNull {
            it.toObject(Mesa::class.java)?.copy(id = it.id)
        }
    } catch (e: Exception) { emptyList() }

    override suspend fun guardar(mesa: Mesa): Boolean = try {
        val doc = if (mesa.id.isBlank()) col.document() else col.document(mesa.id)
        doc.set(mesa.copy(id = doc.id)).await(); true
    } catch (e: Exception) { Timber.e(e, "Error guardar mesa"); false }

    override suspend fun eliminar(id: String): Boolean = try {
        col.document(id).delete().await(); true
    } catch (e: Exception) { false }

    override suspend fun actualizarEstado(id: String, estado: String): Boolean = try {
        col.document(id).update("estado", estado).await(); true
    } catch (e: Exception) { false }
}
