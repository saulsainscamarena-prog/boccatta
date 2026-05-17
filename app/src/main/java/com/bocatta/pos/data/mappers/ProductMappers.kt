package com.bocatta.pos.data.mappers

import com.bocatta.pos.domain.model.InventoryProductV2
import com.bocatta.pos.domain.model.SalesInventoryProductV2

@Deprecated("Legacy bridge — use IProductRepository.getAllProducts() which returns InventoryProductV2")
fun SalesInventoryProductV2.toDomain(): InventoryProductV2 = InventoryProductV2(
    id = id,
    name = nombre,
    category = categoria,
    type = if (esCombo) "KIT" else "FINISHED",
    basePrice = precioVenta.values.firstOrNull() ?: 0.0,
    baseUnit = "pza",
    imageUrl = fotoUrl,
    attributes = mapOf(
        "emoji" to emoji,
        "esCombo" to esCombo,
        "recetaId" to (recetaId ?: ""),
        "toppingsIncluidos" to toppingsIncluidos,
        "costoToppingExtra" to costoToppingExtra,
        "esProductoTopping" to esProductoTopping
    )
)

@Deprecated("Legacy bridge — remove when UI layer uses InventoryProductV2 directly")
fun InventoryProductV2.toLegacy(): SalesInventoryProductV2 = SalesInventoryProductV2(
    id = id,
    nombre = name,
    emoji = (attributes["emoji"] as? String) ?: "🍩",
    categoria = category,
    precioVenta = mapOf("default" to basePrice),
    fotoUrl = imageUrl,
    esCombo = type == "KIT",
    recetaId = (attributes["recetaId"] as? String)?.ifBlank { null },
    toppingsIncluidos = (attributes["toppingsIncluidos"] as? Number)?.toInt() ?: 2,
    costoToppingExtra = (attributes["costoToppingExtra"] as? Number)?.toDouble() ?: 10.0,
    esProductoTopping = (attributes["esProductoTopping"] as? Boolean) ?: false
)

