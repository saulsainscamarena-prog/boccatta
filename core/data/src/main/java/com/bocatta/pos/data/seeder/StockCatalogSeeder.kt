package com.bocatta.pos.data.seeder

import com.bocatta.pos.core.constants.FirestoreCollections
import com.bocatta.pos.data.repository.StockCatalogDefaults
import com.bocatta.pos.network.firebase.FirebaseFirestoreProvider
import kotlinx.coroutines.tasks.await
import timber.log.Timber

/**
 * Crea el documento de configuración de catálogo de stock en Firestore si no existe.
 * Idempotente: seguro de llamar en cada arranque de la app.
 *
 * Path: v2_configuracion/pos_stock_catalog
 */
object StockCatalogSeeder {

    private val db = FirebaseFirestoreProvider.db
    private const val TAG = "StockCatalogSeeder"

    suspend fun ensureExists() {
        try {
            val ref = db.collection(FirestoreCollections.CONFIGURACION)
                .document(StockCatalogDefaults.FIRESTORE_DOC)

            val snap = ref.get().await()
            if (snap.exists()) {
                Timber.tag(TAG).d("pos_stock_catalog ya existe, sin cambios")
                return
            }

            val payload = mapOf(
                "itemsVirtuales" to StockCatalogDefaults.itemsVirtuales,
                "itemsFisicos"   to StockCatalogDefaults.itemsFisicos,
                "nombres"        to StockCatalogDefaults.nombres,
                "version"        to 1,
                "creadoEn"       to System.currentTimeMillis(),
                "descripcion"    to "Catálogo de insumos gestionados por StockAllocationRepository. " +
                                    "Editar desde Firestore Console para actualizar sin nuevo release."
            )

            ref.set(payload).await()
            Timber.tag(TAG).i(
                "pos_stock_catalog creado: " +
                "virtuales=${StockCatalogDefaults.itemsVirtuales} " +
                "fisicos=${StockCatalogDefaults.itemsFisicos}"
            )
        } catch (e: Exception) {
            // No crítico: StockAllocationRepository tiene fallback a defaults en runtime
            Timber.tag(TAG).w(e, "No se pudo crear pos_stock_catalog, usando defaults en runtime")
        }
    }
}
