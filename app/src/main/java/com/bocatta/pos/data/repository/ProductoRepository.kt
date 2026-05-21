package com.bocatta.pos.data.repository

import com.bocatta.pos.core.constants.FirestoreCollections
import com.bocatta.pos.domain.model.SalesInventoryProductV2
import com.bocatta.pos.domain.model.RecetaV2
import com.bocatta.pos.network.firebase.FirebaseFirestoreProvider
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import timber.log.Timber

/**
 * ProductoRepository — CRUD de productos y recetas en Firestore.
 *
 * Único punto de verdad para crear, editar y eliminar productos.
 * El DataSeederV2 usa este repositorio para la inicialización,
 * y AdminViewModel lo usa para operaciones en producción.
 */
class ProductoRepository {
    private val db = FirebaseFirestoreProvider.db

    // ── LECTURA ───────────────────────────────────────────────────────────────

    suspend fun existenProductos(): Boolean {
        return try {
            val snap = db.collection(FirestoreCollections.PRODUCTOS).limit(1).get().await()
            !snap.isEmpty
        } catch (e: Exception) {
            Timber.tag("PRODUCTO_REPO").e(e, "Error verificando existencia de productos")
            false
        }
    }

    suspend fun obtenerProductos(): List<SalesInventoryProductV2> {
        return try {
            db.collection(FirestoreCollections.PRODUCTOS).get().await()
                .documents.mapNotNull { it.toObject(SalesInventoryProductV2::class.java)?.copy(id = it.id) }
        } catch (e: Exception) {
            Timber.tag("PRODUCTO_REPO").e(e, "Error obteniendo productos")
            emptyList()
        }
    }

    suspend fun obtenerRecetaDeProducto(productoId: String): RecetaV2? {
        return try {
            val snap = db.collection(FirestoreCollections.RECETAS)
                .whereEqualTo("productoId", productoId)
                .limit(1).get().await()
            snap.documents.firstOrNull()?.toObject(RecetaV2::class.java)
        } catch (e: Exception) {
            null
        }
    }

    // ── ESCRITURA ─────────────────────────────────────────────────────────────

    /**
     * Guarda o actualiza un producto. Usa merge para no sobreescribir
     * campos no incluidos (útil para actualizaciones parciales).
     */
    suspend fun guardarProducto(producto: SalesInventoryProductV2): Boolean {
        return try {
            db.collection(FirestoreCollections.PRODUCTOS)
                .document(producto.id)
                .set(producto, SetOptions.merge())
                .await()
            Timber.tag("PRODUCTO_REPO").i("Producto guardado: ${producto.id}")
            true
        } catch (e: Exception) {
            Timber.tag("PRODUCTO_REPO").e(e, "Error guardando producto ${producto.id}")
            false
        }
    }

    /**
     * Guarda producto y receta en una sola operación atómica.
     * Si la receta es null o está vacía, solo guarda el producto.
     */
    suspend fun guardarProductoConReceta(producto: SalesInventoryProductV2, receta: RecetaV2?): Boolean {
        return try {
            val batch = db.batch()

            if (receta != null && receta.ingredientes.isNotEmpty()) {
                // Copiamos receta asignándole el id del producto
                val recetaConProductoId = receta.copy(productoId = producto.id)
                // Producto incluye referencia a receta
                val productoConReceta = producto.copy(recetaId = recetaConProductoId.id)
                // Guardar producto con referencia a receta
                batch.set(
                    db.collection(FirestoreCollections.PRODUCTOS).document(producto.id),
                    productoConReceta,
                    SetOptions.merge()
                )
                // Guardar receta
                batch.set(
                    db.collection(FirestoreCollections.RECETAS).document(recetaConProductoId.id),
                    recetaConProductoId,
                    SetOptions.merge()
                )
            } else {
                // Sólo producto, sin receta
                batch.set(
                    db.collection(FirestoreCollections.PRODUCTOS).document(producto.id),
                    producto,
                    SetOptions.merge()
                )
            }

            batch.commit().await()
            Timber.tag("PRODUCTO_REPO").i("Producto+receta guardados: ${producto.id}")
            true
        } catch (e: Exception) {
            Timber.tag("PRODUCTO_REPO").e(e, "Error en guardarProductoConReceta: ${producto.id}")
            false
        }
    }

    /**
     * Elimina producto y su receta asociada en una sola operación.
     */
    suspend fun eliminarProducto(productoId: String): Boolean {
        return try {
            val batch = db.batch()

            // Eliminar producto
            batch.delete(db.collection(FirestoreCollections.PRODUCTOS).document(productoId))

            // Buscar y eliminar receta asociada
            val recetaSnap = db.collection(FirestoreCollections.RECETAS)
                .whereEqualTo("productoId", productoId)
                .get().await()
            recetaSnap.documents.forEach { batch.delete(it.reference) }

            batch.commit().await()
            Timber.tag("PRODUCTO_REPO").i("Producto eliminado: $productoId")
            true
        } catch (e: Exception) {
            Timber.tag("PRODUCTO_REPO").e(e, "Error eliminando producto $productoId")
            false
        }
    }

    /**
     * Replica productos y recetas a una sucursal específica.
     * Útil cuando una sucursal necesita un catálogo diferente en el futuro.
     * Por ahora el catálogo es global, pero este método prepara la arquitectura.
     */
    suspend fun replicarCatalogoASucursal(sucursalId: String): Boolean {
        return try {
            // Utilizar agregación count() para obtener número de productos sin descargar documentos
            val countSnapshot = db.collection(FirestoreCollections.PRODUCTOS).count().get(AggregateSource.SERVER).await()
            val total = countSnapshot.count.toInt()

            // Actualizar sucursal con referencia al catálogo y total de productos
            db.collection(FirestoreCollections.SUCURSALES)
                .document(sucursalId)
                .set(
                    mapOf(
                        "id" to sucursalId,
                        "catalogoVersion" to System.currentTimeMillis(),
                        "totalProductos" to total
                    ),
                    SetOptions.merge()
                )
                .await()

            Timber.tag("PRODUCTO_REPO").i("Catálogo referenciado en sucursal $sucursalId: $total productos")
            true
        } catch (e: Exception) {
            Timber.tag("PRODUCTO_REPO").e(e, "Error referenciando catálogo en $sucursalId")
            false
        }
    }
}

