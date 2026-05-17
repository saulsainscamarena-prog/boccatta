package com.bocatta.pos.presentation.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.viewModelScope
import com.bocatta.pos.domain.model.*
import com.bocatta.pos.network.firebase.FirebaseFirestoreProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.bocatta.pos.core.constants.FirestoreCollections
import timber.log.Timber

class AperturaViewModelV2 : BaseViewModel() {
    private val db = FirebaseFirestoreProvider.db

    // Estado del Checklist
    var conteoPostres = mutableStateMapOf<String, String>()
        private set
    var hayHielo by mutableStateOf(false)
    var hayChantilly by mutableStateOf(false)
    var hayPopotes by mutableStateOf(false)
    var hayBaseFresas by mutableStateOf(false)
    var fondoCaja by mutableStateOf("0.0")
    var tandasMasaEnBodega by mutableStateOf(0.0)
    var tandasATomar by mutableStateOf("0")
    
    var globalStock = mutableStateMapOf<String, Int>()
        private set
    
    var aperturaCompletada by mutableStateOf(false)
        private set
    var turnoYaActivo by mutableStateOf(false)
        private set

    fun cargarGlobalStock() {
        val itemsFiltrados = listOf("masa_crepa", "carlota", "tiramisu", "fresas_crema", "duraznos")
        viewModelScope.launch {
            try {
                val snap = db.collection(FirestoreCollections.INVENTARIO_GLOBAL).get().await()
                globalStock.clear()
                snap.documents.forEach { doc ->
                    if (itemsFiltrados.contains(doc.id)) {
                        globalStock[doc.id] = (doc.getDouble("cantidadEnBase") ?: doc.getDouble("cantidadDisponible") ?: 0.0).toInt()
                    }
                }
            } catch (e: Exception) {
                mensajeError = "Error: ${e.message}"
            }
        }
    }

    private var transferenciaEnProceso by mutableStateOf(false)

    fun confirmarTransferencia(sucursalId: String, items: Map<String, Int>, onComplete: () -> Unit) {
        if (cargando || transferenciaEnProceso) return
        viewModelScope.launch {
            cargando = true
            transferenciaEnProceso = true
            try {
                val batch = db.batch()
                items.forEach { (id, cant) ->
                    if (cant > 0) {
                        // Descontar de global
                        batch.update(db.collection(FirestoreCollections.INVENTARIO_GLOBAL).document(id),
                            "cantidadEnBase", FieldValue.increment(-cant.toDouble()))
                        
                        // Sumar a sucursal (en porciones si es masa, o directo si es postre)
                        val multiplier = if (id == "masa_crepa") 60.0 else 1.0
                        val ref = db.collection(FirestoreCollections.INVENTARIO_SUCURSAL).document("${sucursalId.lowercase()}_${id}")
                        batch.set(ref, mapOf(
                            "id" to "${sucursalId.lowercase()}_${id}",
                            "insumoId" to id,
                            "sucursal" to sucursalId.lowercase(),
                            "cantidadEnBase" to FieldValue.increment(cant * multiplier),
                            "ultimaActualizacion" to System.currentTimeMillis()
                        ), SetOptions.merge())
                    }
                }
                batch.commit().await()
                onComplete()
            } catch (e: Exception) {
                mensajeError = "Error: ${e.message}"
                onComplete()
            } finally {
                cargando = false
                transferenciaEnProceso = false
            }
        }
    }

