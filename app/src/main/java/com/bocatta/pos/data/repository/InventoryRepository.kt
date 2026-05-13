package com.bocatta.pos.data.repository

import com.bocatta.pos.data.local.OfflineDatabase
import com.bocatta.pos.domain.model.IngredienteReceta
import com.bocatta.pos.domain.model.InsumoV2
import com.bocatta.pos.domain.model.ItemCarritoV2
import com.bocatta.pos.domain.model.RegistroCompraV2
import com.google.firebase.firestore.FieldValue
import com.bocatta.pos.network.firebase.FirebaseFirestoreProvider
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.WriteBatch
import kotlinx.coroutines.tasks.await
import com.bocatta.pos.core.constants.FirestoreCollections
import java.math.BigDecimal
import timber.log.Timber

class InventoryRepository(private val offlineDb: OfflineDatabase? = null) {
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
            registrarMovimiento(batch, "compra", insumoId, cantidadUsoTotal, "global", proveedorId, compraId)
            batch.commit().await()
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
            arrayOf(cantidad, insumoId)
        )
    }

    fun addStock(insumoId: String, cantidad: Double) {
        Timber.tag("INVENTORY").d("Añadiendo $cantidad a $insumoId")
        val db = offlineDb?.writableDatabase ?: return
        db.execSQL(
            "UPDATE ${OfflineDatabase.TABLE_INSUMOS} SET cantidadEnBase = cantidadEnBase + ? WHERE id = ?",
            arrayOf(cantidad, insumoId)
        )
    }

    fun validarVentaCompleta(
        productoId: String,
        recetaId: String?,
        toppings: List<String>,
        base: String? = null,
        aderezo: String? = null,
        esSeparado: Boolean = false,
        cantidad: Int = 1
    ): Boolean {
        val valido = validarStockLocal(calcularDeduccionesVentaOffline(productoId, recetaId, toppings, base, aderezo, esSeparado, cantidad))
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
        return descontarVentaCompleta(productoId, recetaId, toppings, null, null, false, cantidad)
    }

    fun descontarVentaCompleta(
        productoId: String,
        recetaId: String?,
        toppings: List<String>,
        base: String? = null,
        aderezo: String? = null,
        esSeparado: Boolean = false,
        cantidad: Int = 1
    ): Boolean {
        val deducciones = calcularDeduccionesVentaOffline(productoId, recetaId, toppings, base, aderezo, esSeparado, cantidad)
        if (!validarStockLocal(deducciones)) {
            Timber.tag("INVENTORY").w("Stock insuficiente para $productoId")
            return false
        }
        deducciones.forEach { (insumoId, cant) -> deductStock(insumoId, cant) }
        Timber.tag("INVENTORY").i("Venta completada para $productoId, deducciones aplicadas")
        return true
    }

    private fun calcularDeduccionesVentaOffline(
        productoId: String,
        recetaId: String?,
        toppings: List<String>,
        base: String?,
        aderezo: String?,
        esSeparado: Boolean,
        cantidad: Int
    ): Map<String, Double> {
        val db = offlineDb ?: return emptyMap()
        val producto = db.obtenerProductoPorId(productoId) ?: return emptyMap()
        val receta = recetaId?.let { db.obtenerRecetaPorId(it) }
        val item = ItemCarritoV2(
            producto = producto,
            precioFinal = BigDecimal.ZERO,
            cantidad = cantidad,
            base = base,
            aderezo = aderezo,
            toppings = toppings,
            esSeparado = esSeparado
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
        tandasPreparadas: Double, // Ahora representa 'tandas'
        sobranteAnterior: Double,
        sucursal: String
    ): Boolean {
        return try {
            val sucursalId = sucursal.lowercase()
            val batch = firestore.batch()
            
            // 1. Añadir porciones a la sucursal
            batch.set(
                firestore.collection(FirestoreCollections.INVENTARIO_SUCURSAL).document("${sucursalId}_$insumoId"),
                mapOf(
                    "id" to "${sucursalId}_$insumoId",
                    "insumoId" to insumoId,
                    "sucursal" to sucursalId,
                    "cantidadEnBase" to FieldValue.increment(porcionesObtenidas),
                    "ultimaActualizacion" to System.currentTimeMillis()
                ),
                SetOptions.merge()
            )

            // 2. Descontar ingredientes de Bodega Central (Global)
            val deducciones = InventoryDeductions.getProductionDeductions(insumoId, tandasPreparadas)
            deducciones.forEach { (ingId, cant) ->
                batch.set(
                    firestore.collection(FirestoreCollections.INVENTARIO_GLOBAL).document(ingId),
                    mapOf(
                        "cantidadEnBase" to FieldValue.increment(-cant),
                        "ultimaActualizacion" to System.currentTimeMillis()
                    ),
                    SetOptions.merge()
                )
                registrarMovimiento(batch, "consumo_produccion", ingId, -cant, "global", "sistema", null)
            }

            registrarMovimiento(batch, "produccion", insumoId, porcionesObtenidas, sucursalId, "sistema", null)
            batch.commit().await()
            Timber.tag("PRODUCTION").i("Producción registrada: $insumoId +$porcionesObtenidas en $sucursalId")
            true
        } catch (e: Exception) {
            Timber.tag("PRODUCTION").e(e, "Error al registrar producción")
            false
        }
    }

    suspend fun registrarCierreDiario(
        sobrantes: Map<String, Double>,
        stockSistema: Map<String, Double>,
        sucursal: String
    ): Boolean {
        return try {
            val sucursalId = sucursal.lowercase()
            val batch = firestore.batch()
            val cierreId = firestore.collection(FirestoreCollections.INVENTORY_CLOSURES).document().id
            batch.set(
                firestore.collection(FirestoreCollections.INVENTORY_CLOSURES).document(cierreId),
                mapOf("id" to cierreId, "sucursal" to sucursalId, "sobrantes" to sobrantes, "stockSistema" to stockSistema, "fecha" to System.currentTimeMillis())
            )
            sobrantes.forEach { (insumoId, conteo) ->
                val diferencia = conteo - (stockSistema[insumoId] ?: 0.0)
                batch.set(
                    firestore.collection(FirestoreCollections.INVENTARIO_SUCURSAL).document("${sucursalId}_$insumoId"),
                    mapOf("cantidadEnBase" to conteo, "ultimaActualizacion" to System.currentTimeMillis()),
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
            val sucursalId = sucursal.lowercase()
            val batch = firestore.batch()
            val aperturaId = firestore.collection(FirestoreCollections.INVENTORY_OPENINGS).document().id
            batch.set(
                firestore.collection(FirestoreCollections.INVENTORY_OPENINGS).document(aperturaId),
                mapOf("id" to aperturaId, "sucursal" to sucursalId, "conteos" to conteos, "usuarioId" to usuarioId, "fecha" to System.currentTimeMillis())
            )
            conteos.forEach { (insumoId, conteo) ->
                val diferencia = conteo - (stockSistema[insumoId] ?: 0.0)
                batch.set(
                    firestore.collection(FirestoreCollections.INVENTARIO_SUCURSAL).document("${sucursalId}_$insumoId"),
                    mapOf(
                        "id" to "${sucursalId}_$insumoId",
                        "insumoId" to insumoId,
                        "sucursal" to sucursalId,
                        "cantidadEnBase" to conteo,
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
            val sucursalId = sucursal.lowercase()
            val batch = firestore.batch()
            val mermaId = firestore.collection(FirestoreCollections.MERMA_LOGS).document().id
            batch.set(
                firestore.collection(FirestoreCollections.MERMA_LOGS).document(mermaId),
                mapOf("id" to mermaId, "insumoId" to insumoId, "cantidad" to cantidad, "motivo" to motivo, "usuarioId" to usuarioId, "sucursal" to sucursalId, "fecha" to System.currentTimeMillis())
            )
            batch.set(
                firestore.collection(FirestoreCollections.INVENTARIO_SUCURSAL).document("${sucursalId}_$insumoId"),
                mapOf("cantidadEnBase" to FieldValue.increment(-cantidad), "ultimaActualizacion" to System.currentTimeMillis()),
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
                "insumoId" to insumoId,
                "cantidadEnBase" to cantidad,
                "sucursal" to sucursal.lowercase(),
                "usuarioId" to usuarioId,
                "referenciaId" to referenciaId,
                "fecha" to System.currentTimeMillis()
            )
        )
    }
}


