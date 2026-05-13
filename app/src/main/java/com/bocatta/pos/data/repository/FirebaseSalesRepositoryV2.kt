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

class FirebaseSalesRepositoryV2 : SalesRepository {
    private val db = FirebaseFirestoreProvider.db

    override suspend fun finalizarVentaConInventario(
        carrito: List<ItemCarritoV2>,
        sucursal: String,
        usuarioNombre: String,
        clienteSeleccionado: ClienteV2?,
        descuentoLealtad: Double,
        metodoPagoSeleccionado: String,
        esConsumoEmpleado: Boolean
    ): ResultadoVenta {
        val sucursalId = sucursal.lowercase()
        val subtotal = carrito.sumOf { it.precioFinal.toDouble() * it.cantidad }
        val totalFinal = if (esConsumoEmpleado) 0.0 else (subtotal - descuentoLealtad).coerceAtLeast(0.0)

        return db.runTransaction { transaction ->
            val contadorRef = db.collection(FirestoreCollections.CONFIGURACION).document("contadores_$sucursalId")
            val counterDoc = transaction.get(contadorRef)
            if (!counterDoc.exists()) throw IllegalStateException("Contador no inicializado")
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
            val stockDocs = deducciones.keys.associateWith { insumoId ->
                transaction.get(branchRef.document("${sucursalId}_$insumoId"))
            }
            deducciones.forEach { (insumoId, requerido) ->
                val snap = stockDocs[insumoId]
                val actual = snap?.getDouble("cantidadEnBase") ?: snap?.getDouble("cantidadDisponible") ?: 0.0
                if (actual < requerido) {
                    throw IllegalStateException("Stock insuficiente para $insumoId")
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
                    deducciones = deduccionesPorLinea[item.cartId] ?: emptyMap()
                )
            }
            val productosIds = carrito.flatMap { item -> List(item.cantidad) { item.producto.id } }

            transaction.set(contadorRef, mapOf("ultimo_ticket" to nextTicket), SetOptions.merge())
            transaction.set(
                db.collection(FirestoreCollections.VENTAS).document(ventaId),
                mapOf(
                    "id" to ventaId,
                    "ticket" to nextTicket,
                    "numeroTicket" to nextTicket,
                    "codigoTicket" to codigoTicket,
                    "total" to totalFinal,
                    "descuentoLealtad" to descuentoLealtad,
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
                val stockRef = branchRef.document("${sucursalId}_$insumoId")
                transaction.set(
                    stockRef,
                    mapOf(
                        "id" to "${sucursalId}_$insumoId",
                        "insumoId" to insumoId,
                        "sucursal" to sucursalId,
                        "cantidadEnBase" to FieldValue.increment(-cantidad),
                        "ultimaActualizacion" to System.currentTimeMillis()
                    ),
                    SetOptions.merge()
                )
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


