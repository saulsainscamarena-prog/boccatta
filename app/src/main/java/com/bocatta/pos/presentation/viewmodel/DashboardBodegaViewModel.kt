package com.bocatta.pos.presentation.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.viewModelScope
import com.bocatta.pos.network.firebase.FirebaseFirestoreProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.bocatta.pos.core.constants.FirestoreCollections

data class InsumoStock(
    val id: String,
    val cantidadGlobal: Double,
    val diasRestantes: Int,
    val esCritico: Boolean,
    val stockPorSucursal: Map<String, Double>
)

class DashboardBodegaViewModel : BaseViewModel() {
    private val db = FirebaseFirestoreProvider.db
    var insumos = mutableStateListOf<InsumoStock>()
        private set
    var sucursales = mutableStateListOf<String>()
        private set
    var totalInsumos by mutableIntStateOf(0)
        private set
    var promedioStock by mutableStateOf(0.0)
        private set
    var cantidadCriticos by mutableIntStateOf(0)
        private set
    var ultimaActualizacion by mutableStateOf("")
        private set
    
    init { cargarDatos() }
    
    private fun cargarDatos() {
        viewModelScope.launch {
            try {
                cargando = true
                val ahora = System.currentTimeMillis()
                val hace7dias = ahora - (7 * 24 * 60 * 60 * 1000L)
                
                val globalDocs = db.collection(FirestoreCollections.INVENTARIO_GLOBAL).get().await()
                val ventasDocs = db.collection(FirestoreCollections.VENTAS).whereGreaterThanOrEqualTo("fecha", hace7dias).get().await()
                val branchDocs = db.collection(FirestoreCollections.INVENTARIO_SUCURSAL).get().await()
                
                val consumos = mutableMapOf<String, Double>()
                ventasDocs.documents.forEach { doc ->
                    (doc.get("productosIds") as? List<*>)?.filterIsInstance<String>()?.forEach { pid ->
                        consumos[pid] = (consumos[pid] ?: 0.0) + 1.0
                    }
                }
                
                val sucSet = mutableSetOf<String>()
                val stockBranch = mutableMapOf<String, MutableMap<String, Double>>()
                branchDocs.documents.forEach { doc ->
                    val suc = doc.getString("sucursal") ?: return@forEach
                    sucSet.add(suc)
                    val insumoId = doc.getString("insumoId") ?: doc.id.replaceFirst("${suc}_", "")
                    val branchMap = stockBranch.getOrPut(insumoId) { mutableMapOf() }
                    branchMap[suc] = doc.getDouble("cantidadEnBase") ?: doc.getDouble("cantidadDisponible") ?: 0.0
                }
                
                sucursales.clear()
                sucursales.addAll(sucSet.sorted())
                
                insumos.clear()
                totalInsumos = 0
                var sumaStock = 0.0
                cantidadCriticos = 0
                
                globalDocs.documents.forEach { doc ->
                    val id = doc.id
                    val cantGlobal = doc.getDouble("cantidadEnBase") ?: doc.getDouble("cantidadDisponible") ?: 0.0
                    val consumo7dias = consumos[id] ?: 0.0
                    val dias = if (consumo7dias > 0) (cantGlobal / consumo7dias).toInt() else 999
                    val esCritico = cantGlobal <= 0 || dias <= 3
                    
                    insumos.add(InsumoStock(id, cantGlobal, dias, esCritico, stockBranch[id]?.toMap() ?: emptyMap()))
                    totalInsumos++
                    sumaStock += cantGlobal
                    if (esCritico) cantidadCriticos++
                }
                
                promedioStock = if (totalInsumos > 0) (sumaStock / totalInsumos) * 10 else 0.0
                
                val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                ultimaActualizacion = sdf.format(java.util.Date())
                
                insumos.sortByDescending { it.esCritico }
            } catch (e: Exception) {
                mensajeError = "Error: ${e.message}"
            }
            finally { cargando = false }
        }
    }
}



