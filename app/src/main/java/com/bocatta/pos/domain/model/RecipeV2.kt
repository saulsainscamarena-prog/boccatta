package com.bocatta.pos.domain.model

/**
 * Receta orientada a producción batch (tandas).
 *
 * NO se guarda en la colección Firestore [FirestoreCollections.RECETAS] (`v2_recetas`).
 * Su colección es [FirestoreCollections.RECETAS_PRODUCCION] (`v2_recetas_produccion`).
 *
 * Diferencia clave con [RecetaV2]:
 * - [RecetaV2] -> deducción unitaria: "qué insumos descuenta 1 venta de este producto"
 * - [RecipeV2] -> rendimiento batch: "cuánto produce una tanda con X materia prima"
 *
 * @property yield cuántas unidades se obtienen de una tanda
 * @property yieldUnit unidad del rendimiento (pza, kg, L, etc.)
 * @property inputs lista de insumos necesarios para la tanda
 * @property status activa/inactiva
 */
data class RecipeV2(
    val id: String = "",
    val productId: String = "",
    val yield: Double = 1.0,
    val yieldUnit: String = "pza",
    val inputs: List<RecipeInput> = emptyList(),
    val status: String = "ACTIVE"
)

/**
 * Insumo individual dentro de una [RecipeV2].
 * Equivalente conceptual a [IngredienteReceta] pero sin nombre del insumo
 * (se resuelve por [insumoId]).
 */
data class RecipeInput(
    val insumoId: String = "",
    val qty: Double = 0.0,
    val unit: String = "g"
)
