package com.bocatta.pos.data.repository

import android.content.Context
import com.bocatta.pos.core.constants.FirestoreCollections
import com.bocatta.pos.data.local.OfflineDatabase
import com.bocatta.pos.domain.model.InsumoV2
import com.bocatta.pos.domain.model.RecetaV2
import com.bocatta.pos.domain.model.SalesInventoryProductV2
import com.bocatta.pos.network.firebase.FirebaseFirestoreProvider
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Mantiene una copia local operativa del catalogo que se usa para ventas offline.
 *
 * Firebase sigue siendo la fuente administrativa. SQLite es el snapshot local que
 * checkout, recetas y deducciones pueden consultar cuando no hay red.
 */
class OperationalCatalogSyncRepository(
    context: Context,
    private val offlineDb: OfflineDatabase
) {
    private val db = FirebaseFirestoreProvider.db
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    suspend fun sincronizarCatalogoSucursal(
        sucursal: String,
        productos: List<SalesInventoryProductV2>
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val sucursalId = sucursal.trim().lowercase(Locale.ROOT)
            if (sucursalId.isBlank()) return@withContext Result.success(Unit)

            val fingerprint = calcularFingerprint(sucursalId, productos)
            if (!requiereSincronizacion(sucursalId, fingerprint)) {
                Timber.tag("CATALOG_SYNC").d("Catalogo local vigente para $sucursalId; se omite sync completo")
                return@withContext Result.success(Unit)
            }

            productos.forEach { producto ->
                offlineDb.guardarProducto(producto)
            }

            val productIds = productos.map { it.id }.toSet()
            val recetaIds = productos.mapNotNull { it.recetaId }.toSet()
            val recetas = cargarRecetasRelevantes(recetaIds, productIds)

            recetas.forEach { receta ->
                offlineDb.guardarReceta(receta)
            }

            val insumoIds = obtenerInsumosRelevantes(productos, recetas)
            val stockSucursal = cargarStockSucursal(sucursalId, insumoIds)
            val insumos = cargarInsumosRelevantes(insumoIds)

            insumos.forEach { insumo ->
                offlineDb.guardarInsumo(
                    insumo.copy(cantidadEnBase = stockSucursal[insumo.id] ?: insumo.cantidadEnBase)
                )
            }

            guardarEstadoSync(sucursalId, fingerprint)
            Timber.tag("CATALOG_SYNC").i(
                "Catalogo local actualizado: productos=${productos.size}, recetas=${recetas.size}, insumos=${insumos.size}, sucursal=$sucursalId"
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.tag("CATALOG_SYNC").e(e, "No se pudo sincronizar catalogo operativo local")
            Result.failure(e)
        }
    }

    private suspend fun cargarRecetasRelevantes(
        recetaIds: Set<String>,
        productIds: Set<String>
    ): List<RecetaV2> {
        val collection = db.collection(FirestoreCollections.RECETAS)
        val documents = linkedMapOf<String, DocumentSnapshot>()

        recetaIds.chunked(FIRESTORE_IN_LIMIT).forEach { chunk ->
            if (chunk.isNotEmpty()) {
                collection.whereIn(FieldPath.documentId(), chunk).get().await().documents.forEach { doc ->
                    documents[doc.id] = doc
                }
            }
        }

        productIds.chunked(FIRESTORE_IN_LIMIT).forEach { chunk ->
            if (chunk.isNotEmpty()) {
                collection.whereIn("productoId", chunk).get().await().documents.forEach { doc ->
                    documents[doc.id] = doc
                }
            }
        }

        return documents.values.mapNotNull { doc ->
            doc.toObject(RecetaV2::class.java)?.copy(id = doc.id)
        }
    }

    private suspend fun cargarInsumosRelevantes(insumoIds: Set<String>): List<InsumoV2> {
        if (insumoIds.isEmpty()) return emptyList()
        val collection = db.collection(FirestoreCollections.INSUMOS)
        return insumoIds.chunked(FIRESTORE_IN_LIMIT)
            .flatMap { chunk ->
                collection.whereIn(FieldPath.documentId(), chunk).get().await().documents
            }
            .mapNotNull { doc -> doc.toObject(InsumoV2::class.java)?.copy(id = doc.id) }
    }

    private suspend fun cargarStockSucursal(
        sucursalId: String,
        insumoIds: Set<String>
    ): Map<String, Double> {
        if (insumoIds.isEmpty()) return emptyMap()
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
            if (insumoId in insumoIds) {
                result[insumoId] = cantidad
            }
        }

        return result
    }

    private fun obtenerInsumosRelevantes(
        productos: List<SalesInventoryProductV2>,
        recetas: List<RecetaV2>
    ): Set<String> {
        val ids = linkedSetOf<String>()
        ids += recetas.flatMap { receta -> receta.ingredientes.map { it.insumoId } }
            .filter { it.isNotBlank() }
        ids += productos.flatMap { producto -> producto.consumiblesAsociados.map { it.consumibleId } }
            .filter { it.isNotBlank() }
        ids += productos.map { it.id }.filter { it.isNotBlank() }

        productos.flatMap { producto -> producto.configSchema.flatMap { it.options } }
            .forEach { opcion ->
                InventoryDeductions.mapearBaseAInsumo(opcion)?.first?.let(ids::add)
                InventoryDeductions.mapearToppingOAderezoAInsumo(opcion)?.first?.let(ids::add)
            }

        ids += setOf(
            "masa_crepa",
            "servilletas",
            "papel_hamburguesero",
            "tenedor",
            "cuchara",
            "charola",
            "vaso",
            "domo"
        )
        return ids
    }

    private fun requiereSincronizacion(sucursalId: String, fingerprint: String): Boolean {
        val now = System.currentTimeMillis()
        val lastHash = prefs.getString("${sucursalId}_hash", null)
        val lastAt = prefs.getLong("${sucursalId}_last_at", 0L)
        return lastHash != fingerprint || now - lastAt > SYNC_TTL_MS
    }

    private fun guardarEstadoSync(sucursalId: String, fingerprint: String) {
        prefs.edit()
            .putString("${sucursalId}_hash", fingerprint)
            .putLong("${sucursalId}_last_at", System.currentTimeMillis())
            .apply()
    }

    private fun calcularFingerprint(
        sucursalId: String,
        productos: List<SalesInventoryProductV2>
    ): String {
        val raw = buildString {
            append("sucursal=").append(sucursalId).append('|')
            productos.sortedBy { it.id }.forEach { prod ->
                append(prod.id).append('|')
                append(prod.nombre).append('|')
                append(prod.categoria).append('|')
                append(prod.subcategoria).append('|')
                append(prod.tipoProducto).append('|')
                append(prod.activo).append('|')
                append(prod.esProductoTopping).append('|')
                append(prod.recetaId.orEmpty()).append('|')
                append(prod.precioVenta.toSortedMap()).append('|')
                append(prod.consumiblesAsociados.sortedBy { it.consumibleId }).append('|')
                append(prod.configSchema).append('\n')
            }
        }
        return MessageDigest.getInstance("SHA-256")
            .digest(raw.toByteArray())
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }

    companion object {
        private const val PREFS_NAME = "operational_catalog_sync"
        private const val FIRESTORE_IN_LIMIT = 10
        private val SYNC_TTL_MS = TimeUnit.MINUTES.toMillis(5)
    }
}
