package com.bocatta.pos.data.repository

import com.bocatta.pos.core.constants.FirestoreCollections
import com.bocatta.pos.domain.model.InventoryProductV2
import com.bocatta.pos.domain.repository.IProductRepository
import com.bocatta.pos.network.firebase.FirebaseFirestoreProvider
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber

class ProductRepositoryImpl : IProductRepository {

    private val db = FirebaseFirestoreProvider.db
    private val collection = db.collection(FirestoreCollections.PRODUCTOS)

    private val _allProducts = MutableStateFlow<List<InventoryProductV2>>(emptyList())
    override fun getAllProducts(): Flow<List<InventoryProductV2>> {
        collection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Timber.tag("PRODUCT_REPO").e(error, "Error listening products")
                return@addSnapshotListener
            }
            val products = snapshot?.documents?.mapNotNull { doc ->
                val p = doc.toObject(InventoryProductV2::class.java)
                p?.copy(id = doc.id)
            } ?: emptyList()
            _allProducts.value = products.sortedBy { it.name }
        }
        return _allProducts.asStateFlow()
    }

    override fun getProductById(id: String): Flow<InventoryProductV2?> {
        val result = MutableStateFlow<InventoryProductV2?>(null)
        collection.document(id).addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            val product = snapshot?.toObject(InventoryProductV2::class.java)?.copy(id = snapshot.id)
            result.value = product
        }
        return result.asStateFlow()
    }

    override fun getProductsByFilter(category: String?, giro: String?): Flow<List<InventoryProductV2>> {
        val result = MutableStateFlow<List<InventoryProductV2>>(emptyList())
        var query = collection.whereEqualTo("status", "ACTIVE")
        if (category != null) query = query.whereEqualTo("category", category)
        if (giro != null) query = query.whereEqualTo("giro", giro)
        query.addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            val products = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(InventoryProductV2::class.java)?.copy(id = doc.id)
            } ?: emptyList()
            result.value = products
        }
        return result.asStateFlow()
    }

    override suspend fun saveProduct(product: InventoryProductV2): Boolean {
        return try {
            collection.document(product.id)
                .set(product, SetOptions.merge())
                .await()
            Timber.tag("PRODUCT_REPO").i("Product saved: ${product.id}")
            true
        } catch (e: Exception) {
            Timber.tag("PRODUCT_REPO").e(e, "Error saving product ${product.id}")
            false
        }
    }

    override suspend fun deleteProduct(id: String): Boolean {
        return try {
            collection.document(id)
                .update("status", "INACTIVE")
                .await()
            true
        } catch (e: Exception) {
            Timber.tag("PRODUCT_REPO").e(e, "Error deleting product $id")
            false
        }
    }

    override suspend fun getProductType(id: String): String? {
        return try {
            val doc = collection.document(id).get().await()
            doc.getString("type")
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getProductBaseUnit(productId: String): String? {
        return try {
            val doc = collection.document(productId).get().await()
            doc.getString("baseUnit")
        } catch (e: Exception) {
            null
        }
    }
}
