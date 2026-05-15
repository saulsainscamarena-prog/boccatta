package com.bocatta.pos.data.repository

import com.bocatta.pos.domain.model.Zona
import com.bocatta.pos.domain.repository.IZonaRepository
import com.bocatta.pos.network.firebase.FirebaseFirestoreProvider
import kotlinx.coroutines.tasks.await
import timber.log.Timber

class ZonaRepository : IZonaRepository {
    private val col = FirebaseFirestoreProvider.db.collection("v2_zonas")

    override suspend fun getAll(): List<Zona> = try {
        col.orderBy("orden").get().await().documents.mapNotNull {
            it.toObject(Zona::class.java)?.copy(id = it.id)
        }
    } catch (e: Exception) { Timber.e(e, "Error zonas"); emptyList() }

    override suspend fun guardar(zona: Zona): Boolean = try {
        val doc = if (zona.id.isBlank()) col.document() else col.document(zona.id)
        doc.set(zona.copy(id = doc.id)).await(); true
    } catch (e: Exception) { Timber.e(e, "Error guardar zona"); false }

    override suspend fun eliminar(id: String): Boolean = try {
        col.document(id).delete().await(); true
    } catch (e: Exception) { false }
}
