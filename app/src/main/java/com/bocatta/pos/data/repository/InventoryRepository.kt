package com.bocatta.pos.data.repository

import com.bocatta.pos.data.local.OfflineDatabase
import com.bocatta.pos.domain.model.IngredienteReceta
import com.bocatta.pos.domain.model.InsumoV2
import com.bocatta.pos.domain.model.ItemCarritoV2
import com.bocatta.pos.domain.model.RecetaV2
import com.bocatta.pos.domain.model.RegistroCompraV2
import com.google.firebase.firestore.FieldValue
import com.bocatta.pos.network.firebase.FirebaseFirestoreProvider
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Transaction
import com.google.firebase.firestore.WriteBatch
import kotlinx.coroutines.tasks.await
import com.bocatta.pos.core.constants.FirestoreCollections
import java.math.BigDecimal
import timber.log.Timber

import com.bocatta.pos.domain.model.StockAdjustmentEntity
import com.bocatta.pos.domain.repository.IStockAdjustmentQueue
import com.bocatta.pos.data.queue.SQLiteStockAdjustmentQueue

class InventoryRepository(
    private val offlineDb: OfflineDatabase? = null,
    private val adjustmentQueue: IStockAdjustmentQueue? = null
) {
    private val firestore = FirebaseFirestoreProvider.db

    suspend fun registrarCompra(
        insumoId: String,
        proveedorId: String,
        cantidadCompra: Double,
        precioUnitario: Double,
        precioTotal: Double,
        cantidadUsoTotal: Double,
        sucursal: String
    ): Boolean {
        return try {
            val compraId = firestore.collection(FirestoreCollections.COMPRAS).document().id
            Timber.tag("PURCHASE").i("Iniciando registro compra $compraId para insumo $insumoId, cantidadUsoTotal=$cantidadUsoTotal")
            val registro = RegistroCompraV2(
                id = compraId,
                insumoId = insumoId,
                proveedorId = proveedorId,
                cantidadComprada = cantidadCompra,
                precioUnitarioCompra = precioUnitario,
                precioTotal = precioTotal,
                fecha = System.currentTimeMillis(),
                sucursalRecibe = sucursal
            )
            val batch = firestore.batch()
            batch.set(firestore.collection(FirestoreCollections.COMPRAS).document(compraId), registro)
            batch.set(
                firestore.collection(FirestoreCollections.INSUMOS).document(insumoId),
                mapOf("costoUnitarioBase" to precioUnitario),
                SetOptions.merge()
            )
            batch.set(
                firestore.collection(FirestoreCollections.INVENTARIO_GLOBAL).document(insumoId),
                mapOf(
                    "id" to insumoId,
                    "insumoId" to insumoId,
                    "cantidadEnBase" to FieldValue.increment(cantidadUsoTotal),
                    "ultimaActualizacion" to System.currentTimeMillis()
                ),
                SetOptions.merge()
            )

            val sucursalLower = sucursal.trim().lowercase(java.util.Locale.getDefault())
            if (sucursalLower.isNotBlank() && sucursalLower != "global") {
                batch.set(
                    firestore.collection(FirestoreCollections.INVENTARIO_SUCURSAL).document("${sucursalLower}_$insumoId"),
                    mapOf(
                        "id" to "${sucursalLower}_$insumoId",
                        "insumoId" to insumoId,
                        "productId" to insumoId,
                        "sucursal" to sucursalLower,
                        "branchId" to sucursalLower,
                        "cantidadEnBase" to FieldValue.increment(cantidadUsoTotal),
                        "cantidadDisponible" to FieldValue.increment(cantidadUsoTotal),
                        "currentQty" to FieldValue.increment(cantidadUsoTotal),
                        "ultimaActualizacion" to System.currentTimeMillis()
                    ),
                    SetOptions.merge()
                )
            }

            registrarMovimiento(batch, "compra", insumoId, cantidadUsoTotal, if (sucursalLower.isNotBlank()) sucursalLower else "global", proveedorId, compraId)
            batch.commit().await()

            if (offlineDb != null && sucursalLower.isNotBlank() && sucursalLower != "global") {
                try {
                    addStock(insumoId, cantidadUsoTotal)
                } catch (ex: Exception) {
                    Timber.tag("PURCHASE").e(ex, "Error al actualizar stock local SQLite")
                }
            }

            Timber.tag("PURCHASE").i("Compra $compraId registrada con éxito")
            true
        } catch (e: Exception) {
            Timber.tag("PURCHASE").e(e, "Error al registrar compra")
            false
        }
    }

    fun obtenerIngredientesReceta(recetaId: String): List<IngredienteReceta> {
        val db = offlineDb?.readableDatabase ?: return emptyList()
        val list = mutableListOf<IngredienteReceta>()
        db.query(
            OfflineDatabase.TABLE_INGREDIENTES_RECETA,
            null,
            "recetaId = ?",
            arrayOf(recetaId),
            null,
            null,
            null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(
                    IngredienteReceta(
                        insumoId = cursor.getString(cursor.getColumnIndexOrThrow("insumoId")),
                        nombreInsumo = cursor.getString(cursor.getColumnIndexOrThrow("nombreInsumo")),
                        cantidad = cursor.getDouble(cursor.getColumnIndexOrThrow("cantidad")),
                        unidad = cursor.getString(cursor.getColumnIndexOrThrow("unidad"))
                    )
                )
            }
        }
        return list
    }

    fun deductStock(insumoId: String, cantidad: Double) {
        Timber.tag("INVENTORY").d("Deduciendo $cantidad de $insumoId")
        val db = offlineDb?.writableDatabase ?: return
        db.execSQL(
            "UPDATE ${OfflineDatabase.TABLE_INSUMOS} SET cantidadEnBase = cantidadEnBase - ? WHERE id = ?",
            arrayOf<Any>(cantidad, insumoId)
        )
    }

    fun addStock(insumoId: String, cantidad: Double) {
        Timber.tag("INVENTORY").d("Añadiendo $cantidad a $insumoId")
        val db = offlineDb?.writableDatabase ?: return
        db.execSQL(
            "UPDATE ${OfflineDatabase.TABLE_INSUMOS} SET cantidadEnBase = cantidadEnBase + ? WHERE id = ?",
            arrayOf<Any>(cantidad, insumoId)
        )
    }

    fun validarVentaCompleta(
        productoId: String,
        recetaId: String?,
        toppings: List<String>,
        base: String? = null,
        aderezos: List<String> = emptyList(),
        esSeparado: Boolean = false,
        cantidad: Int = 1
    ): Boolean {
        val valido = validarStockLocal(calcularDeduccionesVentaOffline(productoId, recetaId, toppings, base, aderezos, esSeparado, cantidad))
        Timber.tag("INVENTORY").i("Validación de venta completa para $productoId: $valido")
        return valido
    }

    fun procesarVentaCompleta(
        productoId: String,
        recetaId: String?,
        toppings: List<String>,
        cantidad: Int = 1
    ): Boolean {
        Timber.tag("INVENTORY").i("Procesando venta completa para $productoId con $cantidad unidades")
        return descontarVentaCompleta(productoId, recetaId, toppings, null, emptyList(), false, cantidad)
    }

    fun descontarVentaCompleta(
        productoId: String,
        recetaId: String?,
        toppings: List<String>,
        base: String? = null,
        aderezos: List<String> = emptyList(),
        esSeparado: Boolean = false,
        cantidad: Int = 1,
        componentes: List<ItemCarritoV2> = emptyList()
    ): Boolean {
        val deducciones = calcularDeduccionesVentaOffline(productoId, recetaId, toppings, base, aderezos, esSeparado, cantidad, componentes)
        if (!validarStockLocal(deducciones)) {
            Timber.tag("INVENTORY").w("Stock insuficiente para $productoId")
            return false
        }
        deducciones.forEach { (insumoId, cant) -> deductStock(insumoId, cant) }
        Timber.tag("INVENTORY").i("Venta completada para $productoId, deducciones aplicadas")
        return true
    }

    fun calcularDeduccionesItemOffline(item: ItemCarritoV2): Map<String, Double> {
        return calcularDeduccionesVentaOffline(
            productoId = item.producto.id,
            recetaId = item.producto.recetaId,
            toppings = item.toppings,
            base = item.base,
            aderezos = item.aderezos,
            esSeparado = item.esSeparado,
            cantidad = item.cantidad,
            componentes = item.componentesCombo
        )
    }

    private fun calcularDeduccionesVentaOffline(
        productoId: String,
        recetaId: String?,
        toppings: List<String>,
        base: String?,
        aderezos: List<String>,
        esSeparado: Boolean,
        cantidad: Int,
        componentes: List<ItemCarritoV2> = emptyList()
    ): Map<String, Double> {
        val db = offlineDb ?: return emptyMap()
        val producto = db.obtenerProductoPorId(productoId) ?: return emptyMap()
        val receta = recetaId?.let { db.obtenerRecetaPorId(it) }
        val item = ItemCarritoV2(
            producto = producto,
            precioFinal = BigDecimal.ZERO,
            cantidad = cantidad,
            base = base,
            aderezos = aderezos,
            toppings = toppings,
            esSeparado = esSeparado,
            componentesCombo = componentes
        )
        return InventoryDeductions.calcularParaItem(item, receta?.ingredientes ?: emptyList())
    }

    private fun validarStockLocal(deducciones: Map<String, Double>): Boolean {
        val db = offlineDb ?: return false
        val stockActual = db.obtenerInsumos().associate { it.id to it.cantidadEnBase }
        return deducciones.all { (insumoId, requerido) -> (stockActual[insumoId] ?: 0.0) >= requerido }
    }

    suspend fun registrarProduccion(
        insumoId: String,
        porcionesObtenidas: Double,
        tandasPreparadas: Double,
        sobranteAnterior: Double,
        sucursal: String
    ): Boolean {
        return try {
            val sucursalId = sucursal.lowercase(java.util.Locale.getDefault())
            val produccionId = firestore.collection(FirestoreCollections.MOVIMIENTOS_INVENTARIO).document().id
            firestore.runTransaction { transaction ->
                val now = System.currentTimeMillis()
                val receta = recetaProduccionIdPara(insumoId)?.let { recetaId ->
                    val recetaRef = firestore.collection(FirestoreCollections.RECETAS_PRODUCCION).document(recetaId)
                    transaction.get(recetaRef).toObject(RecetaV2::class.java)
                }
                val deducciones = receta?.ingredientes
                    ?.fold(linkedMapOf<String, Double>()) { acc, ing ->
                        val cantidadBase = InventoryDeductions.convertirAUnidadBase(ing.cantidad, ing.unidad) * tandasPreparadas
                        acc[ing.insumoId] = (acc[ing.insumoId] ?: 0.0) + cantidadBase
                        acc
                    }
                    ?: emptyMap()

                val stockRefs = deducciones.keys.associateWith {
                    firestore.collection(FirestoreCollections.INVENTARIO_GLOBAL).document(it)
                }
                val stockSnapshots = stockRefs.mapValues { (_, ref) -> transaction.get(ref) }
                deducciones.forEach { (ingId, requerido) ->
                    val snap = stockSnapshots.getValue(ingId)
                    val disponible = snap.getDouble("cantidadEnBase")
                        ?: snap.getDouble("cantidadDisponible")
                        ?: 0.0
                    if (disponible < requerido) {
                        throw IllegalStateException("Stock insuficiente en bodega para $ingId: $disponible < $requerido")
                    }
                }

                val finishedRef = firestore.collection(FirestoreCollections.INVENTARIO_GLOBAL)
                    .document(insumoId)
                transaction.set(
                    finishedRef,
                    mapOf(
                        "id" to insumoId,
                        "insumoId" to insumoId,
                        "cantidadEnBase" to FieldValue.increment(porcionesObtenidas),
                        "sobranteAnterior" to sobranteAnterior,
                        "ultimaActualizacion" to now
                    ),
                    SetOptions.merge()
                )

                deducciones.forEach { (ingId, cant) ->
                    transaction.set(
                        stockRefs.getValue(ingId),
                        mapOf(
                            "id" to ingId,
                            "insumoId" to ingId,
                            "cantidadEnBase" to FieldValue.increment(-cant),
                            "ultimaActualizacion" to now
                        ),
                        SetOptions.merge()
                    )
                    registrarMovimiento(transaction, "consumo_produccion", ingId, -cant, "global", "sistema", produccionId, now)
                }

                registrarMovimiento(transaction, "produccion", insumoId, porcionesObtenidas, "global", "sistema", produccionId, now)
                null
            }.await()
            Timber.tag("PRODUCTION").i("Produccion registrada: $insumoId +$porcionesObtenidas en bodega central")
            true
        } catch (e: Exception) {
            Timber.tag("PRODUCTION").e(e, "Error al registrar produccion")
            false
        }
    }

    suspend fun registrarCierreDiario(
        sobrantes: Map<String, Double>,
        stockSistema: Map<String, Double>,
        sucursal: String
    ): Boolean {
        return try {
            val sucursalId = sucursal.lowercase(java.util.Locale.getDefault())
            val batch = firestore.batch()
            val cierreId = firestore.collection(FirestoreCollections.INVENTORY_CLOSURES).document().id
            batch.set(
                firestore.collection(FirestoreCollections.INVENTORY_CLOSURES).document(cierreId),
                mapOf(
                    "id" to cierreId,
                    "sucursal" to sucursalId,
                    "branchId" to sucursalId,
                    "sobrantes" to sobrantes,
                    "stockSistema" to stockSistema,
                    "inventarioFinal" to sobrantes,
                    "usuario" to "sistema",
                    "usuarioId" to "sistema",
                    "fecha" to System.currentTimeMillis()
                )
            )
            sobrantes.forEach { (insumoId, conteo) ->
                val diferencia = conteo - (stockSistema[insumoId] ?: 0.0)
                batch.set(
                    firestore.collection(FirestoreCollections.INVENTARIO_SUCURSAL).document("${sucursalId}_$insumoId"),
                    mapOf(
                        "cantidadEnBase" to conteo,
                        "cantidadDisponible" to conteo,
                        "currentQty" to conteo,
                        "ultimaActualizacion" to System.currentTimeMillis()
                    ),
                    SetOptions.merge()
                )
                if (diferencia != 0.0) registrarMovimiento(batch, "cierre", insumoId, diferencia, sucursalId, "sistema", cierreId)
            }
            batch.commit().await()
            Timber.tag("CLOSURE").i("Cierre diario registrado para $sucursalId")
            true
        } catch (e: Exception) {
            Timber.tag("CLOSURE").e(e, "Error al registrar cierre diario")
            false
        }
    }

    suspend fun registrarAperturaInventario(
        conteos: Map<String, Double>,
        stockSistema: Map<String, Double>,
        usuarioId: String,
        sucursal: String
    ): Boolean {
        return try {
            val sucursalId = sucursal.lowercase(java.util.Locale.getDefault())
            val batch = firestore.batch()
            val aperturaId = firestore.collection(FirestoreCollections.INVENTORY_OPENINGS).document().id
            batch.set(
                firestore.collection(FirestoreCollections.INVENTORY_OPENINGS).document(aperturaId),
                mapOf(
                    "id" to aperturaId,
                    "sucursal" to sucursalId,
                    "branchId" to sucursalId,
                    "conteos" to conteos,
                    "usuarioId" to usuarioId,
                    "usuario" to usuarioId,
                    "fecha" to System.currentTimeMillis()
                )
            )
            conteos.forEach { (insumoId, conteo) ->
                val diferencia = conteo - (stockSistema[insumoId] ?: 0.0)
                batch.set(
                    firestore.collection(FirestoreCollections.INVENTARIO_SUCURSAL).document("${sucursalId}_$insumoId"),
                    mapOf(
                        "id" to "${sucursalId}_$insumoId",
                        "insumoId" to insumoId,
                        "productId" to insumoId,
                        "sucursal" to sucursalId,
                        "branchId" to sucursalId,
                        "cantidadEnBase" to conteo,
                        "cantidadDisponible" to conteo,
                        "currentQty" to conteo,
                        "ultimaActualizacion" to System.currentTimeMillis()
                    ),
                    SetOptions.merge()
                )
                registrarMovimiento(batch, "apertura", insumoId, diferencia, sucursalId, usuarioId, aperturaId)
            }
            batch.commit().await()
            Timber.tag("OPENING").i("Apertura de inventario registrada: $aperturaId")
            true
        } catch (e: Exception) {
            Timber.tag("OPENING").e(e, "Error al registrar apertura de inventario")
            false
        }
    }

    suspend fun registrarMermaManual(
        insumoId: String,
        cantidad: Double,
        motivo: String,
        usuarioId: String,
        sucursal: String
    ): Boolean {
        return try {
            val sucursalId = sucursal.lowercase(java.util.Locale.getDefault())
            val batch = firestore.batch()
            val mermaId = firestore.collection(FirestoreCollections.MERMA_LOGS).document().id
            batch.set(
                firestore.collection(FirestoreCollections.MERMA_LOGS).document(mermaId),
                mapOf("id" to mermaId, "insumoId" to insumoId, "cantidad" to cantidad, "motivo" to motivo, "usuarioId" to usuarioId, "sucursal" to sucursalId, "fecha" to System.currentTimeMillis())
            )
            batch.set(
                firestore.collection(FirestoreCollections.INVENTARIO_SUCURSAL).document("${sucursalId}_$insumoId"),
                mapOf(
                    "cantidadEnBase" to FieldValue.increment(-cantidad),
                    "cantidadDisponible" to FieldValue.increment(-cantidad),
                    "currentQty" to FieldValue.increment(-cantidad),
                    "ultimaActualizacion" to System.currentTimeMillis()
                ),
                SetOptions.merge()
            )
            registrarMovimiento(batch, "merma", insumoId, -cantidad, sucursalId, usuarioId, mermaId)
            batch.commit().await()
            Timber.tag("MERMA").i("Merma registrada: $insumoId -$cantidad por $motivo")
            true
        } catch (e: Exception) {
            Timber.tag("MERMA").e(e, "Error al registrar merma manual")
            false
        }
    }

    private fun registrarMovimiento(
        batch: WriteBatch,
        tipo: String,
        insumoId: String,
        cantidad: Double,
        sucursal: String,
        usuarioId: String,
        referenciaId: String?
    ) {
        val ref = firestore.collection(FirestoreCollections.MOVIMIENTOS_INVENTARIO).document()
        batch.set(
            ref,
            mapOf(
                "id" to ref.id,
                "tipo" to tipo,
                "type" to tipo.uppercase(java.util.Locale.getDefault()),
                "insumoId" to insumoId,
                "productId" to insumoId,
                "cantidadEnBase" to cantidad,
                "quantity" to cantidad,
                "sucursal" to sucursal.lowercase(java.util.Locale.getDefault()),
                "branchId" to sucursal.lowercase(java.util.Locale.getDefault()),
                "usuarioId" to usuarioId,
                "userId" to usuarioId,
                "referenciaId" to referenciaId,
                "fecha" to System.currentTimeMillis(),
                "timestamp" to System.currentTimeMillis()
            )
        )
    }

    private fun registrarMovimiento(
        transaction: Transaction,
        tipo: String,
        insumoId: String,
        cantidad: Double,
        sucursal: String,
        usuarioId: String,
        referenciaId: String?,
        fecha: Long = System.currentTimeMillis()
    ) {
        val ref = firestore.collection(FirestoreCollections.MOVIMIENTOS_INVENTARIO).document()
        transaction.set(
            ref,
            mapOf(
                "id" to ref.id,
                "tipo" to tipo,
                "type" to tipo.uppercase(java.util.Locale.getDefault()),
                "insumoId" to insumoId,
                "productId" to insumoId,
                "cantidadEnBase" to cantidad,
                "quantity" to cantidad,
                "sucursal" to sucursal.lowercase(java.util.Locale.getDefault()),
                "branchId" to sucursal.lowercase(java.util.Locale.getDefault()),
                "usuarioId" to usuarioId,
                "userId" to usuarioId,
                "referenciaId" to referenciaId,
                "fecha" to fecha,
                "timestamp" to fecha
            )
        )
    }

    private fun recetaProduccionIdPara(insumoId: String): String? = when (insumoId) {
        "masa_crepa" -> "receta_masa_crepa"
        "carlota_unidad" -> "receta_carlota"
        "tiramisu_unidad" -> "receta_tiramisu"
        "fresas_crema_unidad" -> "receta_fresas_crema"
        "duraznos_crema_unidad" -> "receta_duraznos_crema"
        else -> null
    }
}
