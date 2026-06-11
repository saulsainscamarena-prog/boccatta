package com.bocatta.pos.data.repository

import com.bocatta.pos.core.constants.FirestoreCollections
import com.bocatta.pos.network.firebase.FirebaseFirestoreProvider
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.util.Locale

data class StockAllocationItem(
    val insumoId: String = "",
    val nombre: String = "",
    val stockCentral: Int = 0,
    val cuotaActualSucursal: Int = 0,
    val cuotaSugeridaSucursal: Int = 0,
    val sucursalesAbiertas: List<String> = emptyList(),
    val esFisico: Boolean = false
)

class StockAllocationRepository {
    private val db = FirebaseFirestoreProvider.db

    val itemsVirtuales = listOf("masa_crepa")
    val itemsFisicos = listOf(
        "carlota_unidad",
        "tiramisu_unidad",
        "fresas_crema_unidad",
        "duraznos_crema_unidad"
    )
    val itemsVendibles = itemsVirtuales + itemsFisicos

    private val nombres = mapOf(
        "masa_crepa" to "Masa de Crepa",
        "carlota_unidad" to "Carlota de Limon",
        "tiramisu_unidad" to "Tiramisu",
        "fresas_crema_unidad" to "Fresas con Crema",
        "duraznos_crema_unidad" to "Duraznos con Crema"
    )

    suspend fun previewApertura(sucursal: String): List<StockAllocationItem> {
        val sucursalId = normalizarSucursal(sucursal)
        val sucursales = sucursalesAbiertas().plus(sucursalId).distinct().sorted()
        val divisor = sucursales.size.coerceAtLeast(1)
        return itemsVendibles.map { insumoId ->
            val central = stockGlobal(insumoId).toInt()
            val actual = stockSucursal(sucursalId, insumoId).toInt()
            StockAllocationItem(
                insumoId = insumoId,
                nombre = nombres[insumoId] ?: insumoId,
                stockCentral = central,
                cuotaActualSucursal = actual,
                cuotaSugeridaSucursal = central / divisor,
                sucursalesAbiertas = sucursales,
                esFisico = itemsFisicos.contains(insumoId)
            )
        }
    }

    suspend fun confirmarApertura(
        sucursal: String,
        usuarioId: String,
        conteosFisicos: Map<String, Int> = emptyMap(),
        motivosDiferencia: Map<String, String> = emptyMap()
    ): List<StockAllocationItem> {
        val sucursalId = normalizarSucursal(sucursal)
        val sucursales = sucursalesAbiertas().plus(sucursalId).distinct().sorted()
        aplicarRedistribucionVirtual(sucursales, usuarioId, "asignacion_apertura")
        aplicarConteoFisicoPostres(sucursalId, usuarioId, conteosFisicos, motivosDiferencia)
        return previewApertura(sucursalId)
    }

    suspend fun redistribuirSucursalesAbiertas(usuarioId: String = "sistema") {
        val sucursales = sucursalesAbiertas()
        if (sucursales.isNotEmpty()) {
            aplicarRedistribucionVirtual(sucursales, usuarioId, "reasignacion")
        }
    }

    suspend fun liberarSucursal(sucursal: String, usuarioId: String) {
        val sucursalId = normalizarSucursal(sucursal)
        val batch = db.batch()
        val now = System.currentTimeMillis()
        itemsVirtuales.forEach { insumoId ->
            val branchRef = db.collection(FirestoreCollections.INVENTARIO_SUCURSAL)
                .document("${sucursalId}_$insumoId")
            batch.set(
                branchRef,
                mapOf(
                    "id" to branchRef.id,
                    "insumoId" to insumoId,
                    "sucursal" to sucursalId,
                    "cantidadEnBase" to 0.0,
                    "ultimaActualizacion" to now
                ),
                SetOptions.merge()
            )
            registrarMovimiento(batch, "liberacion_cierre", insumoId, 0.0, sucursalId, usuarioId, now)
        }
        batch.commit().await()
    }

    private suspend fun aplicarRedistribucionVirtual(
        sucursales: List<String>,
        usuarioId: String,
        tipoMovimiento: String
    ) {
        val divisor = sucursales.size.coerceAtLeast(1)
        val stocks = itemsVirtuales.associateWith { stockGlobal(it).toInt() }
        val batch = db.batch()
        val now = System.currentTimeMillis()
        sucursales.forEach { sucursal ->
            itemsVirtuales.forEach { insumoId ->
                val cuota = (stocks[insumoId] ?: 0) / divisor
                val ref = db.collection(FirestoreCollections.INVENTARIO_SUCURSAL)
                    .document("${sucursal}_$insumoId")
                batch.set(
                    ref,
                    mapOf(
                        "id" to ref.id,
                        "insumoId" to insumoId,
                        "sucursal" to sucursal,
                        "cantidadEnBase" to cuota.toDouble(),
                        "ultimaActualizacion" to now,
                        "asignacionVirtual" to true
                    ),
                    SetOptions.merge()
                )
                registrarMovimiento(batch, tipoMovimiento, insumoId, cuota.toDouble(), sucursal, usuarioId, now)
            }
        }
        batch.commit().await()
    }

