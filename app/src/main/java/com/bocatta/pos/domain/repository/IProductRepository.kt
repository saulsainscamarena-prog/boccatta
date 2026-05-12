package com.bocatta.pos.domain.repository

import com.bocatta.pos.domain.model.InventoryProductV2
import kotlinx.coroutines.flow.Flow

/**
 * Contrato para la gestión de productos.
 * Define las operaciones universales que cualquier implementación de repositorio debe cumplir.
 * Esto permite intercambiar la lógica de negocio (Food, Retail, Services) sin afectar la UI.
 */
interface IProductRepository {

    /**
     * Obtiene un flujo reactivo con la lista completa de productos activos.
     * Ideal para observar cambios en tiempo real desde Firestore.
     */
    fun getAllProducts(): Flow<List<InventoryProductV2>>

    /**
     * Obtiene un producto específico por su ID.
     * @return Flow que emite el producto o null si no existe.
     */
    fun getProductById(id: String): Flow<InventoryProductV2?>

    /**
     * Obtiene productos filtrados por categoría o giro.
     * @param category Categoría opcional para filtrar.
     * @param giro Tipo de negocio opcional para filtrar.
     */
    fun getProductsByFilter(category: String? = null, giro: String? = null): Flow<List<InventoryProductV2>>

    /**
     * Guarda o actualiza un producto en la fuente de datos.
     * @param product El producto a persistir.
     * @return true si la operación fue exitosa.
     */
    suspend fun saveProduct(product: InventoryProductV2): Boolean

    /**
     * Elimina lógicamente un producto (cambia status a INACTIVE).
     * @param id ID del producto a eliminar.
     */
    suspend fun deleteProduct(id: String): Boolean

    /**
     * Obtiene el tipo de producto para determinar el motor de negocio a usar.
     * @return String con el tipo: "RAW", "FINISHED", "SERVICE", "KIT", etc.
     */
    suspend fun getProductType(id: String): String?
    
    /**
     * Obtiene la unidad base configurada para un producto.
     * Útil para normalizar operaciones de inventario.
     */
    suspend fun getProductBaseUnit(productId: String): String?
}