package com.bocatta.pos.presentation.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.viewModelScope
import com.bocatta.pos.network.firebase.FirebaseFirestoreProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.bocatta.pos.core.constants.FirestoreCollections

data class AlertaStock(val insumo: String, val diasRestantes: Int, val esCritico: Boolean)

class ReportesInventarioViewModel : BaseViewModel() {
    private val db = FirebaseFirestoreProvider.db
    var consumoInsumos = mutableStateMapOf<String, Double>()
        private set
    var alertas = mutableStateListOf<AlertaStock>()
        private set
    
    init { cargarReportes() }
    
    private fun cargarReportes() {
        viewModelScope.launch {
            try {
                cargando = true
                val hace7dias = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
                val snap = db.collection(FirestoreCollections.VENTAS).whereGreaterThanOrEqualTo("fecha", hace7dias).get().await()
                val counts = mutableMapOf<String, Double>()
                snap.documents.forEach { doc ->
                    val ids = doc.get("productosIds") as? List<*> ?: emptyList<String>()
                    ids.filterIsInstance<String>().forEach { pid -> counts[pid] = (counts[pid] ?: 0.0) + 1.0 }
                }
                consumoInsumos.clear()
                counts.entries.sortedByDescending { it.value }.take(10).forEach { (k, v) -> consumoInsumos[k] = v }
                
                val stockSnap = db.collection(FirestoreCollections.INVENTARIO_GLOBAL).get().await()
                alertas.clear()
                stockSnap.documents.forEach { doc ->
                    val cant = doc.getDouble("cantidadEnBase") ?: doc.getDouble("cantidadDisponible") ?: 0.0
                    val consumo7dias = counts[doc.id] ?: 0.0
                    if (consumo7dias > 0) {
                        val dias = (cant / consumo7dias).toInt()
                        if (dias <= 7) alertas.add(AlertaStock(doc.id, dias, dias <= 3))
                    }
                }
            } catch (e: Exception) {
                mensajeError = "Error: ${e.message}"
            } finally { cargando = false }
        }
    }
}



