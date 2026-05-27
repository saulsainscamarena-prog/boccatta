package com.bocatta.pos.data.repository

import com.bocatta.pos.core.constants.FirestoreCollections
import com.bocatta.pos.data.local.OfflineDatabase
import com.bocatta.pos.domain.model.InsumoV2
import com.bocatta.pos.domain.model.RecetaV2
import com.bocatta.pos.domain.model.SalesInventoryProductV2
import com.bocatta.pos.network.firebase.FirebaseFirestoreProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Locale

/**
 * Mantiene una copia local operativa del catalogo que se usa para ventas offline.
 *
 * Firebase sigue siendo la fuente administrativa. SQLite es el snapshot local que
 * checkout, recetas y deducciones pueden consultar cuando no hay red.
 */
class OperationalCatalogSyncRepository(
    private val offlineDb: OfflineDatabase
) {
    private val db = FirebaseFirestoreProvider.db

    suspend fun sincronizarCatalogoSucursal(
        sucursal: String,
        productos: List<SalesInventoryProductV2>
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val sucursalId = sucursal.trim().lowercase(Locale.ROOT)
            if (sucursalId.isBlank()) return@withContext Result.success(Unit)

            productos.forEach { producto ->
                offlineDb.guardarProducto(producto)
            }

            val productIds = productos.map { it.id }.toSet()
            val recetaIds = productos.mapNotNull { it.recetaId }.toSet()
            val recetas = db.collection(FirestoreCollections.RECETAS)
                .get()
                .await()
                .documents
                .mapNotNull { doc -> doc.toObject(RecetaV2::class.java)?.copy(id = doc.id) }
                .filter { receta ->
                    receta.id in recetaIds || receta.productoId in productIds
                }

            recetas.forEach { receta ->
                offlineDb.guardarReceta(receta)
            }

            val stockSucursal = cargarStockSucursal(sucursalId)
            val insumos = db.collection(FirestoreCollections.INSUMOS)
                .get()
                .await()
                .documents
                .mapNotNull { doc -> doc.toObject(InsumoV2::class.java)?.copy(id = doc.id) }

            insumos.forEach { insumo ->
                offlineDb.guardarInsumo(
                    insumo.copy(cantidadEnBase = stockSucursal[insumo.id] ?: insumo.cantidadEnBase)
                )
            }

            Timber.tag("CATALOG_SYNC").i(
                "Catalogo local actualizado: productos=${productos.size}, recetas=${recetas.size}, insumos=${insumos.size}, sucursal=$sucursalId"
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.tag("CATALOG_SYNC").e(e, "No se pudo sincronizar catalogo operativo local")
            Result.failure(e)
        }
    }

    private suspend fun cargarStockSucursal(sucursalId: String): Map<String, Double> {
        val result = linkedMapOf<String, Double>()
        val collection = db.collection(FirestoreCollections.INVENTARIO_SUCURSAL)

        val snapshots = listOf(
            collection.whereEqualTo("sucursal", sucursalId).get().await(),
            collection.whereEqualTo("branchId", sucursalId).get().await()
        )

        snapshots.flatMap { it.documents }.forEach { doc ->
            val insumoId = doc.getString("insumoId")
                ?: doc.getString("productId")
                ?: doc.id.removePrefix("${sucursalId}_")
            val cantidad = doc.getDouble("cantidadEnBase")
                ?: doc.getDouble("cantidadDisponible")
                ?: doc.getDouble("currentQty")
                ?: return@forEach
            result[insumoId] = cantidad
        }

        return result
    }
}
