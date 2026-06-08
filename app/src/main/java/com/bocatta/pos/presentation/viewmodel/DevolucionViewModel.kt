package com.bocatta.pos.presentation.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.viewModelScope
import com.bocatta.pos.data.repository.StockAllocationRepository
import com.bocatta.pos.domain.model.ItemVendidoV2
import com.bocatta.pos.domain.model.SolicitudDevolucion
import com.bocatta.pos.domain.model.VentaV2
import com.bocatta.pos.network.firebase.FirebaseFirestoreProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.bocatta.pos.core.constants.FirestoreCollections

class DevolucionViewModel : BaseViewModel() {
    private val db = FirebaseFirestoreProvider.db
    private val allocationRepo = StockAllocationRepository()

    var solicitudes = mutableStateListOf<SolicitudDevolucion>()
        private set

    private var listenerDevoluciones: ListenerRegistration? = null

    var ventasBuscadas = mutableStateListOf<VentaV2>()
        private set

    init {
        escucharSolicitudes()
    }

    private fun escucharSolicitudes() {
        listenerDevoluciones = db.collection(FirestoreCollections.DEVOLUCIONES)
            .whereEqualTo("estado", "pendiente")
            .addSnapshotListener { snap, _ ->
                if (snap != null) {
                    solicitudes.clear()
                    snap.documents.forEach { doc ->
                        doc.toObject(SolicitudDevolucion::class.java)?.let { solicitudes.add(it.copy(id = doc.id)) }
                    }
                }
            }
    }

    fun aprobarDevolucion(solicitud: SolicitudDevolucion) {
        viewModelScope.launch {
            cargando = true
            try {
                // REVERSIÓN V2: Restaurar stock en sucursal
                val batch = db.batch()
                val idsConCuotaSucursal = allocationRepo.itemsVendibles.toSet()
                val idsFisicosSucursal = allocationRepo.itemsFisicos.toSet()
                
                // 1. Obtener los productos de la venta para saber qué stock restaurar
                val ventaDoc = db.collection(FirestoreCollections.VENTAS).document(solicitud.ventaId).get().await()
                val sucursal = ventaDoc.getString("sucursal")?.lowercase(java.util.Locale.getDefault()) ?: "atlixco"
                @Suppress("UNCHECKED_CAST")
                val itemsVendidos = (ventaDoc.get("productos") as? List<Map<String, Any>>)?.map { data ->
                    ItemVendidoV2(
                        cartId = data["cartId"] as? String ?: "",
                        productoId = data["productoId"] as? String ?: "",
                        nombre = data["nombre"] as? String ?: "",
                        cantidad = (data["cantidad"] as? Number)?.toInt() ?: 1,
                        precioUnitario = (data["precioUnitario"] as? Number)?.toDouble() ?: 0.0,
                        recetaId = data["recetaId"] as? String,
                        deducciones = (data["deducciones"] as? Map<String, Any>)?.mapValues {
                            (it.value as? Number)?.toDouble() ?: 0.0
                        } ?: emptyMap()
                    )
                } ?: emptyList()

                itemsVendidos.forEach { item ->
                    item.deducciones.forEach { (insumoId, cantidad) ->
                        if (cantidad > 0.0) {
                            if (insumoId in idsConCuotaSucursal) {
                                val stockRef = db.collection(FirestoreCollections.INVENTARIO_SUCURSAL).document("${sucursal}_$insumoId")
                                batch.set(
                                    stockRef,
                                    mapOf(
                                        "id" to "${sucursal}_$insumoId",
                                        "insumoId" to insumoId,
                                        "sucursal" to sucursal,
                                        "cantidadEnBase" to FieldValue.increment(cantidad),
                                        "ultimaActualizacion" to System.currentTimeMillis()
                                    ),
                                    com.google.firebase.firestore.SetOptions.merge()
                                )
                            }
                            if (insumoId !in idsFisicosSucursal) {
                                val stockRef = db.collection(FirestoreCollections.INVENTARIO_GLOBAL).document(insumoId)
                                batch.set(
                                    stockRef,
                                    mapOf(
                                        "id" to insumoId,
                                        "insumoId" to insumoId,
                                        "cantidadEnBase" to FieldValue.increment(cantidad),
                                        "ultimaActualizacion" to System.currentTimeMillis()
                                    ),
                                    com.google.firebase.firestore.SetOptions.merge()
                                )
                            }
                        }
                    }
                }

                // 3. Marcar venta y solicitud como devuelta
                batch.update(db.collection(FirestoreCollections.VENTAS).document(solicitud.ventaId), "estado", "devuelta")
                batch.update(db.collection(FirestoreCollections.DEVOLUCIONES).document(solicitud.id), "estado", "aprobada")

                batch.commit().await()
                mensajeExito = "Devolución aprobada e inventario V2 restaurado ✓"
            } catch (e: Exception) {
                mensajeError = "Error: ${e.message}"
            }
            cargando = false
        }
    }

    fun rechazarDevolucion(solicitud: SolicitudDevolucion) {
        viewModelScope.launch {
            try {
                db.collection(FirestoreCollections.DEVOLUCIONES).document(solicitud.id)
                    .update("estado", "rechazada").await()
                mensajeExito = "Devolución rechazada"
            } catch (e: Exception) {
                mensajeError = "Error: ${e.message}"
            }
        }
    }

    fun cargarVentasRecientes(sucursal: String) {
        viewModelScope.launch {
            val inicioDia = System.currentTimeMillis() - 86_400_000L // últimas 24h
            try {
                val snap = db.collection(FirestoreCollections.VENTAS)
                    .whereEqualTo("sucursal", sucursal.lowercase(java.util.Locale.getDefault()))
                    .whereGreaterThanOrEqualTo("fecha", inicioDia)
                    .get().await()
                
                ventasBuscadas.clear()
                snap.documents.forEach { doc ->
                    doc.toObject(VentaV2::class.java)?.let {
                        if (it.estado != "devuelta") {
                            ventasBuscadas.add(it.copy(id = doc.id))
                        }
                    }
                }
            } catch (e: Exception) {
                mensajeError = "Error: ${e.message}"
            }
        }
    }

    fun solicitarDevolucion(venta: VentaV2, motivo: String, solicitante: String) {
        viewModelScope.launch {
            try {
                val id = db.collection(FirestoreCollections.DEVOLUCIONES).document().id
                val solicitud = SolicitudDevolucion(
                    id = id,
                    ventaId = venta.id,
                    motivo = motivo,
                    solicitadoPor = solicitante,
                    fecha = System.currentTimeMillis(),
                    estado = "pendiente",
                    totalVenta = venta.total,
                    productosVenta = venta.productos,
                    productosIds = venta.productosIds,
                    numeroTicket = venta.numeroTicket
                )
                db.collection(FirestoreCollections.DEVOLUCIONES).document(id).set(solicitud).await()
                mensajeExito = "Solicitud enviada ✓"
            } catch (e: Exception) {
                mensajeError = "Error: ${e.message}"
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        listenerDevoluciones?.remove()
    }
}



