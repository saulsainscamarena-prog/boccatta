package com.bocatta.pos.presentation.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.viewModelScope
import com.bocatta.pos.domain.model.*
import com.bocatta.pos.network.firebase.FirebaseFirestoreProvider
import com.bocatta.pos.core.constants.FirestoreCollections
import com.bocatta.pos.data.repository.DataSeederV2
import com.bocatta.pos.data.repository.MaintenanceRepository
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


class AdminViewModel(
    private val maintRepo: MaintenanceRepository = MaintenanceRepository(),
    private val dataSeeder: DataSeederV2 = DataSeederV2()
) : BaseViewModel() {
    // Listener for dynamic categories
    private var listenerCategorias: ListenerRegistration? = null
    var categorias = mutableStateListOf<Categoria>()
        private set

    private val db = FirebaseFirestoreProvider.db
    private var usuarioActual: Usuario? = null

    private var listenerInsumos: ListenerRegistration? = null
    private var listenerInsumosAuto: ListenerRegistration? = null
    private var listenerGastos: ListenerRegistration? = null
    private var listenerRecetas: ListenerRegistration? = null
    private var listenerProductos: ListenerRegistration? = null
    private var listenerUsuarios: ListenerRegistration? = null
    private var listenerCancelaciones: ListenerRegistration? = null
    private var listenerConfigCaja: ListenerRegistration? = null
    private var listenerConfigSeguridad: ListenerRegistration? = null
    private var seederEjecutado = false
    var seederEnProgreso by mutableStateOf(false)
        private set

    var productos = mutableStateListOf<SalesInventoryProductV2>()
        private set
    var insumosMaestros = mutableStateListOf<InsumoV2>()
        private set
    var historialVentasV2 = mutableStateListOf<VentaV2>()
        private set
    var cancelacionesPendientes = mutableStateListOf<Map<String, Any>>()
        private set
    var configCaja by mutableStateOf<Map<String, Any>>(emptyMap())
        private set
    var configSeguridad by mutableStateOf<Map<String, Any>>(emptyMap())
        private set
    var usuarios = mutableStateListOf<Usuario>()
        private set
    var recetas = mutableStateMapOf<String, RecetaV2>()
        private set
    var totalGastosHoy by mutableStateOf(0.0)
        private set

    fun configurarUsuario(usuario: Usuario?) {
        usuarioActual = usuario
    }

    private fun puedeEjecutarOperacionCritica(): Boolean {
        return usuarioActual?.rol == Rol.ADMIN || usuarioActual?.rol == Rol.DUEÑO
    }

    init {
        escucharProductos()
        escucharUsuarios()
        cargarHistorial()
        escucharCancelaciones()
        escucharRecetas()
        escucharGastosHoy()
        escucharConfigGlobal()
        escucharInsumosMaestrosAutomatico()
        escucharCategorias()
    }

    private fun escucharInsumosMaestrosAutomatico() {
        listenerInsumosAuto?.remove()
        listenerInsumosAuto = db.collection(FirestoreCollections.INVENTARIO_GLOBAL).addSnapshotListener { snap, _ ->
            if (snap != null) {
                insumosMaestros.clear()
                snap.documents.forEach { doc ->
                    val data = doc.data
                    if (data != null) {
                        insumosMaestros.add(InsumoV2(
                            id = doc.id,
                            nombre = data["nombre"]?.toString() ?: doc.id,
                            cantidadEnBase = (data["cantidadEnBase"] as? Number)?.toDouble()
                                ?: (data["cantidadDisponible"] as? Number)?.toDouble()
                                ?: 0.0,
                            categoria = data["categoria"]?.toString() ?: "Bodega",
                            unidadBase = data["unidadBase"]?.toString()
                                ?: data["unidadMedida"]?.toString()
                                ?: data["unidadMedidaMinima"]?.toString()
                                ?: "g"
                        ))
                    }
                }
            }
        }
    }

    private fun escucharConfigGlobal() {
        listenerConfigCaja?.remove()
        listenerConfigSeguridad?.remove()
        listenerConfigCaja = db.collection(FirestoreCollections.CONFIGURACION).document("parametros_caja").addSnapshotListener { snap, _ ->
            configCaja = snap?.data ?: emptyMap()
        }
        listenerConfigSeguridad = db.collection(FirestoreCollections.CONFIGURACION).document("seguridad").addSnapshotListener { snap, _ ->
            configSeguridad = snap?.data ?: emptyMap()
        }
    }

    fun escucharInsumosMaestros(rolUsuario: Rol) {
        if (rolUsuario != Rol.DUEÑO && rolUsuario != Rol.ADMIN) {
            mensajeError = "Acceso Denegado: Se requiere rol Administrativo."
            return
        }
        listenerInsumos?.remove()
        listenerInsumos = db.collection(FirestoreCollections.INSUMOS).addSnapshotListener { snap, _ ->
            if (snap != null) {
                if (snap.isEmpty && !seederEjecutado) {
                    seederEjecutado = true
                    seederEnProgreso = true
                    viewModelScope.launch(safeHandler) {
                        dataSeeder.inicializarTodoV2()
                        seederEnProgreso = false
                    }
                } else if (!snap.isEmpty) {
                    insumosMaestros.clear()
                    snap.documents.forEach { doc ->
                        doc.toObject(InsumoV2::class.java)?.let {
                            insumosMaestros.add(it.copy(id = doc.id))
                        }
                    }
                }
            }
        }
    }

    fun actualizarCostoInsumo(insumoId: String, nuevoCosto: Double) {
        viewModelScope.launch(safeHandler) {
            db.collection(FirestoreCollections.INSUMOS).document(insumoId).update("costoUnitarioBase", nuevoCosto).await()
            mensajeExito = "Costo actualizado ?"
        }
    }

    private fun escucharCategorias() {
        listenerCategorias?.remove()
        listenerCategorias = db.collection(FirestoreCollections.CATEGORIAS).addSnapshotListener { snap, _ ->
            if (snap != null) {
                categorias.clear()
                snap.documents.forEach { doc ->
                    categorias.add(Categoria(id = doc.id, nombre = doc.getString("nombre") ?: doc.id))
                }
            }
        }
    }

    fun agregarCategoria(nombre: String): Result<String> {
        return try {
            val nombreNorm = nombre.trim()
            check(nombreNorm.isNotEmpty()) { "El nombre no puede estar vacÑo" }
            // Verificar duplicado
            if (categorias.any { it.nombre.equals(nombreNorm, ignoreCase = true) }) {
                throw Exception("Ya existe una categorÑa con ese nombre")
            }
            val docRef = db.collection(FirestoreCollections.CATEGORIAS).document()
            val nuevoId = docRef.id
            viewModelScope.launch {
                try {
                    docRef.set(mapOf("nombre" to nombreNorm)).await()
                    categorias.add(Categoria(id = nuevoId, nombre = nombreNorm))
                } catch (e: Exception) {
                    mensajeError = "Error: ${e.message}"
                }
            }
            Result.success(nuevoId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun agregarInsumo(insumo: InsumoV2): Result<String> {
        return try {
            val nombreNorm = insumo.nombre.trim()
            check(nombreNorm.isNotEmpty()) { "El nombre no puede estar vacÑo" }
            // Verificar duplicado en lista local
            val existente = insumosMaestros.find { it.nombre.equals(nombreNorm, ignoreCase = true) }
            if (existente != null) {
                mensajeExito = "Insumo ya existe, se usarÑ el existente: ${existente.id}"
                return Result.success(existente.id)
            }
            // Crear nuevo
            val docRef = db.collection(FirestoreCollections.INSUMOS).document()
            val nuevoId = docRef.id
            val nuevo = insumo.copy(id = nuevoId)
            docRef.set(nuevo).await()
            insumosMaestros.add(nuevo)
            mensajeExito = "Insumo creado correctamente"
            Result.success(nuevoId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun validarProducto(producto: SalesInventoryProductV2): Boolean {
        return producto.nombre.isNotBlank() &&
                producto.precioVenta.values.any { it >= 0 } &&
                producto.categoria.isNotBlank() &&
                (!producto.esCombo || producto.productosCombo.isNotEmpty()) &&
                producto.consumiblesAsociados.all { it.cantidad > 0 }
    }

    private fun escucharGastosHoy() {
        val hoy = java.util.Calendar.getInstance().apply { 
            set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0); set(java.util.Calendar.SECOND, 0) 
        }.timeInMillis
        listenerGastos?.remove()
        listenerGastos = db.collection(FirestoreCollections.GASTOS)
            .whereGreaterThanOrEqualTo("fecha", hoy)
            .addSnapshotListener { snap, _ ->
                totalGastosHoy = snap?.documents?.sumOf { it.getDouble("monto") ?: 0.0 } ?: 0.0
            }
    }

    private fun escucharRecetas() {
        listenerRecetas?.remove()
        listenerRecetas = db.collection(FirestoreCollections.RECETAS).addSnapshotListener { snap, _ ->
            if (snap != null) {
                recetas.clear()
                snap.documents.forEach { doc ->
                    doc.toObject(com.bocatta.pos.domain.model.RecetaV2::class.java)?.let { recetas[it.productoId ?: doc.id] = it }
                }
            }
        }
    }

    fun guardarReceta(receta: com.bocatta.pos.domain.model.RecetaV2) {
        viewModelScope.launch(safeHandler) {
            db.collection(FirestoreCollections.RECETAS).document(receta.id).set(receta).await()
            mensajeExito = "Receta de ${receta.nombre} guardada ?"
        }
    }

    private fun escucharProductos() {
        listenerProductos?.remove()
        listenerProductos = db.collection(FirestoreCollections.PRODUCTOS).addSnapshotListener { snap, _ ->
            if (snap != null) {
                productos.clear()
                snap.documents.forEach { doc ->
                    doc.toObject(SalesInventoryProductV2::class.java)?.let { productos.add(it.copy(id = doc.id)) }
                }
            }
        }
    }

    private fun escucharUsuarios() {
        listenerUsuarios?.remove()
        listenerUsuarios = db.collection(FirestoreCollections.USUARIOS).addSnapshotListener { snap, _ ->
            if (snap != null) {
                usuarios.clear()
                snap.documents.forEach { doc ->
                    doc.toObject(Usuario::class.java)?.let { usuarios.add(it.copy(uid = doc.id)) }
                }
            }
        }
    }

    fun cargarHistorial() {
        viewModelScope.launch(safeHandler) {
            val snap = db.collection(FirestoreCollections.VENTAS)
                .orderBy("fecha", Query.Direction.DESCENDING)
                .limit(50)
                .get().await()
            historialVentasV2.clear()
            snap.documents.forEach { doc ->
                doc.toObject(VentaV2::class.java)?.let { historialVentasV2.add(it.copy(id = doc.id)) }
            }
        }
    }

    fun agregarProducto(producto: SalesInventoryProductV2) {
        viewModelScope.launch(safeHandler) {
            db.collection(FirestoreCollections.PRODUCTOS).document(producto.id).set(producto).await()
            mensajeExito = "Producto agregado ?"
        }
    }

    fun editarProducto(producto: SalesInventoryProductV2) {
        viewModelScope.launch(safeHandler) {
            db.collection(FirestoreCollections.PRODUCTOS).document(producto.id).set(producto).await()
            mensajeExito = "Producto actualizado ?"
        }
    }

    fun eliminarProducto(producto: SalesInventoryProductV2) {
        viewModelScope.launch(safeHandler) {
            db.collection(FirestoreCollections.PRODUCTOS).document(producto.id).delete().await()
            mensajeExito = "Producto eliminado ?"
        }
    }

    fun escucharCancelaciones() {
        listenerCancelaciones?.remove()
        listenerCancelaciones = db.collection(FirestoreCollections.CANCELACIONES)
            .orderBy("fecha", Query.Direction.DESCENDING)
            .limit(20)
            .addSnapshotListener { snap, _ ->
                if (snap != null) {
                    cancelacionesPendientes.clear()
                    snap.documents.forEach { doc -> cancelacionesPendientes.add(doc.data ?: emptyMap()) }
                }
            }
    }

    fun revisarCancelacion(id: String) {
        viewModelScope.launch(safeHandler) {
            db.collection(FirestoreCollections.CANCELACIONES).document(id).delete().await()
            mensajeExito = "Registro de auditorÑa archivado ?"
        }
    }

    fun realizarLimpiezaTotal() {
        if (!puedeEjecutarOperacionCritica()) {
            mensajeError = "? Sin permisos: Solo ADMIN o DUEÑO pueden ejecutar esta operaciÑn."
            return
        }
        viewModelScope.launch(safeHandler) {
            cargando = true
            mensajeExito = "Iniciando purga total... por favor espera."
            val result = maintRepo.purgaTotal()
            result.onSuccess {
                maintRepo.inicializarEstructuraV2()
                mensajeExito = "LIMPIEZA COMPLETA. Base de datos V2 reseteada ?"
                productos.clear(); usuarios.clear(); historialVentasV2.clear(); insumosMaestros.clear(); recetas.clear()
            }.onFailure {
                mensajeError = "Error durante la limpieza: ${it.message}"
            }
            cargando = false
        }
    }

    fun inicializarV2() {
        if (!puedeEjecutarOperacionCritica()) {
            mensajeError = "? Sin permisos: Solo ADMIN o DUEÑO pueden ejecutar esta operaciÑn."
            return
        }
        viewModelScope.launch(safeHandler) {
            cargando = true
            mensajeExito = "Inyectando inteligencia V2..."
            dataSeeder.inicializarTodoV2().onSuccess {
                mensajeExito = "SISTEMA V2 LISTO ?"
            }.onFailure {
                mensajeError = "Error al inyectar V2: ${it.message}"
            }
            cargando = false
        }
    }

    fun ajustarStock(insumoId: String, nuevoStock: Double) {
        viewModelScope.launch(safeHandler) {
            val batch = db.batch()
            val globalRef = db.collection(FirestoreCollections.INVENTARIO_GLOBAL).document(insumoId)
            batch.set(globalRef, mapOf("cantidadEnBase" to nuevoStock), com.google.firebase.firestore.SetOptions.merge())
            batch.update(globalRef, "ultimaActualizacion", System.currentTimeMillis())
            batch.commit().await()
            mensajeExito = "Stock V2 ajustado ?"
        }
    }

    fun registrarGastoNegocio(gasto: GastoV2) {
        viewModelScope.launch(safeHandler) {
            db.collection(FirestoreCollections.GASTOS).document(gasto.id).set(gasto).await()
            mensajeExito = "Gasto V2 registrado ?"
        }
    }

    fun registrarCompraInsumo(gasto: GastoV2, insumoId: String, cantidad: Double) {
        viewModelScope.launch(safeHandler) {
            val batch = db.batch()
            batch.set(db.collection(FirestoreCollections.GASTOS).document(gasto.id), gasto)
            val stockRef = db.collection(FirestoreCollections.INVENTARIO_GLOBAL).document(insumoId)
            batch.set(stockRef, mapOf("cantidadEnBase" to FieldValue.increment(cantidad)), com.google.firebase.firestore.SetOptions.merge())
            batch.update(stockRef, "ultimaActualizacion", System.currentTimeMillis())
            batch.commit().await()
            mensajeExito = "Compra e inventario V2 registrados ?"
        }
    }

    fun actualizarParametrosCaja(toleranciaEf: Double, toleranciaTar: Double) {
        viewModelScope.launch(safeHandler) {
            val data = mapOf(
                "tolerancia_efectivo" to toleranciaEf,
                "tolerancia_tarjeta" to toleranciaTar,
                "ultimaActualizacion" to System.currentTimeMillis()
            )
            db.collection(FirestoreCollections.CONFIGURACION).document("parametros_caja").set(data).await()
            mensajeExito = "ParÑmetros de caja actualizados ?"
        }
    }

    fun actualizarSeguridad(codigoMaestro: String, codigoEmpleado: String) {
        viewModelScope.launch(safeHandler) {
            val data = mapOf(
                "codigo_maestro" to codigoMaestro,
                "codigo_maestro_empleado" to codigoEmpleado,
                "ultimaActualizacion" to System.currentTimeMillis()
            )
            db.collection(FirestoreCollections.CONFIGURACION).document("seguridad").set(data).await()
            mensajeExito = "Seguridad actualizada ?"
        }
    }

    override fun onCleared() {
        super.onCleared()
        listenerCategorias?.remove()
        listenerInsumos?.remove(); listenerInsumosAuto?.remove()
        listenerGastos?.remove(); listenerRecetas?.remove()
        listenerProductos?.remove(); listenerUsuarios?.remove()
        listenerCancelaciones?.remove()
        listenerConfigCaja?.remove(); listenerConfigSeguridad?.remove()
    }
}


