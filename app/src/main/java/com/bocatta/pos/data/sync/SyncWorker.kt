package com.bocatta.pos.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.bocatta.pos.data.local.OfflineDatabase
import com.bocatta.pos.data.local.VentaOffline
import com.bocatta.pos.data.local.OperacionOffline
import com.bocatta.pos.data.repository.StockAllocationRepository
import com.bocatta.pos.domain.model.StockAdjustmentEntity
import com.bocatta.pos.domain.repository.IInventoryRepository
import com.bocatta.pos.domain.repository.IStockAdjustmentQueue
import com.bocatta.pos.domain.model.SyncError
import com.bocatta.pos.domain.repository.ISyncErrorRepository
import com.bocatta.pos.network.firebase.FirebaseFirestoreProvider
import com.bocatta.pos.core.constants.FirestoreCollections
import com.bocatta.pos.domain.model.MovementV2
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.io.IOException
import com.bocatta.pos.domain.model.ItemCarritoV2
import com.bocatta.pos.domain.model.SalesInventoryProductV2
import com.bocatta.pos.domain.model.IngredienteReceta
import com.bocatta.pos.data.repository.InventoryDeductions

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {
    private val db = FirebaseFirestoreProvider.db
    private val adjustmentQueue: IStockAdjustmentQueue by inject()
    private val inventoryRepo: IInventoryRepository by inject()
    private val syncErrorRepo: ISyncErrorRepository by inject()
    private val allocationRepo = StockAllocationRepository()


    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        if (!isNetworkAvailable()) {
            return@withContext Result.retry()
        }

        // Check max retries from input data
        val maxRetries = inputData.getInt("max_retries", 3)
        if (runAttemptCount > maxRetries) {
            Timber.tag("SYNC_WORKER").e("Max retries ($maxRetries) exceeded. Reporting error.")
            
            val failedAdjustments = adjustmentQueue.getAllPending()
            syncErrorRepo.reportError(
                SyncError(
                    deviceId = android.os.Build.ID,
                    timestamp = System.currentTimeMillis(),
                    errorMessage = "Max retries exceeded for offline sync",
                    failedIds = failedAdjustments.mapNotNull { it.id },
                    appVersion = android.os.Build.VERSION.SDK_INT.toString(), // Simplified version
                    stackTrace = "WorkManager runAttemptCount: $runAttemptCount"
                )
            )
            return@withContext Result.failure()
        }

        val database = OfflineDatabase.getInstance(applicationContext)
        val ventasPendientes = database.obtenerVentasPendientes()
        val operacionesPendientes = database.obtenerOperacionesPendientes()

        if (ventasPendientes.isEmpty() && operacionesPendientes.isEmpty()) {
            // Still check if there are stock adjustments
            val pendingAdjustments = adjustmentQueue.getAllPending()
            if (pendingAdjustments.isEmpty()) return@withContext Result.success()
        }

        var ventasSincronizadas = 0
        val ahora = System.currentTimeMillis()

        for (venta in ventasPendientes) {
            if (venta.intentos >= VentaOffline.MAX_INTENTOS) {
                database.marcarVentaFallidaCritica(venta.id, ahora)
                reportCriticalSaleSyncError(
                    venta = venta,
                    error = null,
                    reason = "Max sale sync attempts exceeded before retry"
                )
                continue
            }

            try {
                sincronizarVenta(venta)
                database.marcarVentaSincronizada(venta.id, ahora)
                ventasSincronizadas++
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e

                when (classifySaleSyncFailure(e)) {
                    SaleSyncFailure.CRITICAL -> {
                        database.marcarVentaFallidaCritica(venta.id, ahora)
                        reportCriticalSaleSyncError(
                            venta = venta,
                            error = e,
                            reason = "Critical sale sync failure"
                        )
                    }
                    SaleSyncFailure.TRANSIENT -> {
                        val nextAttempt = venta.intentos + 1
                        if (nextAttempt >= VentaOffline.MAX_INTENTOS) {
                            database.marcarVentaFallidaCritica(venta.id, ahora)
                            reportCriticalSaleSyncError(
                                venta = venta,
                                error = e,
                                reason = "Transient sale sync failure reached max attempts"
                            )
                        } else {
                            database.registrarIntentoVentaFallido(venta.id, ahora)
                            Timber.tag("SYNC_WORKER").w(
                                e,
                                "Transient sale sync failure for ${venta.id}. Attempt $nextAttempt/${VentaOffline.MAX_INTENTOS}. Stopping queue sync."
                            )
                        }
                        break // Interrupt the loop immediately to avoid burning other sales
                    }
                }
            }
        }

        // -- Process pending stock adjustments (Two-Phase Commit) ------------------
        val pending = adjustmentQueue.getAllPending()
        if (pending.isNotEmpty()) {
            Timber.tag("SYNC_WORKER").i("SYNC_STARTED: ${pending.size} pending adjustments")
            val pendingIds = pending.mapNotNull { it.id }
            adjustmentQueue.markAsSyncing(pendingIds)

            val processedIds = mutableListOf<Long>()
            try {
                for (adj in pending) {
                    val success = inventoryRepo.syncOfflineAdjustment(adj)
                    if (success) adj.id?.let { processedIds.add(it) }
                }
            } finally {
                adjustmentQueue.deleteProcessed(processedIds)
                val failedIds = pendingIds - processedIds.toSet()
                if (failedIds.isNotEmpty()) {
                    adjustmentQueue.resetSyncingState(failedIds)
                    Timber.tag("SYNC_WORKER").w("SYNC_FAILED_IDS=${failedIds.joinToString()}")
                }
            }
            Timber.tag("SYNC_WORKER").i("SYNC_SUCCESS_COUNT=${processedIds.size}")
        }

        // -- Process pending offline operations (devoluciones, cancelaciones, mermas) -----
        for (op in operacionesPendientes) {
            if (op.intentos >= OperacionOffline.MAX_INTENTOS) continue
            try {
                sincronizarOperacion(op)
                database.marcarOperacionSincronizada(op.id)
            } catch (e: Exception) {
                database.marcarOperacionFallida(op.id)
                Timber.tag("SYNC_WORKER").e(e, "Error syncing operation ${op.id}")
            }
        }

        val pendientesRestantes = database.contarPendientes() + database.obtenerOperacionesPendientes().size
        val queuePending = adjustmentQueue.getAllPending().size
        if (pendientesRestantes == 0 && queuePending == 0) Result.success() else Result.retry()
    }

    private suspend fun sincronizarVenta(venta: VentaOffline) {
        val ventaData = mapOf(
            "id" to venta.id,
            "ticket" to venta.ticket,
            "numeroTicket" to venta.ticket,
            "codigoTicket" to venta.codigoTicket,
            "total" to venta.total,
            "descuentoLealtad" to venta.descuentoLealtad,
            "fecha" to venta.fecha,
            "sucursal" to venta.sucursal,
            "atendio" to venta.atendio,
            "metodoPago" to venta.metodoPago,
            "esConsumoEmpleado" to venta.esConsumoEmpleado,
            "estado" to "completada",
            "clienteId" to venta.clienteId,
            "offlineSync" to true,
            "syncTimestamp" to System.currentTimeMillis()
        )

        val items = JSONArray(venta.carritoJson)
        val sucursalId = venta.sucursal.lowercase()

        db.runTransaction { transaction ->
            // --- IDEMPOTENCY CHECK ---
            val ventaRef = db.collection(FirestoreCollections.VENTAS).document(venta.id)
            val existingSnap = transaction.get(ventaRef)
            if (existingSnap.exists()) {
                Timber.tag("SYNC_WORKER").w("Sale ${venta.id} already exists in Firestore. Skipping stock deduction to ensure idempotency.")
                return@runTransaction null
            }

            val deducciones = linkedMapOf<String, Double>()
            val deduccionesPorLinea = mutableMapOf<Int, Map<String, Double>>()
            val parsedItems = mutableListOf<ItemCarritoV2>()

            for (i in 0 until items.length()) {
                val jsonItem = items.getJSONObject(i)
                val item = parseItemCarrito(jsonItem)
                parsedItems.add(item)

                val recetaId = item.producto.recetaId
                val ingredientesMapList = recetaId?.let {
                    val recetaSnap = transaction.get(db.collection(FirestoreCollections.RECETAS).document(it))
                    @Suppress("UNCHECKED_CAST")
                    recetaSnap.get("ingredientes") as? List<Map<String, Any?>> ?: emptyList()
                } ?: emptyList()

                val recetaIngredientes = ingredientesMapList.map { ing ->
                    IngredienteReceta(
                        insumoId = ing["insumoId"] as? String ?: "",
                        nombreInsumo = ing["nombreInsumo"] as? String ?: "",
                        cantidad = (ing["cantidad"] as? Number)?.toDouble() ?: 0.0,
                        unidad = ing["unidad"] as? String ?: "g"
                    )
                }

                val lineDeductions = InventoryDeductions.calcularParaItem(item, recetaIngredientes)
                deduccionesPorLinea[i] = lineDeductions

                lineDeductions.forEach { (insumoId, cantidad) ->
                    deducciones[insumoId] = (deducciones[insumoId] ?: 0.0) + cantidad
                }
            }

            val idsConCuotaSucursal = allocationRepo.itemsVendibles.toSet()
            val idsFisicosSucursal = allocationRepo.itemsFisicos.toSet()
            val branchDocs = deducciones
                .filterKeys { it in idsConCuotaSucursal }
                .keys
                .associateWith { insumoId ->
                    transaction.get(db.collection(FirestoreCollections.INVENTARIO_SUCURSAL).document("${sucursalId}_$insumoId"))
                }
            val globalDocs = deducciones
                .filterKeys { it !in idsFisicosSucursal }
                .keys
                .associateWith { insumoId ->
                    transaction.get(db.collection(FirestoreCollections.INVENTARIO_GLOBAL).document(insumoId))
                }
            deducciones.forEach { (insumoId, requerido) ->
                if (insumoId in idsConCuotaSucursal) {
                    val snap = branchDocs[insumoId]
                    val actual = snap?.getDouble("cantidadEnBase") ?: snap?.getDouble("cantidadDisponible") ?: 0.0
                    if (actual < requerido) {
                        Timber.tag("SYNC_WORKER").w("Stock insuficiente en nube para $insumoId en sucursal. Disponible: $actual, Requerido: $requerido. Registrando venta igualmente.")
                    }
                }
                if (insumoId !in idsFisicosSucursal) {
                    val snap = globalDocs[insumoId]
                    val actual = snap?.getDouble("cantidadEnBase") ?: snap?.getDouble("cantidadDisponible") ?: 0.0
                    if (actual < requerido) {
                        Timber.tag("SYNC_WORKER").w("Stock insuficiente en nube para $insumoId en bodega central. Disponible: $actual, Requerido: $requerido. Registrando venta igualmente.")
                    }
                }
            }

            val productos = mutableListOf<Map<String, Any?>>()
            val productosIds = mutableListOf<String>()

            fun itemToMap(item: ItemCarritoV2, lineDeductions: Map<String, Double>): Map<String, Any?> {
                return mapOf(
                    "productoId" to item.producto.id,
                    "nombre" to item.nombre,
                    "cantidad" to item.cantidad,
                    "precioUnitario" to item.precioFinal.toDouble(),
                    "base" to item.base,
                    "aderezos" to item.aderezos,
                    "toppings" to item.toppings,
                    "separadas" to item.esSeparado,
                    "paraLlevar" to item.paraLlevar,
                    "cantidadGramos" to item.cantidadGramos,
                    "recetaId" to item.producto.recetaId,
                    "deducciones" to lineDeductions,
                    "componentesCombo" to item.componentesCombo.map { comp ->
                        itemToMap(comp, emptyMap())
                    }
                )
            }

            for (i in parsedItems.indices) {
                val item = parsedItems[i]
                productosIds.addAll(List(item.cantidad) { item.producto.id })
                productos.add(itemToMap(item, deduccionesPorLinea[i] ?: emptyMap()))
            }

            transaction.set(db.collection(FirestoreCollections.VENTAS).document(venta.id), ventaData + mapOf("productos" to productos, "productosIds" to productosIds))
            deducciones.forEach { (insumoId, cantidad) ->
                if (insumoId in idsConCuotaSucursal) {
                    transaction.set(
                        db.collection(FirestoreCollections.INVENTARIO_SUCURSAL).document("${sucursalId}_$insumoId"),
                        mapOf(
                            "id" to "${sucursalId}_$insumoId",
                            "insumoId" to insumoId,
                            "sucursal" to sucursalId,
                            "cantidadEnBase" to FieldValue.increment(-cantidad),
                            "currentQty" to FieldValue.increment(-cantidad), // Escritura dual para compatibilidad
                            "ultimaActualizacion" to System.currentTimeMillis()
                        ),
                        SetOptions.merge()
                    )
                }
                if (insumoId !in idsFisicosSucursal) {
                    transaction.set(
                        db.collection(FirestoreCollections.INVENTARIO_GLOBAL).document(insumoId),
                        mapOf(
                            "id" to insumoId,
                            "insumoId" to insumoId,
                            "cantidadEnBase" to FieldValue.increment(-cantidad),
                            "currentQty" to FieldValue.increment(-cantidad), // Escritura dual para compatibilidad
                            "ultimaActualizacion" to System.currentTimeMillis()
                        ),
                        SetOptions.merge()
                    )
                }
            }
            null
        }.await()
    }

    private suspend fun sincronizarOperacion(operacion: com.bocatta.pos.data.local.OperacionOffline) {
        val operacionData = mapOf(
            "id" to operacion.id,
            "tipo" to operacion.tipo,
            "ventaId" to operacion.ventaId,
            "motivo" to operacion.motivo,
            "usuarioId" to operacion.usuarioId,
            "sucursal" to operacion.sucursal,
            "fecha" to operacion.fecha,
            "requiereAprobacion" to operacion.requiereAprobacion,
            "estado" to if (operacion.requiereAprobacion) "pendiente_aprobacion" else "sincronizada",
            "offlineSync" to true
        )

        val collection = when (operacion.tipo) {
            "devolucion" -> FirestoreCollections.DEVOLUCIONES
            "cancelacion" -> FirestoreCollections.CANCELACIONES
            "merma" -> FirestoreCollections.MERMA_LOGS
            else -> FirestoreCollections.OPERACIONES_GENERALES
        }

        db.collection(collection).document(operacion.id).set(operacionData).await()
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private enum class SaleSyncFailure {
        TRANSIENT,
        CRITICAL
    }

    private fun classifySaleSyncFailure(error: Exception): SaleSyncFailure {
        if (error is JSONException) return SaleSyncFailure.CRITICAL
        if (error is IllegalArgumentException) return SaleSyncFailure.CRITICAL
        if (error is IOException) return SaleSyncFailure.TRANSIENT

        var current: Throwable? = error
        var firestoreError: FirebaseFirestoreException? = null
        while (current != null && firestoreError == null) {
            if (current is FirebaseFirestoreException) {
                firestoreError = current
            }
            current = current.cause
        }

        return when (firestoreError?.code) {
            FirebaseFirestoreException.Code.UNAVAILABLE,
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED,
            FirebaseFirestoreException.Code.ABORTED,
            FirebaseFirestoreException.Code.CANCELLED,
            FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED -> SaleSyncFailure.TRANSIENT

            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            FirebaseFirestoreException.Code.UNAUTHENTICATED,
            FirebaseFirestoreException.Code.INVALID_ARGUMENT,
            FirebaseFirestoreException.Code.FAILED_PRECONDITION -> SaleSyncFailure.CRITICAL

            else -> SaleSyncFailure.TRANSIENT
        }
    }

    private fun parseItemCarrito(json: org.json.JSONObject): ItemCarritoV2 {
        val productoId = json.optString("productoId")
        val nombre = json.optString("nombre")
        val categoria = json.optString("categoria")
        val recetaId = json.optString("recetaId").takeIf { it.isNotBlank() }

        val componentesArray = json.optJSONArray("componentesCombo")
        val componentesList = mutableListOf<ItemCarritoV2>()
        if (componentesArray != null) {
            for (i in 0 until componentesArray.length()) {
                val compJson = componentesArray.getJSONObject(i)
                componentesList.add(parseItemCarrito(compJson))
            }
        }

        val producto = SalesInventoryProductV2(
            id = productoId,
            nombre = nombre,
            categoria = categoria,
            recetaId = recetaId,
            esCombo = componentesList.isNotEmpty()
        )

        val aderezosArray = json.optJSONArray("aderezos")
        val aderezosList = mutableListOf<String>()
        if (aderezosArray != null) {
            for (i in 0 until aderezosArray.length()) {
                aderezosList.add(aderezosArray.optString(i))
            }
        }

        val toppingsArray = json.optJSONArray("toppings")
        val toppingsList = mutableListOf<String>()
        if (toppingsArray != null) {
            for (i in 0 until toppingsArray.length()) {
                toppingsList.add(toppingsArray.optString(i))
            }
        }

        val base = json.optString("base").takeIf { it.isNotBlank() && it != "null" }
        val precio = java.math.BigDecimal.valueOf(json.optDouble("precio", 0.0))
        val cantidad = json.optDouble("cantidad", 1.0).toInt().coerceAtLeast(1)
        val esSeparado = json.optBoolean("esSeparado", false)
        val paraLlevar = json.optBoolean("paraLlevar", false)
        // -1.0 como sentinel: campo ausente en ventas antiguas -> null
        val cantidadGramosRaw = json.optDouble("cantidadGramos", -1.0)
        val cantidadGramos = if (cantidadGramosRaw > 0) cantidadGramosRaw else null

        return ItemCarritoV2(
            producto = producto,
            precioFinal = precio,
            cantidad = cantidad,
            nombre = nombre,
            base = base,
            aderezos = aderezosList,
            toppings = toppingsList,
            esSeparado = esSeparado,
            componentesCombo = componentesList,
            paraLlevar = paraLlevar,
            cantidadGramos = cantidadGramos
        )
    }

    private suspend fun reportCriticalSaleSyncError(
        venta: VentaOffline,
        error: Exception?,
        reason: String
    ) {
        val message = buildString {
            append(reason)
            append(" for sale ")
            append(venta.id)
            error?.message?.takeIf { it.isNotBlank() }?.let {
                append(": ")
                append(it)
            }
        }

        val reported = syncErrorRepo.reportError(
            SyncError(
                deviceId = android.os.Build.ID,
                timestamp = System.currentTimeMillis(),
                errorMessage = message,
                failedSaleIds = listOf(venta.id),
                appVersion = android.os.Build.VERSION.SDK_INT.toString(),
                stackTrace = error?.stackTraceToString()
            )
        )

        if (!reported) {
            Timber.tag("SYNC_WORKER").e(error, "Failed to report critical sale sync error for ${venta.id}")
        }
    }
}



