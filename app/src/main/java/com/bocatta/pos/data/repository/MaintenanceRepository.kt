package com.bocatta.pos.data.repository

import com.bocatta.pos.network.firebase.FirebaseFirestoreProvider
import kotlinx.coroutines.tasks.await
import com.bocatta.pos.core.constants.FirestoreCollections

class MaintenanceRepository {
    private val db = FirebaseFirestoreProvider.db

    private val coleccionesAntiguas = listOf(
        "productos",
        "inventario_maestro",
        "inventario_inteligente",
        "recetas",
        "gastos",
        "ventas",
        "devoluciones",
        "turnos_caja",
        "clientes",
        "auditoria_cancelaciones",
        "mermas",
        "recetas_produccion",
        FirestoreCollections.PRODUCTOS,
        FirestoreCollections.CLIENTES,
        FirestoreCollections.VENTAS,
        FirestoreCollections.GASTOS,
        FirestoreCollections.EMPLEADOS,
        FirestoreCollections.INSUMOS,
        FirestoreCollections.INVENTARIO_GLOBAL,
        FirestoreCollections.INVENTARIO_SUCURSAL,
        "FirestoreCollections.RECETAS_produccion",
        FirestoreCollections.RECETAS,
        FirestoreCollections.CONFIGURACION,
        FirestoreCollections.CANCELACIONES,
        FirestoreCollections.MERMA_LOGS,
        FirestoreCollections.PROVEEDORES,
        FirestoreCollections.COMPRAS_OLD
    )

    suspend fun purgaTotal(): Result<Unit> {
        return try {
            for (coleccion in coleccionesAntiguas) {
                borrarColeccion(coleccion)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun borrarColeccion(nombre: String) {
        val ref = db.collection(nombre)
        val snapshot = ref.get().await()
        
        if (snapshot.isEmpty) return

        // Firestore permite batches de máximo 500 documentos
        val chunks = snapshot.documents.chunked(500)
        for (chunk in chunks) {
            val batch = db.batch()
            for (doc in chunk) {
                batch.delete(doc.reference)
            }
            batch.commit().await()
        }
    }

    /**
     * Prepara la estructura base para la V2 creando documentos semilla vacíos
     * si fuera necesario, o simplemente asegurando que las colecciones existan.
     */
    suspend fun inicializarEstructuraV2() {
        val configRef = db.collection("config_v2").document("metadata")
        configRef.set(mapOf(
            "version" to "2.0.0",
            "last_reset" to System.currentTimeMillis(),
            "status" to "clean_slate"
        )).await()
    }
}


