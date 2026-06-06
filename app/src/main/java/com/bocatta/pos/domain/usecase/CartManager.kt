package com.bocatta.pos.domain.usecase

import androidx.compose.runtime.mutableStateListOf
import com.bocatta.pos.domain.model.ConfigResult
import com.bocatta.pos.domain.model.ItemCarritoV2
import com.bocatta.pos.domain.model.SalesInventoryProductV2
import com.bocatta.pos.domain.engine.PricingEngine
import java.math.BigDecimal
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

/**
 * Gestor del carrito de compras para Bocatta POS.
 * Extrae la lógica de agregación, edición y cálculo de precios inline del SalesViewModelV2.
 */
class CartManager {
    val carrito = mutableStateListOf<ItemCarritoV2>()
    private val cartIdSequence = AtomicLong(System.currentTimeMillis())
    private val undoStack = mutableListOf<List<ItemCarritoV2>>()
    val hayUndo: Boolean get() = undoStack.isNotEmpty()

    fun guardarEstadoParaUndo() {
        undoStack.add(carrito.toList())
        if (undoStack.size > 20) undoStack.removeFirst()
    }

    fun undoLastAction() {
        if (undoStack.isNotEmpty()) {
            carrito.clear()
            carrito.addAll(undoStack.removeLast())
        }
    }

    fun clear() {
        carrito.clear()
    }

    fun setCarrito(items: List<ItemCarritoV2>) {
        carrito.clear()
        carrito.addAll(items)
    }

    fun generarCartId(productoId: String): String {
        val safeProductId = productoId.ifBlank { "item" }
            .replace(Regex("[^A-Za-z0-9_-]"), "_")
        return "cart_${cartIdSequence.incrementAndGet()}_$safeProductId"
    }

    fun crearItemCarrito(
        producto: SalesInventoryProductV2,
        sucursal: String,
        base: String? = null,
        aderezos: List<String> = emptyList(),
        toppings: List<String> = emptyList(),
        esSeparado: Boolean = false,
        componentes: List<ItemCarritoV2> = emptyList(),
        cantidadGramos: Double? = null
    ): ItemCarritoV2 {
        val nota = buildString {
            if (cantidadGramos != null && cantidadGramos > 0.0) {
                append("${cantidadGramos.toInt()}g de ${producto.nombre}")
            } else {
                base?.let { append("Base: $it. ") }
                if (aderezos.isNotEmpty()) append("Aderezos: ${aderezos.joinToString(", ")}. ")
                if (toppings.isNotEmpty()) append("Extras: ${toppings.joinToString(", ")}")
                if (esSeparado) append(" (Separadas)")
            }
        }

        val precioBase = producto.precioVenta[sucursal.lowercase(Locale.ROOT)] ?: 0.0
        val precioCalculado = PricingEngine.calcularPrecioDirecto(
            precioBase = precioBase,
            categoria = producto.categoria,
            base = base,
            toppings = toppings,
            costoToppingExtra = producto.costoToppingExtra
        )

        return ItemCarritoV2(
            cartId = generarCartId(producto.id),
            producto = producto,
            precioFinal = BigDecimal.valueOf(precioCalculado),
            nota = nota,
            nombre = producto.nombre,
            base = base ?: "",
            aderezos = aderezos,
            toppings = toppings,
            esSeparado = esSeparado,
            componentesCombo = componentes,
            cantidadGramos = cantidadGramos
        )
    }

    fun agregarAlCarrito(
        producto: SalesInventoryProductV2,
        sucursal: String,
        base: String? = null,
        aderezos: List<String> = emptyList(),
        toppings: List<String> = emptyList(),
        esSeparado: Boolean = false,
        componentes: List<ItemCarritoV2> = emptyList(),
        cantidadGramos: Double? = null
    ) {
        guardarEstadoParaUndo()
        val item = crearItemCarrito(producto, sucursal, base, aderezos, toppings, esSeparado, componentes, cantidadGramos)
        val existingIndex = carrito.indexOfFirst {
            it.producto.id == item.producto.id &&
            it.base == item.base &&
            it.aderezos == item.aderezos &&
            it.toppings == item.toppings &&
            it.esSeparado == item.esSeparado &&
            it.paraLlevar == item.paraLlevar &&
            it.cantidadGramos == item.cantidadGramos &&
            it.nota == item.nota &&
            it.nombre == item.nombre &&
            it.precioFinal == item.precioFinal &&
            it.componentesCombo == item.componentesCombo
        }
        if (existingIndex != -1) {
            val existingItem = carrito[existingIndex]
            carrito[existingIndex] = existingItem.copyConCantidad(existingItem.cantidad + 1)
        } else {
            carrito.add(item)
        }
    }

    fun reemplazarItemCarrito(
        itemOriginal: ItemCarritoV2,
        producto: SalesInventoryProductV2,
        sucursal: String,
        base: String? = null,
        aderezos: List<String> = emptyList(),
        toppings: List<String> = emptyList(),
        esSeparado: Boolean = false,
        componentes: List<ItemCarritoV2> = emptyList(),
        cantidadGramos: Double? = null
    ) {
        val index = carrito.indexOf(itemOriginal)
        if (index == -1) return
        guardarEstadoParaUndo()
        carrito[index] = crearItemCarrito(
            producto,
            sucursal,
            base,
            aderezos,
            toppings,
            esSeparado = esSeparado,
            componentes = componentes,
            cantidadGramos = cantidadGramos
        ).copy(cartId = itemOriginal.cartId)
    }

