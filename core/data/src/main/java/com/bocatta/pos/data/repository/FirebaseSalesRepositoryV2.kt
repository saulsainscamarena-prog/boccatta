package com.bocatta.pos.data.repository

import com.bocatta.pos.core.model.TicketUtils
import com.bocatta.pos.core.constants.FirestoreCollections
import com.bocatta.pos.domain.model.ClienteV2
import com.bocatta.pos.domain.model.GastoV2
import com.bocatta.pos.domain.model.IngredienteReceta
import com.bocatta.pos.domain.model.ItemCarritoV2
import com.bocatta.pos.domain.model.ItemVendidoV2
import com.bocatta.pos.domain.model.VentaV2
import com.bocatta.pos.domain.repository.ResultadoVenta
import com.bocatta.pos.domain.repository.SalesRepository
import com.google.firebase.firestore.FieldValue
import com.bocatta.pos.network.firebase.FirebaseFirestoreProvider
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.Locale
import com.bocatta.pos.data.local.OfflineDatabase
import com.bocatta.pos.domain.usecase.TenantSessionManager

class FirebaseSalesRepositoryV2(
    private val offlineDb: OfflineDatabase,
    private val tenantManager: TenantSessionManager
) : SalesRepository {
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
        propina: Double,
        notaOrden: String,
        splitPartes: List<com.bocatta.pos.domain.model.SplitParte>,
        forcedVentaId: String?
    ): ResultadoVenta {
        val sucursalId = normalizarSucursal(sucursal)
        val subtotal = carrito.sumOf { it.precioFinal.toDouble() * it.cantidad }
        val totalSinPropina = (subtotal - descuentoLealtad - descuentoPromociones - descuentoManual).coerceAtLeast(0.0)
        val propinaFinal = if (esConsumoEmpleado) 0.0 else propina.coerceAtLeast(0.0)
        val totalFinal = if (esConsumoEmpleado) 0.0 else totalSinPropina + propinaFinal
        val totalParaLealtad = if (esConsumoEmpleado) 0.0 else totalSinPropina

        // 1. Resolver ingredientes de receta de manera local ultrarrápida (SQLite)
        val recetaIngredientes = carrito.associate { item ->
            val recetaId = item.producto.recetaId
            item.cartId to if (recetaId.isNullOrBlank()) {
                emptyList()
            } else {
                offlineDb.obtenerRecetaPorId(recetaId)?.ingredientes ?: emptyList()
            }
        }

        // 2. Calcular deducciones necesarias
        val deducciones = linkedMapOf<String, Double>()
        val deduccionesPorLinea = carrito.associate { item ->
            val linea = InventoryDeductions.calcularParaItem(item, recetaIngredientes[item.cartId] ?: emptyList())
            linea.forEach { (insumoId, cantidad) -> deducciones[insumoId] = (deducciones[insumoId] ?: 0.0) + cantidad }
            item.cartId to linea
        }

        val idsConCuotaSucursal = allocationRepo.itemsVendibles.toSet()
        val idsFisicosSucursal = allocationRepo.itemsFisicos.toSet()

        // 3. Pre-flight Check: Validar existencias localmente en SQLite antes de disparar la transacción Firebase
        deducciones.forEach { (insumoId, requerido) ->
            if (idsConCuotaSucursal.contains(insumoId)) {
                val localStock = offlineDb.obtenerStockInsumo(insumoId)
                if (localStock < requerido) {
                    throw IllegalStateException("Stock insuficiente local para $insumoId. Disponible: $localStock, requerido: $requerido")
                }
            }
        }

        // 4. Iniciar transacción en la nube sin realizar lecturas pesadas de recetas ni stocks (Latencia reducida)
        return db.runTransaction { transaction ->
            val contadorRef = db.collection(FirestoreCollections.CONFIGURACION).document("contadores_$sucursalId")
            val counterDoc = transaction.get(contadorRef)
            val nextTicket = (counterDoc.getLong("ultimo_ticket") ?: 0L) + 1
            val codigoTicket = TicketUtils.generarCodigoTicket(sucursalId, nextTicket)

            val ventaId = forcedVentaId ?: db.collection(FirestoreCollections.VENTAS).document().id
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
                "tenantId" to tenantManager.getTenantId(),
                "businessType" to tenantManager.getBusinessType(),
                "ticket" to nextTicket,
                "numeroTicket" to nextTicket,
                "codigoTicket" to codigoTicket,
                "total" to totalFinal,
                "descuentoLealtad" to descuentoLealtad,
                "descuentoPromociones" to descuentoPromociones,
                "descuentoManual" to descuentoManual,
                "propina" to propinaFinal,
                "notaOrden" to notaOrden.trim(),
                "fecha" to System.currentTimeMillis(),
                "sucursal" to sucursalId,
                "branchId" to sucursalId,
                "atendio" to usuarioNombre,
                "userId" to usuarioNombre,
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
                    transaction.update(clienteRef, "comprasCicloActual", listOf(totalParaLealtad))
                } else {
                    transaction.update(clienteRef, "comprasCicloActual", FieldValue.arrayUnion(totalParaLealtad))
                }
            }

            val branchRef = db.collection(FirestoreCollections.INVENTARIO_SUCURSAL)
            val globalRef = db.collection(FirestoreCollections.INVENTARIO_GLOBAL)

            // Aplicar decrementos atómicos en background (sin bloquear en transaction.get de stock)
            deducciones.forEach { (insumoId, cantidad) ->
                if (idsConCuotaSucursal.contains(insumoId)) {
                    val stockRef = branchRef.document("${sucursalId}_$insumoId")
                    transaction.set(
                        stockRef,
                        mapOf(
                            "id" to stockRef.id,
                            "insumoId" to insumoId,
                            "productId" to insumoId,
                            "sucursal" to sucursalId,
                            "branchId" to sucursalId,
                            "cantidadEnBase" to FieldValue.increment(-cantidad),
                            "cantidadDisponible" to FieldValue.increment(-cantidad),
                            "currentQty" to FieldValue.increment(-cantidad),
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
                            "productId" to insumoId,
                            "cantidadEnBase" to FieldValue.increment(-cantidad),
                            "cantidadDisponible" to FieldValue.increment(-cantidad),
                            "currentQty" to FieldValue.increment(-cantidad),
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
                        "type" to "SALE",
                        "insumoId" to insumoId,
                        "productId" to insumoId,
                        "cantidadEnBase" to -cantidad,
                        "quantity" to -cantidad,
                        "sucursal" to sucursalId,
                        "branchId" to sucursalId,
                        "referenciaId" to ventaId,
                        "fecha" to System.currentTimeMillis(),
                        "timestamp" to System.currentTimeMillis(),
                        "usuarioId" to usuarioNombre,
                        "userId" to usuarioNombre
                    )
                )
            }

            ResultadoVenta(numeroTicket = nextTicket, codigoTicket = codigoTicket)
        }.await()
    }

    override fun getActiveKdsOrders(sucursal: String): Flow<List<VentaV2>> = callbackFlow {
        val sucursalId = normalizarSucursal(sucursal)
        val registration = db.collection(FirestoreCollections.VENTAS)
            .whereEqualTo("branchId", sucursalId)
            .whereIn("estadoCocina", listOf("pendiente", "preparando"))
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.tag("KDS").e(error, "Error listening to KDS orders")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val orders = snapshot.documents.mapNotNull { doc ->
                        val data = doc.data ?: return@mapNotNull null
                        VentaV2(
                            id = doc.id,
                            tenantId = data["tenantId"] as? String ?: "",
                            businessType = data["businessType"] as? String ?: "",
                            ticket = (data["ticket"] as? Long) ?: 0L,
                            numeroTicket = (data["numeroTicket"] as? Long) ?: 0L,
                            codigoTicket = data["codigoTicket"] as? String ?: "",
                            total = (data["total"] as? Double) ?: 0.0,
                            descuentoLealtad = (data["descuentoLealtad"] as? Double) ?: 0.0,
                            propina = (data["propina"] as? Double) ?: 0.0,
                            notaOrden = data["notaOrden"] as? String ?: "",
                            fecha = (data["fecha"] as? Long) ?: 0L,
                            sucursal = data["sucursal"] as? String ?: sucursalId,
                            atendio = data["atendio"] as? String ?: "",
                            metodoPago = data["metodoPago"] as? String ?: "efectivo",
                            esConsumoEmpleado = data["esConsumoEmpleado"] as? Boolean ?: false,
                            estado = data["estado"] as? String ?: "completada",
                            clienteId = data["clienteId"] as? String?,
                            estadoCocina = data["estadoCocina"] as? String ?: ""
                        )
                    }
                    trySend(orders)
                }
            }
        awaitClose { registration.remove() }
    }

    override suspend fun updateKdsOrderStatus(ventaId: String, estado: String, sucursal: String): Boolean {
        return try {
            val sucursalId = normalizarSucursal(sucursal)
            db.collection(FirestoreCollections.VENTAS).document(ventaId)
                .update("estadoCocina", estado, "branchId", sucursalId)
                .await()
            true
        } catch (e: Exception) {
            Timber.tag("KDS").e(e, "Error updating KDS order status for $ventaId to $estado")
            false
        }
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
                sucursal = sucursal.lowercase(java.util.Locale.getDefault()),
                usuarioId = usuarioId
            )
            db.collection(FirestoreCollections.GASTOS).document(id).set(
                mapOf(
                    "id" to gasto.id,
                    "tenantId" to tenantManager.getTenantId(),
                    "businessType" to tenantManager.getBusinessType(),
                    "descripcion" to gasto.descripcion,
                    "concepto" to gasto.descripcion,
                    "monto" to gasto.monto,
                    "categoria" to gasto.categoria,
                    "fecha" to gasto.fecha,
                    "sucursal" to gasto.sucursal,
                    "usuarioId" to gasto.usuarioId,
                    "usuario" to gasto.usuarioId
                )
            ).await()
            true
        } catch (e: Exception) {
            Timber.tag("SALES").e(e, "Error registrando gasto validado: $motivo")
            false
        }
    }
}

