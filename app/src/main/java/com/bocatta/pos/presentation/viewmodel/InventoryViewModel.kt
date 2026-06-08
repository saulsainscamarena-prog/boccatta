package com.bocatta.pos.presentation.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.viewModelScope
import com.bocatta.pos.domain.model.InsumoV2
import com.bocatta.pos.network.firebase.FirebaseFirestoreProvider
import com.bocatta.pos.core.constants.FirestoreCollections
import com.bocatta.pos.data.repository.DataSeederV2
import com.bocatta.pos.data.repository.InventoryRepository
import com.bocatta.pos.domain.repository.IInventoryRepository
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlinx.coroutines.tasks.await
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class InventoryViewModel(private val repo: InventoryRepository = InventoryRepository()) : BaseViewModel(), KoinComponent {
    private val cloudInventoryRepo: IInventoryRepository by inject()
    private val dataSeeder: DataSeederV2 by inject()
    private val db = FirebaseFirestoreProvider.db

    private var listenerInventario: ListenerRegistration? = null
    private var listenerMaestro: ListenerRegistration? = null

    var stockInsumos = mutableStateMapOf<String, Double>()
        private set
    var maestroInsumos = mutableStateMapOf<String, com.bocatta.pos.domain.model.InsumoV2>()
        private set
    var insumosApertura = mutableStateListOf<Map<String, Any>>()
        private set
    var promediosConsumo = mutableStateMapOf<String, Double>()
        private set

    private var sucursalActiva: String? = null

    fun configurarSucursal(sucursal: String) {
        sucursalActiva = sucursal.lowercase(java.util.Locale.getDefault())
        escucharInventario()
        escucharMaestro()
    }

    private fun escucharMaestro() {
        listenerMaestro?.remove()
        listenerMaestro = db.collection(FirestoreCollections.INSUMOS).addSnapshotListener { snap, error ->
            if (error != null) { return@addSnapshotListener }
            if (snap != null) {
                val newList = mutableStateMapOf<String, com.bocatta.pos.domain.model.InsumoV2>()
                snap.documents.forEach { doc ->
                    doc.toObject(com.bocatta.pos.domain.model.InsumoV2::class.java)?.let { newList[doc.id] = it.copy(id = doc.id) }
                }
                maestroInsumos.clear()
                maestroInsumos.putAll(newList)
            }
        }
    }

    private fun escucharInventario() {
        val sucursal = sucursalActiva ?: return
        listenerInventario?.remove()
        listenerInventario = db.collection(FirestoreCollections.INVENTARIO_SUCURSAL)
            .whereEqualTo("sucursal", sucursal)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    stockInsumos.clear()
                    snapshot.documents.forEach { doc ->
                        val id = doc.getString("insumoId") ?: doc.id.removePrefix("${sucursal}_")
                        val cant = doc.getDouble("cantidadEnBase") ?: doc.getDouble("cantidadDisponible") ?: 0.0
                        stockInsumos[id] = cant
                    }
                    calcularPromediosGlobales()
                }
            }
    }

    private fun calcularPromediosGlobales() {
        val sucursal = sucursalActiva ?: return
        viewModelScope.launch {
            try {
                val hace7Dias = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
                val snap = db.collection(FirestoreCollections.VENTAS)
                    .whereEqualTo("sucursal", sucursal)
                    .get().await()

                val counts = mutableMapOf<String, Double>()
                snap.documents.filter { (it.getLong("fecha") ?: 0L) >= hace7Dias }.forEach { doc ->
                    val productosIds = (doc.get("productosIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    productosIds.forEach { pid ->
                        counts[pid] = (counts[pid] ?: 0.0) + 1.0
                        if (pid.contains("cr") || pid.contains("combo")) {
                            counts["masa_crepa"] = (counts["masa_crepa"] ?: 0.0) + 1.0
                        }
                    }
                }

                promediosConsumo.clear()
                counts.forEach { (k, v) -> promediosConsumo[k] = v / 7.0 }
            } catch (e: Exception) {
                mensajeError = "Error: ${e.message}"
                Timber.tag("InventoryVM").e(e, "Error calculando promedios globales")
            }
        }
    }


    fun registrarProduccion(
        insumoId: String,
        porcionesObtenidas: Double,
        tandasPreparadas: Double,
        sobranteAnterior: Double,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val exito = repo.registrarProduccion(insumoId, porcionesObtenidas, tandasPreparadas, sobranteAnterior, sucursalActiva ?: "global")
                onResult(exito)
            } catch (e: Exception) {
                mensajeError = "Error: ${e.message}"
                onResult(false)
            }
        }
    }

    fun registrarCompraConPresentacion(
        insumoId: String,
        presentacionNombre: String,
        cantidad: Double,
        contenidoEquivalente: Double,
        costoTotal: Double,
        usuarioId: String,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val exito = cloudInventoryRepo.registrarCompraConPresentacion(
                    branchId = sucursalActiva ?: "global",
                    insumoId = insumoId,
                    presentacionNombre = presentacionNombre,
                    cantidadComprada = cantidad,
                    contenidoEquivalente = contenidoEquivalente,
                    costoTotal = costoTotal,
                    userId = usuarioId
                )
                onResult(exito)
            } catch (e: Exception) {
                mensajeError = "Error: ${e.message}"
                onResult(false)
            }
        }
    }

    fun cargarStockEmergencia(sucursal: String) {
        viewModelScope.launch {
            cargando = true
            try {
                dataSeeder.cargarStockEmergencia(sucursal.lowercase(java.util.Locale.getDefault()))
                    .onSuccess { 
                        mensajeExito = "✅ Stock de emergencia cargado para ${sucursal.uppercase(java.util.Locale.getDefault())}"
                        configurarSucursal(sucursal)
                    }
                    .onFailure { e -> mensajeError = "Error: ${e.message}" }
            } catch (e: Exception) {
                mensajeError = "Error: ${e.message}"
            } finally {
                cargando = false
            }
        }
    }

    fun registrarCierreDiario(sobrantes: Map<String, Double>, onResult: (Boolean) -> Unit) {
        val sucursal = sucursalActiva ?: return
        viewModelScope.launch {
            try {
                val exito = repo.registrarCierreDiario(sobrantes, stockInsumos.toMap(), sucursal)
                onResult(exito)
            } catch (e: Exception) {
                mensajeError = "Error: ${e.message}"
                onResult(false)
            }
        }
    }

    fun registrarAperturaDiaria(conteos: Map<String, Double>, usuarioId: String, onResult: (Boolean) -> Unit) {
        val sucursal = sucursalActiva ?: return
        viewModelScope.launch {
            try {
                val exito = repo.registrarAperturaInventario(conteos, stockInsumos.toMap(), usuarioId, sucursal)
                onResult(exito)
            } catch (e: Exception) {
                mensajeError = "Error: ${e.message}"
                onResult(false)
            }
        }
    }

    fun registrarMermaManual(insumoId: String, cantidad: Double, motivo: String, usuarioId: String, onResult: (Boolean) -> Unit) {
        val sucursal = sucursalActiva ?: "global"
        viewModelScope.launch {
            try {
                val exito = repo.registrarMermaManual(insumoId, cantidad, motivo, usuarioId, sucursal)
                onResult(exito)
            } catch (e: Exception) {
                mensajeError = "Error: ${e.message}"
                onResult(false)
            }
        }
    }

    fun obtenerPromedioVenta(insumoId: String, onResult: (Double) -> Unit) {
        viewModelScope.launch {
            try {
                val hace7Dias = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
                val snap = db.collection(FirestoreCollections.VENTAS)
                    .whereEqualTo("sucursal", sucursalActiva)
                    .get().await()

                var totalVendido = 0.0
                snap.documents.filter { (it.getLong("fecha") ?: 0L) >= hace7Dias }.forEach { doc ->
                    val productosIds = (doc.get("productosIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    totalVendido += productosIds.count { it == insumoId }

                    if (insumoId == "masa_crepa") {
                        totalVendido += productosIds.count { it.contains("cr") || it.contains("combo") }
                    }
                }
                onResult(totalVendido / 7.0)
            } catch (e: Exception) {
                onResult(0.0)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        listenerInventario?.remove()
        listenerMaestro?.remove()
    }
}



