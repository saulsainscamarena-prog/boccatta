package com.bocatta.pos.data.repository

import com.bocatta.pos.domain.model.StockAdjustmentEntity
import com.bocatta.pos.core.constants.FirestoreCollections
import com.bocatta.pos.domain.model.InventoryItem
import com.bocatta.pos.network.NetworkStateProvider
import com.bocatta.pos.domain.repository.IInventoryRepository
import com.bocatta.pos.domain.repository.IStockAdjustmentQueue
import com.bocatta.pos.domain.repository.StockDeduction
import kotlinx.coroutines.flow.first
import com.bocatta.pos.domain.model.MovementV2
import com.bocatta.pos.domain.repository.InventoryMovement
import com.bocatta.pos.domain.unit.UnitConverter
import com.bocatta.pos.network.firebase.FirebaseFirestoreProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.bocatta.pos.data.local.OfflineDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.Locale

class InventoryRepositoryImpl(
    private val networkStateProvider: NetworkStateProvider,
    private val adjustmentQueue: IStockAdjustmentQueue,
    private val offlineDb: OfflineDatabase
) : IInventoryRepository {

    private val db = FirebaseFirestoreProvider.db

    private val inventoryCollection = db.collection(FirestoreCollections.INVENTARIO_SUCURSAL)
    private val movementsCollection = db.collection(FirestoreCollections.MOVEMENTS)

    override fun getStockForBranch(branchId: String): Flow<List<InventoryItem>> = callbackFlow {
        val subscription = inventoryCollection.whereEqualTo("branchId", branchId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val items = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(InventoryItem::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(items)
            }
        awaitClose {
            subscription.remove()
            Timber.tag("INV_REPO").d("getStockForBranch snapshot listener removed successfully.")
        }
    }

    override fun getStockItem(branchId: String, productId: String): Flow<InventoryItem?> = callbackFlow {
        val docId = "${branchId}_$productId"
        val subscription = inventoryCollection.document(docId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val item = snapshot?.toObject(InventoryItem::class.java)?.copy(id = snapshot.id)
                trySend(item)
            }
        awaitClose {
            subscription.remove()
            Timber.tag("INV_REPO").d("getStockItem snapshot listener removed successfully.")
        }
    }

    override suspend fun getCurrentStock(branchId: String, productId: String): Double {
        return try {
            val docId = "${branchId}_$productId"
            val doc = inventoryCollection.document(docId).get().await()
            doc.getDouble("currentQty") ?: doc.getDouble("cantidadEnBase") ?: 0.0
        } catch (e: Exception) {
            Timber.e(e, "Error reading stock for $productId in $branchId")
            throw e
        }
    }

    override suspend fun adjustStock(
        branchId: String,
        productId: String,
        quantity: Double,
        unit: String,
        reason: String,
        referenceId: String,
        userId: String
    ): Boolean {
        return try {
            val baseQty = UnitConverter.toBase(quantity, unit)
            val baseUnit = UnitConverter.getBaseUnit(unit)
            val docId = "${branchId}_$productId"
            val batch = db.batch()

            batch.set(
                inventoryCollection.document(docId),
                mapOf(
                    "id" to docId,
                    "branchId" to branchId,
                    "productId" to productId,
                    "currentQty" to FieldValue.increment(baseQty),
                    "cantidadEnBase" to FieldValue.increment(baseQty), // Escritura dual para compatibilidad
                    "unit" to baseUnit,
                    "lastUpdated" to System.currentTimeMillis()
                ),
                SetOptions.merge()
            )

            val movementRef = movementsCollection.document()
            batch.set(movementRef, MovementV2(
                id = movementRef.id,
                branchId = branchId,
                type = reason.uppercase(Locale.ROOT),
                referenceId = referenceId,
                productId = productId,
                quantity = baseQty,
                unit = baseUnit,
                userId = userId,
                timestamp = System.currentTimeMillis()
            ))

            batch.commit().await()
            Timber.tag("INV_REPO").i("Stock adjusted: $productId $baseQty $unit ($reason)")
            true
        } catch (e: Exception) {
            Timber.tag("INV_REPO").e(e, "Error adjusting stock for $productId")
            false
        }
    }

    override suspend fun adjustStockBatch(
        branchId: String,
        userId: String,
        referenceId: String,
        deductions: List<StockDeduction>
    ): Boolean {
        return try {
            // Check connectivity
            val online = networkStateProvider.isOnline.first()
            if (online) {
                // Online: perform Firestore transaction as before
                val timestamp = System.currentTimeMillis()
                val result = db.runTransaction { transaction ->
                    for (deduction in deductions) {
                        val baseQty = UnitConverter.toBase(deduction.quantity, deduction.unit)
                        val baseUnit = UnitConverter.getBaseUnit(deduction.unit)
                        val docId = "${branchId}_${deduction.productId}"
                        val docRef = inventoryCollection.document(docId)

                        transaction.set(
                            docRef,
                            mapOf(
                                "id" to docId,
                                "branchId" to branchId,
                                "productId" to deduction.productId,
                                "currentQty" to FieldValue.increment(baseQty),
                                "cantidadEnBase" to FieldValue.increment(baseQty), // Escritura dual para compatibilidad
                                "unit" to baseUnit,
                                "lastUpdated" to timestamp
                            ),
                            SetOptions.merge()
                        )

                        val movRef = movementsCollection.document()
                        transaction.set(
                            movRef,
                            MovementV2(
                                id = movRef.id,
                                branchId = branchId,
                                type = deduction.reason.uppercase(Locale.ROOT),
                                referenceId = referenceId,
                                productId = deduction.productId,
                                quantity = baseQty,
                                unit = baseUnit,
                                userId = userId,
                                timestamp = timestamp
                            )
                        )
                    }
                    true
                }.await()
                Timber.tag("INV_REPO").i("Batch deduction completed online: ${deductions.size} items")
                result
            } else {
                // Offline: enqueue each deduction in SQLite queue
                val now = System.currentTimeMillis()
                deductions.forEach { deduction ->
                    val entity = StockAdjustmentEntity(
                        branchId = branchId,
                        productId = deduction.productId,
                        quantity = deduction.quantity,
                        unit = deduction.unit,
                        reason = deduction.reason,
                        timestamp = now
                    )
                    adjustmentQueue.enqueue(entity)
                }
                Timber.tag("INV_REPO").i("Batch deduction queued offline: ${deductions.size} items")
                true
            }
        } catch (e: Exception) {
            Timber.tag("INV_REPO").e(e, "Error in batch deduction (online/offline)")
            false
        }
    }

    override suspend fun syncOfflineAdjustment(adjustment: StockAdjustmentEntity): Boolean {
        // Reuse the existing adjustStock logic with placeholder reference/user IDs.
        return adjustStock(
            branchId = adjustment.branchId,
            productId = adjustment.productId,
            quantity = adjustment.quantity,
            unit = adjustment.unit,
            reason = adjustment.reason,
            referenceId = "",
            userId = "offline_sync"
        )
    }

    override suspend fun hasSufficientStock(
        branchId: String,
        productId: String,
        requiredQty: Double,
        unit: String
    ): Boolean {
        return try {
            val online = networkStateProvider.isOnline.first()
            val requiredBase = UnitConverter.toBase(requiredQty, unit)
            if (online) {
                try {
                    val currentStock = getCurrentStock(branchId, productId)
                    currentStock >= requiredBase
                } catch (e: Exception) {
                    Timber.tag("INV_REPO").w("Firestore stock query failed, falling back to local SQLite: ${e.message}")
                    validarStockLocal(productId, requiredBase)
                }
            } else {
                Timber.tag("INV_REPO").i("Offline mode detected, validating stock in local SQLite")
                validarStockLocal(productId, requiredBase)
            }
        } catch (e: Exception) {
            Timber.tag("INV_REPO").e(e, "Error validating stock; checking local SQLite before allowing sale")
            validarStockLocal(productId, UnitConverter.toBase(requiredQty, unit))
        }
    }

    private fun validarStockLocal(productId: String, requiredBase: Double): Boolean {
        return try {
            val localInsumo = offlineDb.obtenerInsumos().find { it.id == productId }
            if (localInsumo == null) {
                val idNormalizado = productId.lowercase(Locale.ROOT)
                val pareceInsumoControlado = idNormalizado.contains("_") || idNormalizado in CONTROLLED_STOCK_IDS
                if (pareceInsumoControlado) {
                    Timber.tag("INV_REPO").w(
                        "Controlled stock item $productId is missing from local SQLite. Blocking sale until catalog sync is complete."
                    )
                    false
                } else {
                    Timber.tag("INV_REPO").d("Product $productId is not in local SQLite insumos. Permitting direct sale.")
                    true
                }
            } else {
                val stockLocal = localInsumo.cantidadEnBase
                stockLocal >= requiredBase
            }
        } catch (localEx: Exception) {
            Timber.tag("INV_REPO").e(localEx, "Error reading local SQLite stock. Permitting sale for offline robustness.")
            true
        }
    }

    override fun getMovementHistory(
        branchId: String,
        productId: String,
        startDate: Long,
        endDate: Long
    ): Flow<List<InventoryMovement>> = callbackFlow {
        val subscription = movementsCollection
            .whereEqualTo("branchId", branchId)
            .whereEqualTo("productId", productId)
            .whereGreaterThanOrEqualTo("timestamp", startDate)
            .whereLessThanOrEqualTo("timestamp", endDate)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val movements = snapshot?.documents?.mapNotNull { doc ->
                    val m = doc.toObject(InventoryMovement::class.java)
                    m?.copy(id = doc.id)
                } ?: emptyList()
                trySend(movements)
            }
        awaitClose {
            subscription.remove()
            Timber.tag("INV_REPO").d("getMovementHistory snapshot listener removed successfully.")
        }
    }

    override suspend fun registrarCompraConPresentacion(
        branchId: String,
        insumoId: String,
        presentacionNombre: String,
        cantidadComprada: Double,
        contenidoEquivalente: Double,
        costoTotal: Double,
        userId: String
    ): Boolean {
        return try {
            val totalUnidades = cantidadComprada * contenidoEquivalente
            val docId = "${branchId}_$insumoId"
            val batch = db.batch()

            // 1. Incrementar stock en inventario sucursal
            batch.set(
                inventoryCollection.document(docId),
                mapOf(
                    "id" to docId,
                    "branchId" to branchId,
                    "productId" to insumoId,
                    "currentQty" to FieldValue.increment(totalUnidades),
                    "cantidadEnBase" to FieldValue.increment(totalUnidades),
                    "lastUpdated" to System.currentTimeMillis()
                ),
                SetOptions.merge()
            )

            // 2. Incrementar stock global y actualizar costo unitario en insumos
            val costoUnitario = if (totalUnidades > 0.0) costoTotal / totalUnidades else 0.0
            val insumoRef = db.collection(FirestoreCollections.INSUMOS).document(insumoId)
            batch.set(insumoRef, mapOf(
                "costoUnitarioBase" to costoUnitario,
                "cantidadEnBase" to FieldValue.increment(totalUnidades),
                "ultimaActualizacion" to System.currentTimeMillis()
            ), SetOptions.merge())

            // 3. Registrar Compra
            val compraRef = db.collection(FirestoreCollections.COMPRAS).document()
            batch.set(compraRef, mapOf(
                "id" to compraRef.id,
                "insumoId" to insumoId,
                "presentacion" to presentacionNombre,
                "cantidadComprada" to cantidadComprada,
                "contenidoUnidades" to contenidoEquivalente,
                "precioPagado" to costoTotal,
                "compradoPor" to userId,
                "fecha" to System.currentTimeMillis(),
                "sucursal" to branchId,
                "estado" to "aprobada",
                "auditada" to true
            ))

            // 4. Registrar Gasto
            val gastoRef = db.collection(FirestoreCollections.GASTOS).document()
            batch.set(gastoRef, mapOf(
                "id" to gastoRef.id,
                "descripcion" to "Compra: $cantidadComprada $presentacionNombre de $insumoId",
                "monto" to costoTotal,
                "categoria" to "Insumo",
                "fecha" to System.currentTimeMillis(),
                "sucursal" to branchId,
                "usuarioId" to userId,
                "insumoId" to insumoId,
                "cantidadSurtida" to totalUnidades,
                "presentacionCompra" to presentacionNombre,
                "cantidadComprada" to cantidadComprada,
                "contenidoPorUnidad" to contenidoEquivalente
            ))

            // 5. Registrar Movimiento
            val movementRef = movementsCollection.document()
            batch.set(movementRef, MovementV2(
                id = movementRef.id,
                branchId = branchId,
                type = "PURCHASE",
                referenceId = compraRef.id,
                productId = insumoId,
                quantity = totalUnidades,
                unit = "",
                userId = userId,
                timestamp = System.currentTimeMillis()
            ))

            batch.commit().await()
            Timber.tag("INV_REPO").i("Compra registrada con presentacion: $insumoId, $totalUnidades unidades")

            // Actualizar SQLite local si esta disponible para consistencia offline inmediata.
            try {
                offlineDb.actualizarStockInsumo(insumoId, offlineDb.obtenerStockInsumo(insumoId) + totalUnidades)
            } catch (sqle: Exception) {
                Timber.tag("INV_REPO").w("No se pudo actualizar stock en SQLite local: ${sqle.message}")
            }

            true
        } catch (e: Exception) {
            Timber.tag("INV_REPO").e(e, "Error al registrar compra con presentacion para $insumoId")
            false
        }
    }

    companion object {
        private val CONTROLLED_STOCK_IDS = setOf(
            "masa_crepa",
            "servilletas",
            "charola",
            "vaso",
            "domo",
            "tenedor",
            "cuchara"
        )
    }
}
