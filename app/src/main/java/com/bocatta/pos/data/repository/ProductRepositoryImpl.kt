package com.bocatta.pos.data.repository

import com.bocatta.pos.core.constants.FirestoreCollections
import com.bocatta.pos.domain.model.InventoryProductV2
import com.bocatta.pos.domain.repository.IProductRepository
import com.bocatta.pos.network.firebase.FirebaseFirestoreProvider
import com.bocatta.pos.domain.model.SalesInventoryProductV2
import com.bocatta.pos.domain.model.ConfigOptionGroup
import com.bocatta.pos.domain.model.ConfigFieldType
import com.bocatta.pos.domain.model.DescuentoOpcion
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.tasks.await
import timber.log.Timber

class ProductRepositoryImpl : IProductRepository {

    private val db = FirebaseFirestoreProvider.db
    private val collection = db.collection(FirestoreCollections.PRODUCTOS)

    override fun getAllProducts(): Flow<List<InventoryProductV2>> = callbackFlow {
        val subscription = collection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Timber.tag("PRODUCT_REPO").e(error, "Error listening products")
                close(error)
                return@addSnapshotListener
            }
            val products = snapshot?.documents?.mapNotNull { doc ->
                val p = doc.toObject(InventoryProductV2::class.java)
                p?.copy(id = doc.id)
            } ?: emptyList()
            trySend(products.sortedBy { it.name })
        }
        awaitClose {
            subscription.remove()
            Timber.tag("PRODUCT_REPO").d("All products snapshot listener removed successfully.")
        }
    }

    override fun getSalesProducts(): Flow<List<SalesInventoryProductV2>> = callbackFlow {
        val subscription = collection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Timber.tag("PRODUCT_REPO").e(error, "Error listening sales products")
                close(error)
                return@addSnapshotListener
            }
            val products = snapshot?.documents?.mapNotNull { doc ->
                val prod = doc.toObject(SalesInventoryProductV2::class.java)
                if (prod != null) {
                    val schema = (doc.get("configSchema") as? List<*>)
                        ?.mapNotNull { rawGroup ->
                            (rawGroup as? Map<*, *>)?.entries
                                ?.mapNotNull { (key, value) -> (key as? String)?.let { it to value } }
                                ?.toMap()
                        }
                        ?.map { mapToConfigGroup(it) } ?: emptyList()
                    prod.copy(configSchema = schema, id = doc.id)
                } else null
            } ?: emptyList()
            trySend(products)
        }
        awaitClose {
            subscription.remove()
            Timber.tag("PRODUCT_REPO").d("Sales products snapshot listener removed successfully.")
        }
    }

    private fun mapToConfigGroup(map: Map<String, Any?>): ConfigOptionGroup {
       val typeStr = (map["type"] as? String) ?: "SINGLE_CHIP"
       val type = try { ConfigFieldType.valueOf(typeStr) } catch (e: Exception) { Timber.e(e, "Invalid ConfigFieldType: %s", typeStr); ConfigFieldType.SINGLE_CHIP }

       val preciosExtraRaw = map["preciosExtra"] as? Map<*, *>
       val preciosExtra = preciosExtraRaw?.entries?.mapNotNull { (k, v) ->
          val key = k as? String
          val value = (v as? Number)?.toDouble()
          if (key != null && value != null) key to value else null
       }?.toMap() ?: emptyMap()

       val descuentosInsumoRaw = map["descuentosInsumo"] as? Map<*, *>
       val descuentosInsumo = descuentosInsumoRaw?.entries?.mapNotNull { (k, v) ->
          val key = k as? String
          val valMap = v as? Map<*, *>
          if (key != null && valMap != null) {
             val insumoId = valMap["insumoId"] as? String ?: ""
             val cantidad = (valMap["cantidad"] as? Number)?.toDouble() ?: 0.0
             val unidad = valMap["unidad"] as? String ?: "g"
             key to DescuentoOpcion(insumoId, cantidad, unidad)
          } else null
       }?.toMap() ?: emptyMap()

       return ConfigOptionGroup(
          key = map["key"] as? String ?: "",
          title = map["title"] as? String ?: "",
          type = type,
          options = (map["options"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
          required = map["required"] as? Boolean ?: false,
          multiMax = (map["multiMax"] as? Number)?.toInt(),
          defaultValue = map["defaultValue"] as? String,
          preciosExtra = preciosExtra,
          descuentosInsumo = descuentosInsumo
       )
    }

    override fun getProductById(id: String): Flow<InventoryProductV2?> = callbackFlow {
        val subscription = collection.document(id).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val product = snapshot?.toObject(InventoryProductV2::class.java)?.copy(id = snapshot.id)
            trySend(product)
        }
        awaitClose {
            subscription.remove()
            Timber.tag("PRODUCT_REPO").d("ProductById snapshot listener removed successfully.")
        }
    }

    override fun getProductsByFilter(category: String?, giro: String?): Flow<List<InventoryProductV2>> = callbackFlow {
        var query = collection.whereEqualTo("status", "ACTIVE")
        if (category != null) query = query.whereEqualTo("category", category)
        if (giro != null) query = query.whereEqualTo("giro", giro)
        val subscription = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val products = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(InventoryProductV2::class.java)?.copy(id = doc.id)
            } ?: emptyList()
            trySend(products)
        }
        awaitClose {
            subscription.remove()
            Timber.tag("PRODUCT_REPO").d("ProductsByFilter snapshot listener removed successfully.")
        }
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

