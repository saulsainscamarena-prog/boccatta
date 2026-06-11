package com.bocatta.pos.data.repository

import com.bocatta.pos.core.constants.FirestoreCollections
import com.bocatta.pos.domain.model.ThemeConfigV2
import com.bocatta.pos.network.firebase.FirebaseFirestoreProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ThemeRepository(
    private val db: FirebaseFirestore = FirebaseFirestoreProvider.db
) {
    private val docRef = db.collection(FirestoreCollections.CONFIGURACION).document("theme_global")

    fun observarTema(): Flow<ThemeConfigV2> = callbackFlow {
        val listener = docRef.addSnapshotListener { snap, _ ->
            val config = snap?.toObject(ThemeConfigV2::class.java) ?: ThemeConfigV2()
            trySend(config)
        }
        awaitClose { listener.remove() }
    }

    suspend fun guardar(config: ThemeConfigV2) {
        docRef.set(config.copy(updatedAt = System.currentTimeMillis())).await()
    }

    suspend fun restaurar() {
        docRef.set(ThemeConfigV2(enabled = false, updatedAt = System.currentTimeMillis())).await()
    }
}