    fun agregarAlCarritoConConfig(
        producto: SalesInventoryProductV2,
        sucursal: String,
        config: ConfigResult
    ) {
        guardarEstadoParaUndo()
        val base = config["base"]?.firstOrNull()?.removeSuffix(" (Premium)")
        val aderezos = config["aderezos"] ?: emptyList()
        val toppings = (config["toppings"] ?: emptyList()).map { it.removeSuffix(" (Premium)") }
        val extras = config.entries
            .filter { it.key !in setOf("base", "aderezos", "toppings") }
            .flatMap { (key, values) -> values.map { "$key: $it" } }

        val nota = buildString {
            base?.let { append("Base: $it. ") }
            if (aderezos.isNotEmpty()) append("Aderezos: ${aderezos.joinToString(", ")}. ")
            if (toppings.isNotEmpty()) append("Extras: ${toppings.joinToString(", ")}")
            if (extras.isNotEmpty()) append(" [${extras.joinToString("; ")}]")
        }

        val precioBase = producto.precioVenta[sucursal.lowercase(Locale.ROOT)] ?: 0.0
        val preciosExtra = producto.configSchema.flatMap { it.preciosExtra.entries }.associate { it.key to it.value }
        val precioCalculado = PricingEngine.calcularPrecioProducto(precioBase, producto.categoria, config, preciosExtra)

        val item = ItemCarritoV2(
            cartId = generarCartId(producto.id),
            producto = producto,
            precioFinal = BigDecimal.valueOf(precioCalculado),
            nota = nota,
            nombre = producto.nombre,
            base = base ?: "",
            aderezos = aderezos,
            toppings = toppings,
            esSeparado = false,
            componentesCombo = emptyList()
        )

        val existingIndex = carrito.indexOfFirst {
            it.producto.id == item.producto.id &&
            it.base == item.base &&
            it.aderezos == item.aderezos &&
            it.toppings == item.toppings &&
            it.esSeparado == item.esSeparado &&
            it.paraLlevar == item.paraLlevar &&
            it.cantidadGramos == item.cantidadGramos &&
            it.nota == item.nota &&
            it.nombre == item.nombre &&
            it.precioFinal == item.precioFinal &&
            it.componentesCombo == item.componentesCombo
        }
        if (existingIndex != -1) {
            val existingItem = carrito[existingIndex]
            carrito[existingIndex] = existingItem.copyConCantidad(existingItem.cantidad + 1)
        } else {
            carrito.add(item)
        }
    }

    fun reemplazarItemCarritoConConfig(
        itemOriginal: ItemCarritoV2,
        producto: SalesInventoryProductV2,
        sucursal: String,
        config: ConfigResult
    ) {
        val index = carrito.indexOf(itemOriginal)
        if (index == -1) return
        guardarEstadoParaUndo()

        val base = config["base"]?.firstOrNull()?.removeSuffix(" (Premium)")
        val aderezos = config["aderezos"] ?: emptyList()
        val toppings = (config["toppings"] ?: emptyList()).map { it.removeSuffix(" (Premium)") }
        val extras = config.entries
            .filter { it.key !in setOf("base", "aderezos", "toppings") }
            .flatMap { (key, values) -> values.map { "$key: $it" } }

        val nota = buildString {
            base?.let { append("Base: $it. ") }
            if (aderezos.isNotEmpty()) append("Aderezos: ${aderezos.joinToString(", ")}. ")
            if (toppings.isNotEmpty()) append("Extras: ${toppings.joinToString(", ")}")
            if (extras.isNotEmpty()) append(" [${extras.joinToString("; ")}]")
        }

        val precioBase = producto.precioVenta[sucursal.lowercase(Locale.ROOT)] ?: 0.0
        val preciosExtra = producto.configSchema.flatMap { it.preciosExtra.entries }.associate { it.key to it.value }
        val precioCalculado = PricingEngine.calcularPrecioProducto(precioBase, producto.categoria, config, preciosExtra)

        val item = ItemCarritoV2(
            cartId = itemOriginal.cartId,
            producto = producto,
            precioFinal = BigDecimal.valueOf(precioCalculado),
            nota = nota,
            nombre = producto.nombre,
            base = base ?: "",
            aderezos = aderezos,
            toppings = toppings,
            esSeparado = itemOriginal.esSeparado,
            paraLlevar = itemOriginal.paraLlevar,
            cantidadGramos = itemOriginal.cantidadGramos,
            cantidad = itemOriginal.cantidad,
            componentesCombo = emptyList()
        )

        carrito[index] = item
    }

    fun modificarCantidad(item: ItemCarritoV2, delta: Int) {
        val index = carrito.indexOf(item)
        if (index != -1) {
            val nuevaCant = item.cantidad + delta
            if (nuevaCant >= 1) {
                guardarEstadoParaUndo()
                carrito[index] = item.copy(cantidad = nuevaCant)
            }
        }
    }

    fun toggleParaLlevarItem(cartId: String) {
        val index = carrito.indexOfFirst { it.cartId == cartId }
        if (index != -1) {
            guardarEstadoParaUndo()
            val item = carrito[index]
            carrito[index] = item.copy(paraLlevar = !item.paraLlevar)
        }
    }

    fun eliminarDelCarrito(item: ItemCarritoV2) {
        guardarEstadoParaUndo()
        carrito.remove(item)
    }

    fun limpiarCarrito(guardarUndo: Boolean = true) {
        if (guardarUndo && carrito.isNotEmpty()) {
            guardarEstadoParaUndo()
        }
        carrito.clear()
    }
}
