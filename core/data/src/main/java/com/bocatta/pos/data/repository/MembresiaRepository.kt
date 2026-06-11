package com.bocatta.pos.data.repository

import com.bocatta.pos.core.constants.FirestoreCollections
import com.bocatta.pos.domain.model.ClienteV2
import com.bocatta.pos.network.firebase.FirebaseFirestoreProvider
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.tasks.await

class MembresiaRepository {

    private val db = FirebaseFirestoreProvider.db
    private val collection = db.collection(FirestoreCollections.CLIENTES)

    /**
     * Retrieves a client document by its id.
     * Returns null if the document does not exist or on any error.
     */
    suspend fun obtenerCliente(clienteId: String): ClienteV2? {
        return try {
            val doc = collection.document(clienteId).get().await()
            doc.toObject(ClienteV2::class.java)?.copy(idDocumento = doc.id)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Atomically increments the current cycle visit count, adds a purchase amount to the
     * current cycle list and updates the last‑visit timestamp. If `premioAplicado` is true the
     * purchases list is reset for the new cycle, otherwise the amount is appended.
     */
    suspend fun incrementarVisita(
        clienteId: String,
        montoCompra: Double,
        premioAplicado: Boolean
    ): Boolean {
        return try {
            val ref = collection.document(clienteId)
            db.runTransaction { tx ->
                val updates = mutableMapOf<String, Any>(
                    "visitasCicloActual" to FieldValue.increment(1),
                    "fechaUltimaVisita" to System.currentTimeMillis()
                )
                if (premioAplicado) {
                    // Reset the cycle: start with the current purchase as the first entry.
                    updates["comprasCicloActual"] = listOf(montoCompra)
                } else {
                    // Append to the existing purchases list.
                    val snapshot = tx.get(ref)
                    @Suppress("UNCHECKED_CAST")
                    val existing = snapshot.get("comprasCicloActual") as? List<Double> ?: emptyList()
                    updates["comprasCicloActual"] = existing + montoCompra
                }
                tx.update(ref, updates)
            }.await()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Resets the purchases list for a client, used when a new loyalty cycle starts.
     */
    suspend fun resetearCiclo(clienteId: String): Boolean {
        return try {
            collection.document(clienteId)
                .update(mapOf("comprasCicloActual" to emptyList<Double>()))
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }
}
