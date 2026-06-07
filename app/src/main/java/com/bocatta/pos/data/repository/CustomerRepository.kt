package com.bocatta.pos.data.repository

import com.bocatta.pos.core.constants.FirestoreCollections
import com.bocatta.pos.domain.model.ClienteV2
import com.bocatta.pos.network.firebase.FirebaseFirestoreProvider
import kotlinx.coroutines.tasks.await

class CustomerRepository {

    private val db = FirebaseFirestoreProvider.db
    private val collection = db.collection(FirestoreCollections.CLIENTES)

    suspend fun buscarCliente(query: String): List<ClienteV2> {
        return try {
            val snap = collection
                .whereGreaterThanOrEqualTo("nombre", query)
                .whereLessThanOrEqualTo("nombre", query + "\uf8ff")
                .limit(5).get().await()

            snap.documents.mapNotNull { doc ->
                doc.toObject(ClienteV2::class.java)?.copy(idDocumento = doc.id)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun registrarClienteNuevo(nombre: String, telefono: String): Result<ClienteV2> {
        return try {
            val snap = collection.whereEqualTo("telefono", telefono).get().await()
            if (!snap.isEmpty) {
                return Result.failure(Exception("Este cliente ya esta registrado."))
            }

            val docRef = collection.document()
            val nuevo = ClienteV2(idDocumento = docRef.id, nombre = nombre, telefono = telefono)
            docRef.set(nuevo).await()
            Result.success(nuevo)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
