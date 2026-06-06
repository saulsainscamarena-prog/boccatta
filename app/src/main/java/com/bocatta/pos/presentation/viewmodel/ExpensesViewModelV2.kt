package com.bocatta.pos.presentation.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.viewModelScope
import com.bocatta.pos.domain.model.*
import com.bocatta.pos.network.firebase.FirebaseFirestoreProvider
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.bocatta.pos.core.constants.FirestoreCollections

class ExpensesViewModelV2 : BaseViewModel() {
    private val db = FirebaseFirestoreProvider.db
    private var suppliesListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var expensesListener: com.google.firebase.firestore.ListenerRegistration? = null

    var insumosDisponibles = mutableStateListOf<InsumoV2>()
        private set
    var gastos = mutableStateListOf<GastoV2>()
        private set

    init {
        escucharInsumos()
    }

    private fun escucharInsumos() {
        suppliesListener = db.collection(FirestoreCollections.INSUMOS).addSnapshotListener { snap, _ ->
            if (snap != null) {
                insumosDisponibles.clear()
                snap.documents.forEach { doc ->
                    doc.toObject(InsumoV2::class.java)?.let { insumosDisponibles.add(it.copy(id = doc.id)) }
                }
            }
        }
    }

    fun cargarGastos(sucursal: String) {
        val hoy = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0); set(java.util.Calendar.SECOND, 0)
        }.timeInMillis

        expensesListener?.remove()
        expensesListener = db.collection(FirestoreCollections.GASTOS)
            .whereEqualTo("sucursal", sucursal.lowercase())
            .addSnapshotListener { snap, _ ->
                if (snap != null) {
                    gastos.clear()
                    snap.documents.forEach { doc ->
                        doc.toObject(GastoV2::class.java)
                            ?.takeIf { it.fecha >= hoy }
                            ?.let { gastos.add(it.copy(id = doc.id)) }
                    }
                }
            }
    }

    override fun onCleared() {
        super.onCleared()
        suppliesListener?.remove()
        expensesListener?.remove()
    }

    fun registrarGastoIndustrial(
        descripcion: String,
        monto: Double,
        categoria: String,
        sucursal: String,
        usuarioId: String,
        insumoId: String? = null,
        cantidadSurtida: Double = 0.0,
        presentacionCompra: String? = null,
        cantidadComprada: Double = 0.0,
        contenidoPorUnidad: Double = 0.0
    ) {
        if (cargando) return
        if (monto <= 0) {
            mensajeError = "El monto debe ser mayor a $0"
            return
        }
        viewModelScope.launch(safeHandler) {
            cargando = true
            try {
                val batch = db.batch()
                val gastoId = db.collection(FirestoreCollections.GASTOS).document().id
                val gasto = GastoV2(
                    id = gastoId,
                    descripcion = descripcion,
                    monto = monto,
                    categoria = categoria,
                    fecha = System.currentTimeMillis(),
                    sucursal = sucursal.lowercase(),
                    usuarioId = usuarioId
                )

                batch.set(
                    db.collection(FirestoreCollections.GASTOS).document(gastoId),
                    mapOf(
                        "id" to gasto.id,
                        "descripcion" to gasto.descripcion,
                        "concepto" to gasto.descripcion,
                        "monto" to gasto.monto,
                        "categoria" to gasto.categoria,
                        "fecha" to gasto.fecha,
                        "sucursal" to gasto.sucursal,
                        "usuarioId" to gasto.usuarioId,
                        "usuario" to gasto.usuarioId,
                        "insumoId" to insumoId,
                        "cantidadSurtida" to cantidadSurtida,
                        "presentacionCompra" to presentacionCompra,
                        "cantidadComprada" to cantidadComprada,
                        "contenidoPorUnidad" to contenidoPorUnidad
                    )
                )

                if (insumoId != null && cantidadSurtida > 0) {
                    val costoUnitario = monto / cantidadSurtida
                    val stockRef = db.collection(FirestoreCollections.INVENTARIO_GLOBAL).document(insumoId)
                    batch.set(
                        stockRef,
                        mapOf(
                            "cantidadEnBase" to FieldValue.increment(cantidadSurtida),
                            "cantidadDisponible" to FieldValue.increment(cantidadSurtida),
                            "currentQty" to FieldValue.increment(cantidadSurtida),
                            "ultimaActualizacion" to System.currentTimeMillis()
                        ),
                        com.google.firebase.firestore.SetOptions.merge()
                    )
                    batch.set(
                        db.collection(FirestoreCollections.INSUMOS).document(insumoId),
                        mapOf(
                            "cantidadEnBase" to FieldValue.increment(cantidadSurtida),
                            "costoUnitarioBase" to costoUnitario,
                            "ultimaActualizacion" to System.currentTimeMillis()
                        ),
                        com.google.firebase.firestore.SetOptions.merge()
                    )
                }

                batch.commit().await()
                mensajeExito = "Operación exitosa"
            } finally {
                cargando = false
            }
        }
    }

    fun eliminarGasto(gastoId: String) {
        viewModelScope.launch(safeHandler) {
            val gastoRef = db.collection(FirestoreCollections.GASTOS).document(gastoId)
            val snap = gastoRef.get().await()
            val insumoId = snap.getString("insumoId")
            val cantidadSurtida = snap.getDouble("cantidadSurtida") ?: 0.0
            val batch = db.batch()
            if (!insumoId.isNullOrBlank() && cantidadSurtida > 0.0) {
                batch.set(
                    db.collection(FirestoreCollections.INVENTARIO_GLOBAL).document(insumoId),
                    mapOf(
                        "cantidadEnBase" to FieldValue.increment(-cantidadSurtida),
                        "cantidadDisponible" to FieldValue.increment(-cantidadSurtida),
                        "currentQty" to FieldValue.increment(-cantidadSurtida),
                        "ultimaActualizacion" to System.currentTimeMillis()
                    ),
                    com.google.firebase.firestore.SetOptions.merge()
                )
                batch.set(
                    db.collection(FirestoreCollections.INSUMOS).document(insumoId),
                    mapOf("cantidadEnBase" to FieldValue.increment(-cantidadSurtida), "ultimaActualizacion" to System.currentTimeMillis()),
                    com.google.firebase.firestore.SetOptions.merge()
                )
            }
            batch.delete(gastoRef)
            batch.commit().await()
            mensajeExito = "Gasto eliminado"
        }
    }

    fun limpiarError() { mensajeError = null }
}