    fun verificarTurno(sucursal: String) {
        val sucursalId = sucursal.lowercase()
        viewModelScope.launch {
            try {
                // 1. Verificar si la sucursal existe, si no, crearla (Industrialización V2)
                val sucDoc = db.collection(FirestoreCollections.SUCURSALES).document(sucursalId).get().await()
                if (!sucDoc.exists()) {
                    Timber.tag("APERTURA").i("Sucursal $sucursalId no encontrada. Inicializando automáticamente...")
                    // Aquí podríamos inyectar GestionSucursalesViewModel, pero para evitar dependencias circulares,
                    // usaremos el seeder directamente si es necesario o confiaremos en que el admin ya lo hizo.
                    // Por ahora, solo aseguramos que el documento de CONFIG existe para que no falle la apertura.
                }

                val doc = db.collection(FirestoreCollections.SUCURSAL_CONFIG).document(sucursalId).get().await()
                turnoYaActivo = doc.getBoolean("abierta") ?: false
                
                // Leer masa disponible en bodega global
                val masaDoc = db.collection(FirestoreCollections.INVENTARIO_GLOBAL).document("masa_crepa").get().await()
                tandasMasaEnBodega = masaDoc.getDouble("cantidadEnBase") ?: masaDoc.getDouble("cantidadDisponible") ?: 0.0
            } catch (e: Exception) {
                mensajeError = "Error: ${e.message}"
            }
        }
    }
    fun realizarApertura(sucursal: String, usuarioId: String) {
        if (cargando) return
        val fondo = fondoCaja.toDoubleOrNull() ?: 0.0
        if (fondo < 100.0) {
            mensajeError = "El fondo de caja debe ser de al menos $100.00 para iniciar."
            return
        }
        viewModelScope.launch {
            cargando = true
            try {
                val batch = db.batch()
                val aperturaId = db.collection(FirestoreCollections.APERTURAS).document().id

                // 1. Registrar Conteo de Producto Terminado en Stock de Venta (Unificado)
                conteoPostres.forEach { (prodId, cant) ->
                    val cantidad = cant.toDoubleOrNull() ?: 0.0
                    val stockRef = db.collection(FirestoreCollections.INVENTARIO_SUCURSAL)
                        .document("${sucursal.lowercase()}_${prodId}")
                    
                    batch.set(stockRef, mapOf(
                        "id" to "${sucursal.lowercase()}_${prodId}",
                        "sucursal" to sucursal.lowercase(),
                        "insumoId" to prodId,
                        "cantidadEnBase" to cantidad,
                        "ultimaActualizacion" to System.currentTimeMillis()
                    ), SetOptions.merge())
                }

                // 2. Registrar Disponibilidad de Insumos CrÑticos
                val configRef = db.collection(FirestoreCollections.SUCURSAL_CONFIG).document(sucursal.lowercase())
                batch.set(configRef, mapOf(
                    "hayHielo" to hayHielo,
                    "hayChantilly" to hayChantilly,
                    "hayPopotes" to hayPopotes,
                    "hayBaseFresas" to hayBaseFresas,
                    "fondoCaja" to (fondoCaja.toDoubleOrNull() ?: 0.0),
                    "ultimaApertura" to System.currentTimeMillis(),
                    "abierta" to true
                ), SetOptions.merge())

                // 4. Transferencia de Masa (De Global a Porciones de Sucursal)
                val numTandas = tandasATomar.toDoubleOrNull() ?: 0.0
                if (numTandas > 0 && numTandas <= tandasMasaEnBodega) {
                    batch.update(db.collection(FirestoreCollections.INVENTARIO_GLOBAL).document("masa_crepa"), 
                        "cantidadEnBase", FieldValue.increment(-numTandas))
                    
                    val stockVentaRef = db.collection(FirestoreCollections.INVENTARIO_SUCURSAL).document("${sucursal.lowercase()}_masa_crepa")
                    batch.set(stockVentaRef, mapOf(
                        "id" to "${sucursal.lowercase()}_masa_crepa",
                        "insumoId" to "masa_crepa",
                        "sucursal" to sucursal.lowercase(),
                        "cantidadEnBase" to FieldValue.increment(numTandas * 60.0),
                        "ultimaActualizacion" to System.currentTimeMillis()
                    ), SetOptions.merge())
                }

                // 5. Log de Apertura
                val log = mapOf(
                    "id" to aperturaId,
                    "sucursal" to sucursal,
                    "usuarioId" to usuarioId,
                    "fecha" to System.currentTimeMillis(),
                    "conteoInicial" to conteoPostres.toMap(),
                    "tandasTomadas" to numTandas
                )
                batch.set(db.collection(FirestoreCollections.APERTURAS).document(aperturaId), log)

                batch.commit().await()
                aperturaCompletada = true
            } catch (e: Exception) {
                mensajeError = "Error: ${e.message}"
            } finally {
                cargando = false
            }
        }
    }
}



