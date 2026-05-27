package com.bocatta.pos.data.repository

import com.bocatta.pos.core.TicketUtils
import com.bocatta.pos.core.constants.FirestoreCollections
import com.bocatta.pos.domain.model.ClienteV2
import com.bocatta.pos.domain.model.GastoV2
import com.bocatta.pos.domain.model.IngredienteReceta
import com.bocatta.pos.domain.model.ItemCarritoV2
import com.bocatta.pos.domain.model.ItemVendidoV2
import com.bocatta.pos.domain.repository.ResultadoVenta
import com.bocatta.pos.domain.repository.SalesRepository
import com.google.firebase.firestore.FieldValue
import com.bocatta.pos.network.firebase.FirebaseFirestoreProvider
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.util.Locale

class FirebaseSalesRepositoryV2 : SalesRepository {
    private val db = FirebaseFirestoreProvider.db
    private val allocationRepo = StockAllocationRepository()

    override suspend fun finalizarVentaConInventario(
        carrito: List<ItemCarritoV2>,
        sucursal: String,
        usuarioNombre: String,
        clienteSeleccionado: ClienteV2?,
        descuentoLealtad: Double,
        metodoPagoSeleccionado: String,
        esConsumoEmpleado: Boolean,
        descuentoPromociones: Double,
        descuentoManual: Double,
        splitPartes: List<com.bocatta.pos.domain.model.SplitParte>
    ): ResultadoVenta {
        val sucursalId = normalizarSucursal(sucursal)
        val subtotal = carrito.sumOf { it.precioFinal.toDouble() * it.cantidad }
        val totalFinal = if (esConsumoEmpleado) 0.0 else (subtotal - descuentoLealtad - descuentoPromociones - descuentoManual).coerceAtLeast(0.0)

        return db.runTransaction { transaction ->
            val contadorRef = db.collection(FirestoreCollections.CONFIGURACION).document("contadores_$sucursalId")
            val counterDoc = transaction.get(contadorRef)
            val nextTicket = (counterDoc.getLong("ultimo_ticket") ?: 0L) + 1
            val codigoTicket = TicketUtils.generarCodigoTicket(sucursalId, nextTicket)

            val recetaIngredientes = carrito.associate { item ->
                val recetaId = item.producto.recetaId
                item.cartId to if (recetaId.isNullOrBlank()) {
                    emptyList()
                } else {
                    val recetaSnap = transaction.get(db.collection(FirestoreCollections.RECETAS).document(recetaId))
                    val raw = recetaSnap.get("ingredientes") as? List<*> ?: emptyList<Any>()
                    raw.mapNotNull { it as? Map<*, *> }.map {
                        IngredienteReceta(
                            insumoId = it["insumoId"] as? String ?: "",
                            nombreInsumo = it["nombreInsumo"] as? String ?: "",
                            cantidad = (it["cantidad"] as? Number)?.toDouble() ?: 0.0,
                            unidad = it["unidad"] as? String ?: "g"
                        )
                    }.filter { it.insumoId.isNotBlank() }
                }
            }

            val deducciones = linkedMapOf<String, Double>()
            val deduccionesPorLinea = carrito.associate { item ->
                val linea = InventoryDeductions.calcularParaItem(item, recetaIngredientes[item.cartId] ?: emptyList())
                linea.forEach { (insumoId, cantidad) -> deducciones[insumoId] = (deducciones[insumoId] ?: 0.0) + cantidad }
                item.cartId to linea
            }

            val branchRef = db.collection(FirestoreCollections.INVENTARIO_SUCURSAL)
            val globalRef = db.collection(FirestoreCollections.INVENTARIO_GLOBAL)
            val legacySucursalId = sucursal.trim()
            val idsConCuotaSucursal = allocationRepo.itemsVendibles.toSet()
            val idsFisicosSucursal = allocationRepo.itemsFisicos.toSet()
            val stockDocs = deducciones.keys.filter { idsConCuotaSucursal.contains(it) }.associateWith { insumoId ->
                val requerido = deducciones[insumoId] ?: 0.0
                val primaryRef = branchRef.document("${sucursalId}_$insumoId")
                val primarySnap = transaction.get(primaryRef)
                val primaryActual = primarySnap.getDouble("cantidadEnBase") ?: primarySnap.getDouble("cantidadDisponible") ?: 0.0
                val legacyRef = branchRef.document("${legacySucursalId}_$insumoId")
                if (legacyRef.id != primaryRef.id && primaryActual < requerido) {
                    val legacySnap = transaction.get(legacyRef)
                    val legacyActual = legacySnap.getDouble("cantidadEnBase") ?: legacySnap.getDouble("cantidadDisponible") ?: 0.0
                    if (legacyActual >= requerido) legacyRef to legacySnap else primaryRef to primarySnap
                } else {
                    primaryRef to primarySnap
                }
            }
            val globalDocs = deducciones.keys.filter { !idsFisicosSucursal.contains(it) }.associateWith { insumoId ->
                transaction.get(globalRef.document(insumoId))
            }
            deducciones.forEach { (insumoId, requerido) ->
                if (idsConCuotaSucursal.contains(insumoId)) {
                    val snap = stockDocs[insumoId]?.second
                    val actual = snap?.getDouble("cantidadEnBase") ?: snap?.getDouble("cantidadDisponible") ?: 0.0
                    if (actual < requerido) {
                        throw IllegalStateException("Stock insuficiente para $insumoId en sucursal $sucursalId. Disponible: $actual, requerido: $requerido")
                    }
                }
                if (!idsFisicosSucursal.contains(insumoId)) {
                    val globalSnap = globalDocs[insumoId]
                    val globalActual = globalSnap?.getDouble("cantidadEnBase") ?: globalSnap?.getDouble("cantidadDisponible") ?: 0.0
                    if (globalActual < requerido) {
                        throw IllegalStateException("Stock insuficiente para $insumoId en bodega central. Disponible: $globalActual, requerido: $requerido")
                    }
                }
            }

            val ventaId = db.collection(FirestoreCollections.VENTAS).document().id
            val lineasVenta = carrito.map { item ->
                ItemVendidoV2(
                    cartId = item.cartId,
                    productoId = item.producto.id,
                    nombre = item.nombre.ifBlank { item.producto.nombre },
                    cantidad = item.cantidad,
                    precioUnitario = item.precioFinal.toDouble(),
                    base = item.base,
                    aderezos = item.aderezos,
                    toppings = item.toppings,
                    separadas = item.esSeparado,
                    recetaId = item.producto.recetaId,
                    deducciones = deduccionesPorLinea[item.cartId] ?: emptyMap(),
                    componentesCombo = item.componentesCombo.map { componente ->
                        ItemVendidoV2(
                            cartId = componente.cartId,
                            productoId = componente.producto.id,
                            nombre = componente.nombre.ifBlank { componente.producto.nombre },
                            cantidad = componente.cantidad,
                            precioUnitario = componente.precioFinal.toDouble(),
                            base = componente.base,
                            aderezos = componente.aderezos,
                            toppings = componente.toppings,
                            separadas = componente.esSeparado,
                            recetaId = componente.producto.recetaId
                        )
                    }
                )
            }
            val productosIds = carrito.flatMap { item -> List(item.cantidad) { item.producto.id } }

            transaction.set(
                contadorRef,
                mapOf(
                    "ultimo_ticket" to nextTicket,
                    "sucursal" to sucursalId,
                    "creado" to (counterDoc.getLong("creado") ?: System.currentTimeMillis())
                ),
                SetOptions.merge()
            )
            val ventaDocMap = mutableMapOf<String, Any?>(
                "id" to ventaId,
                "ticket" to nextTicket,
                "numeroTicket" to nextTicket,
                "codigoTicket" to codigoTicket,
                "total" to totalFinal,
                "descuentoLealtad" to descuentoLealtad,
                "descuentoPromociones" to descuentoPromociones,
                "descuentoManual" to descuentoManual,
                "fecha" to System.currentTimeMillis(),
                "sucursal" to sucursalId,
                "atendio" to usuarioNombre,
                "metodoPago" to metodoPagoSeleccionado,
                "esConsumoEmpleado" to esConsumoEmpleado,
                "estado" to "completada",
                "clienteId" to clienteSeleccionado?.telefono,
                "productos" to lineasVenta,
                "productosIds" to productosIds
            )
            if (splitPartes.isNotEmpty()) {
                ventaDocMap["pagosDivididos"] = splitPartes.map {
                    mapOf(
                        "persona" to it.personaIndex + 1,
                        "monto" to it.monto.toDouble(),
                        "metodoPago" to it.metodoPago.valor
                    )
                }
            }
            transaction.set(
                db.collection(FirestoreCollections.VENTAS).document(ventaId),
                ventaDocMap
            )

            if (clienteSeleccionado != null && !esConsumoEmpleado) {
                val clienteRef = db.collection(FirestoreCollections.CLIENTES).document(clienteSeleccionado.telefono)
                val nuevasVisitas = if (clienteSeleccionado.visitasCicloActual >= 5) 1 else clienteSeleccionado.visitasCicloActual + 1
                transaction.update(clienteRef, "visitasCicloActual", nuevasVisitas)
                transaction.update(clienteRef, "fechaUltimaVisita", System.currentTimeMillis())
                if (clienteSeleccionado.visitasCicloActual >= 5) {
                    transaction.update(clienteRef, "comprasCicloActual", listOf(totalFinal))
                } else {
                    transaction.update(clienteRef, "comprasCicloActual", FieldValue.arrayUnion(totalFinal))
                }
            }

            deducciones.forEach { (insumoId, cantidad) ->
                if (idsConCuotaSucursal.contains(insumoId)) {
                    val stockRef = stockDocs[insumoId]?.first ?: branchRef.document("${sucursalId}_$insumoId")
                    transaction.set(
                        stockRef,
                        mapOf(
                            "id" to stockRef.id,
                            "insumoId" to insumoId,
                            "sucursal" to sucursalId,
                            "cantidadEnBase" to FieldValue.increment(-cantidad),
                            "ultimaActualizacion" to System.currentTimeMillis()
                        ),
                        SetOptions.merge()
                    )
                }
                if (!idsFisicosSucursal.contains(insumoId)) {
                    val centralRef = globalRef.document(insumoId)
                    transaction.set(
                        centralRef,
                        mapOf(
                            "id" to insumoId,
                            "insumoId" to insumoId,
                            "cantidadEnBase" to FieldValue.increment(-cantidad),
                            "ultimaActualizacion" to System.currentTimeMillis()
                        ),
                        SetOptions.merge()
                    )
                }
                val movRef = db.collection(FirestoreCollections.MOVIMIENTOS_INVENTARIO).document()
                transaction.set(
                    movRef,
                    mapOf(
                        "id" to movRef.id,
                        "tipo" to "venta",
                        "insumoId" to insumoId,
                        "cantidadEnBase" to -cantidad,
                        "sucursal" to sucursalId,
                        "referenciaId" to ventaId,
                        "fecha" to System.currentTimeMillis()
                    )
                )
            }

            ResultadoVenta(numeroTicket = nextTicket, codigoTicket = codigoTicket)
        }.await()
    }

    private fun normalizarSucursal(sucursal: String): String =
        sucursal.trim().lowercase(Locale.ROOT).replace(" ", "_")

    override suspend fun registrarGastoValidado(monto: Double, motivo: String, sucursal: String, usuarioId: String): Boolean {
        return try {
            val id = db.collection(FirestoreCollections.GASTOS).document().id
            val gasto = GastoV2(
                id = id,
                descripcion = motivo,
                monto = monto,
                categoria = "Alimentos Empleado",
                fecha = System.currentTimeMillis(),
                sucursal = sucursal.lowercase(),
                usuarioId = usuarioId
            )
            db.collection(FirestoreCollections.GASTOS).document(id).set(gasto).await()
            true
        } catch (e: Exception) {
            false
        }
    }
}



