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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber

class InventoryRepositoryImpl(
    private val networkStateProvider: NetworkStateProvider,
    private val adjustmentQueue: IStockAdjustmentQueue
) : IInventoryRepository {

    private val db = FirebaseFirestoreProvider.db

    private val inventoryCollection = db.collection(FirestoreCollections.INVENTARIO_SUCURSAL)
    private val movementsCollection = db.collection(FirestoreCollections.MOVEMENTS)

    override fun getStockForBranch(branchId: String): Flow<List<InventoryItem>> {
        val result = MutableStateFlow<List<InventoryItem>>(emptyList())
        inventoryCollection.whereEqualTo("branchId", branchId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val items = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(InventoryItem::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                result.value = items
            }
        return result.asStateFlow()
    }

    override fun getStockItem(branchId: String, productId: String): Flow<InventoryItem?> {
        val result = MutableStateFlow<InventoryItem?>(null)
        val docId = "${branchId}_$productId"
        inventoryCollection.document(docId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val item = snapshot?.toObject(InventoryItem::class.java)?.copy(id = snapshot.id)
                result.value = item
            }
        return result.asStateFlow()
    }

    override suspend fun getCurrentStock(branchId: String, productId: String): Double {
        return try {
            val docId = "${branchId}_$productId"
            val doc = inventoryCollection.document(docId).get().await()
            doc.getDouble("currentQty") ?: 0.0
        } catch (e: Exception) {
            0.0
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
                    "unit" to baseUnit,
                    "lastUpdated" to System.currentTimeMillis()
                ),
                SetOptions.merge()
            )

            val movementRef = movementsCollection.document()
            batch.set(movementRef, MovementV2(
                id = movementRef.id,
                branchId = branchId,
                type = reason.uppercase(),
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
                                type = deduction.reason.uppercase(),
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
        // Re‑use the existing adjustStock logic with placeholder reference/user IDs
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
            val currentStock = getCurrentStock(branchId, productId)
            val requiredBase = UnitConverter.toBase(requiredQty, unit)
            currentStock >= requiredBase
        } catch (e: Exception) {
            false
        }
    }

    override fun getMovementHistory(
        branchId: String,
        productId: String,
        startDate: Long,
        endDate: Long
    ): Flow<List<InventoryMovement>> {
        val result = MutableStateFlow<List<InventoryMovement>>(emptyList())
        movementsCollection
            .whereEqualTo("branchId", branchId)
            .whereEqualTo("productId", productId)
            .whereGreaterThanOrEqualTo("timestamp", startDate)
            .whereLessThanOrEqualTo("timestamp", endDate)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val movements = snapshot?.documents?.mapNotNull { doc ->
                    val m = doc.toObject(InventoryMovement::class.java)
                    m?.copy(id = doc.id)
                } ?: emptyList()
                result.value = movements
            }
        return result.asStateFlow()
    }
}