    private suspend fun aplicarConteoFisicoPostres(
        sucursal: String,
        usuarioId: String,
        conteosFisicos: Map<String, Int>,
        motivosDiferencia: Map<String, String>
    ) {
        if (itemsFisicos.none { conteosFisicos.containsKey(it) }) return
        val batch = db.batch()
        val now = System.currentTimeMillis()
        itemsFisicos.forEach { insumoId ->
            val confirmado = conteosFisicos[insumoId] ?: return@forEach
            val actualSucursal = stockSucursal(sucursal, insumoId)
            val sugerido = previewApertura(sucursal).firstOrNull { it.insumoId == insumoId }?.cuotaSugeridaSucursal ?: confirmado
            val transferenciaDesdeCentral = (confirmado - actualSucursal.toInt()).coerceAtLeast(0)
            if (transferenciaDesdeCentral > 0) {
                val centralDisponible = stockGlobal(insumoId)
                if (centralDisponible < transferenciaDesdeCentral) {
                    throw IllegalStateException("Stock insuficiente en bodega central para $insumoId: $centralDisponible < $transferenciaDesdeCentral")
                }
                batch.set(
                    db.collection(FirestoreCollections.INVENTARIO_GLOBAL).document(insumoId),
                    mapOf(
                        "id" to insumoId,
                        "insumoId" to insumoId,
                        "cantidadEnBase" to com.google.firebase.firestore.FieldValue.increment(-transferenciaDesdeCentral.toDouble()),
                        "ultimaActualizacion" to now
                    ),
                    SetOptions.merge()
                )
            }
            val branchRef = db.collection(FirestoreCollections.INVENTARIO_SUCURSAL)
                .document("${sucursal}_$insumoId")
            batch.set(
                branchRef,
                mapOf(
                    "id" to branchRef.id,
                    "insumoId" to insumoId,
                    "sucursal" to sucursal,
                    "cantidadEnBase" to confirmado.toDouble(),
                    "ultimaActualizacion" to now,
                    "asignacionVirtual" to false
                ),
                SetOptions.merge()
            )
            if (confirmado != sugerido) {
                val diffRef = db.collection(FirestoreCollections.MOVIMIENTOS_INVENTARIO).document()
                batch.set(
                    diffRef,
                    mapOf(
                        "id" to diffRef.id,
                        "tipo" to "diferencia_apertura_fisica",
                        "insumoId" to insumoId,
                        "cantidadEnBase" to (confirmado - sugerido).toDouble(),
                        "sucursal" to sucursal,
                        "usuarioId" to usuarioId,
                        "motivo" to (motivosDiferencia[insumoId] ?: "Sin motivo capturado"),
                        "sugerido" to sugerido,
                        "confirmado" to confirmado,
                        "fecha" to now
                    )
                )
            }
            registrarMovimiento(batch, "asignacion_fisica_apertura", insumoId, confirmado.toDouble(), sucursal, usuarioId, now)
        }
        batch.commit().await()
    }

    private suspend fun sucursalesAbiertas(): List<String> {
        val snap = db.collection(FirestoreCollections.TURNOS_CAJA)
            .whereEqualTo("estado", "abierto")
            .get()
            .await()
        return snap.documents.mapNotNull { it.getString("sucursal") }
            .map { normalizarSucursal(it) }
            .distinct()
            .sorted()
    }

    private suspend fun stockGlobal(insumoId: String): Double {
        val doc = db.collection(FirestoreCollections.INVENTARIO_GLOBAL).document(insumoId).get().await()
        return doc.getDouble("cantidadEnBase") ?: doc.getDouble("cantidadDisponible") ?: 0.0
    }

    private suspend fun stockSucursal(sucursal: String, insumoId: String): Double {
        val doc = db.collection(FirestoreCollections.INVENTARIO_SUCURSAL)
            .document("${sucursal}_$insumoId")
            .get()
            .await()
        return doc.getDouble("cantidadEnBase") ?: doc.getDouble("cantidadDisponible") ?: 0.0
    }

    private fun registrarMovimiento(
        batch: com.google.firebase.firestore.WriteBatch,
        tipo: String,
        insumoId: String,
        cantidad: Double,
        sucursal: String,
        usuarioId: String,
        now: Long
    ) {
        val ref = db.collection(FirestoreCollections.MOVIMIENTOS_INVENTARIO).document()
        batch.set(
            ref,
            mapOf(
                "id" to ref.id,
                "tipo" to tipo,
                "insumoId" to insumoId,
                "cantidadEnBase" to cantidad,
                "sucursal" to sucursal,
                "usuarioId" to usuarioId,
                "fecha" to now
            )
        )
    }

    private fun normalizarSucursal(sucursal: String): String =
        sucursal.trim().lowercase(Locale.ROOT).replace(" ", "_")
}
