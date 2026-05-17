package com.bocatta.pos.presentation.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.viewModelScope
import com.bocatta.pos.core.constants.FirestoreCollections
import com.bocatta.pos.data.repository.DataSeederV2
import com.bocatta.pos.data.repository.ProductoRepository
import com.bocatta.pos.network.firebase.FirebaseFirestoreProvider
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber

data class SucursalInfo(
    val id: String = "",
    val nombre: String = "",
    val ciudad: String = "",
    val activa: Boolean = true,
    val creadaEn: Long = 0L,
    val totalProductos: Int = 0,
    val catalogoVersion: Long = 0L
)

/**
 * GestionSucursalesViewModel
 *
 * Maneja el ciclo completo de apertura de sucursales:
 * 1. Listar sucursales existentes
 * 2. Crear nueva sucursal con inicialización automática de inventario
 * 3. Activar/desactivar sucursales
 * 4. Estado en tiempo real de cada sucursal
 */
class GestionSucursalesViewModel(
    private val seeder: DataSeederV2,
    private val productoRepo: ProductoRepository
) : BaseViewModel() {

    private val db = FirebaseFirestoreProvider.db
    private var listenerSucursales: ListenerRegistration? = null

    var sucursales = mutableStateListOf<SucursalInfo>()
        private set

    var iniciandoSucursal by mutableStateOf(false)
        private set
    var progresoMensaje by mutableStateOf("")
        private set

    init {
        escucharSucursales()
    }

    private fun escucharSucursales() {
        listenerSucursales?.remove()
        listenerSucursales = db.collection(FirestoreCollections.SUCURSALES)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    Timber.tag("SUCURSALES").e(error)
                    return@addSnapshotListener
                }
                if (snap != null) {
                    sucursales.clear()
                    snap.documents.forEach { doc ->
                        sucursales.add(
                            SucursalInfo(
                                id = doc.id,
                                nombre = doc.getString("nombre") ?: doc.id,
                                ciudad = doc.getString("ciudad") ?: "",
                                activa = doc.getBoolean("activa") ?: true,
                                creadaEn = doc.getLong("creadaEn") ?: 0L,
                                totalProductos = (doc.getLong("totalProductos") ?: 0L).toInt(),
                                catalogoVersion = doc.getLong("catalogoVersion") ?: 0L
                            )
                        )
                    }
                }
            }
    }

    /**
     * Crea una nueva sucursal e inicializa su inventario completo.
     * Tiempo estimado: 2-5 segundos dependiendo del número de insumos.
     *
     * Flujo:
     * 1. Registra la sucursal en Firestore
     * 2. Ejecuta DataSeeder.inicializarSucursal() → crea stock a cero para todos los insumos
     * 3. Referencia el catálogo global en la sucursal
     * 4. Crea el contador de tickets
     */
    fun crearSucursal(nombre: String, ciudad: String) {
        if (nombre.isBlank()) {
            mensajeError = "El nombre de la sucursal no puede estar vacío"
            return
        }

        val sucursalId = nombre.lowercase()
            .trim()
            .replace(" ", "_")
            .replace(Regex("[^a-z0-9_]"), "")

        if (sucursales.any { it.id == sucursalId }) {
            mensajeError = "Ya existe una sucursal con ese nombre"
            return
        }

        viewModelScope.launch(safeHandler) {
            iniciandoSucursal = true

            try {
                // Paso 1: Registrar sucursal
                progresoMensaje = "Registrando sucursal..."
                db.collection(FirestoreCollections.SUCURSALES)
                    .document(sucursalId)
                    .set(mapOf(
                        "id" to sucursalId,
                        "nombre" to nombre,
                        "ciudad" to ciudad,
                        "activa" to true,
                        "creadaEn" to System.currentTimeMillis()
                    ), SetOptions.merge())
                    .await()

                // Paso 2: Inicializar inventario (stock a cero para todos los insumos)
                progresoMensaje = "Inicializando inventario..."
                seeder.inicializarSucursal(sucursalId).onFailure { e ->
                    throw Exception("Error inicializando inventario: ${e.message}")
                }

                // Paso 3: Referenciar catálogo
                progresoMensaje = "Vinculando catálogo de productos..."
                productoRepo.replicarCatalogoASucursal(sucursalId)

                // Paso 4: Configuración inicial de caja
                progresoMensaje = "Configurando caja..."
                db.collection(FirestoreCollections.CONFIGURACION)
                    .document("parametros_caja_$sucursalId")
                    .set(mapOf(
                        "sucursal" to sucursalId,
                        "tolerancia_efectivo" to 10.0,
                        "tolerancia_tarjeta" to 5.0,
                        "fondo_minimo" to 100.0,
                        "creadaEn" to System.currentTimeMillis()
                    ), SetOptions.merge())
                    .await()

                mensajeExito = "✅ Sucursal '$nombre' lista en segundos"
                Timber.tag("SUCURSALES").i("Sucursal $sucursalId creada exitosamente")

            } catch (e: Exception) {
                mensajeError = "Error creando sucursal: ${e.message}"
                Timber.tag("SUCURSALES").e(e)
            } finally {
                iniciandoSucursal = false
                progresoMensaje = ""
            }
        }
    }

    fun toggleActivarSucursal(sucursalId: String, activar: Boolean) {
        viewModelScope.launch(safeHandler) {
            db.collection(FirestoreCollections.SUCURSALES)
                .document(sucursalId)
                .update("activa", activar)
                .await()
            mensajeExito = if (activar) "Sucursal activada ✓" else "Sucursal desactivada"
        }
    }

    fun resetearInventarioSucursal(sucursalId: String) {
        viewModelScope.launch(safeHandler) {
            cargando = true
            seeder.resetearInventarioSucursal(sucursalId)
                .onSuccess { mensajeExito = "Inventario de $sucursalId reseteado a cero ✓" }
                .onFailure { mensajeError = "Error: ${it.message}" }
            cargando = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        listenerSucursales?.remove()
    }
}

