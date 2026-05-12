package com.bocatta.pos.data.repository

import android.util.Log
import com.bocatta.pos.domain.model.PromocionUniversal
import com.bocatta.pos.network.firebase.FirebaseFirestoreProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class PromocionesRepository(
    private val db: FirebaseFirestore = FirebaseFirestoreProvider.db
) {
    private val COLLECTION_NAME = "promociones"

    suspend fun getPromocionesActivas(): List<PromocionUniversal> {
        return try {
            val snapshot = db.collection(COLLECTION_NAME)
                .whereEqualTo("activa", true)
                .get()
                .await()
            snapshot.toObjects(PromocionUniversal::class.java)
        } catch (e: Exception) {
            Log.e("PromocionesRepo", "Error al obtener promociones activas", e)
            emptyList()
        }
    }

    suspend fun getAllPromociones(): List<PromocionUniversal> {
        return try {
            val snapshot = db.collection(COLLECTION_NAME).get().await()
            snapshot.toObjects(PromocionUniversal::class.java)
        } catch (e: Exception) {
            Log.e("PromocionesRepo", "Error al obtener todas las promociones", e)
            emptyList()
        }
    }

    suspend fun guardarPromocion(promocion: PromocionUniversal): Boolean {
        return try {
            val docId = promocion.id.ifBlank { java.util.UUID.randomUUID().toString() }
            val promoParaGuardar = promocion.copy(id = docId)
            db.collection(COLLECTION_NAME).document(docId).set(promoParaGuardar).await()
            true
        } catch (e: Exception) {
            Log.e("PromocionesRepo", "Error al guardar promociÃ³n", e)
            false
        }
    }

    suspend fun eliminarPromocion(id: String): Boolean {
        return try {
            db.collection(COLLECTION_NAME).document(id).delete().await()
            true
        } catch (e: Exception) {
            Log.e("PromocionesRepo", "Error al eliminar promociÃ³n", e)
            false
        }
    }
}
