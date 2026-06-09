package com.bocatta.pos.presentation.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.viewModelScope
import com.bocatta.pos.data.local.OfflineDatabase
import com.bocatta.pos.data.local.TurnoContingenciaLocal
import com.bocatta.pos.data.local.room.dao.VentaPendienteDao
import com.bocatta.pos.data.local.room.entity.VentaPendienteEntity
import com.bocatta.pos.data.repository.StockAllocationRepository
import com.bocatta.pos.domain.model.Rol
import com.bocatta.pos.domain.model.Usuario
import com.bocatta.pos.domain.model.RetiroParcialV2
import com.bocatta.pos.domain.model.TurnoCajaV2
import com.bocatta.pos.domain.usecase.AccionSensible
import com.bocatta.pos.network.firebase.FirebaseFirestoreProvider
import com.bocatta.pos.core.constants.FirestoreCollections
import com.bocatta.pos.domain.CuadreCajaManager
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class CajaViewModel(
    private val authManager: com.bocatta.pos.domain.usecase.AuthorizationManager,
    private val offlineDb: OfflineDatabase,
    private val ventaPendienteDao: VentaPendienteDao
) : BaseViewModel() {
    private val db = FirebaseFirestoreProvider.db
    private val allocationRepo = StockAllocationRepository()

    private var listenerTurno: ListenerRegistration? = null
    private var listenerRetiros: ListenerRegistration? = null
    private var listenerVentas: ListenerRegistration? = null
    private var listenerGastos: ListenerRegistration? = null
    private var listenerCancelaciones: ListenerRegistration? = null
    private var listenerParametros: ListenerRegistration? = null

    var turnoActivo by mutableStateOf<TurnoCajaV2?>(null)
    var denominacionesInput = mutableStateMapOf<String, String>()
    var retiroList = mutableStateListOf<com.bocatta.pos.domain.model.RetiroParcialV2>()
        private set
    var cargandoTurno by mutableStateOf(false)
        private set
    var errorTurno by mutableStateOf<String?>(null)
        private set
    var modoContingenciaLocal by mutableStateOf(false)
        private set

    // CANDADO DE SEGURIDAD
    var tieneCancelacionesPendientes by mutableStateOf(false)
        private set
    var numCancelacionesPendientes by mutableIntStateOf(0)
        private set

    // VENTAS OFFLINE FALLIDAS — badge presionable en Caja (solo admin puede actuar)
    private val _conteoVentasFallidas = MutableStateFlow(0)
    val conteoVentasFallidas = _conteoVentasFallidas.asStateFlow()
    var ventasFallidas by mutableStateOf<List<com.bocatta.pos.data.local.VentaOffline>>(emptyList())
        private set
    var mostrarDialogoFallidas by mutableStateOf(false)
        private set

    fun verificarPendientes() {
        val sucursal = sucursalFiltro
        listenerCancelaciones?.remove()
        var query = db.collection(FirestoreCollections.CANCELACIONES)
            .whereEqualTo("estado", "pendiente_revision")
        if (sucursal != null) {
            query = query.whereEqualTo("sucursal", sucursal.lowercase(java.util.Locale.getDefault()))
        }
        listenerCancelaciones = query.addSnapshotListener { snap, _ ->
            numCancelacionesPendientes = snap?.size() ?: 0
            tieneCancelacionesPendientes = numCancelacionesPendientes > 0
        }
    }

    // Totales del día calculados en tiempo real
    var totalEfectivoSistema by mutableDoubleStateOf(0.0)
        private set
    var totalTarjetaSistema by mutableDoubleStateOf(0.0)
        private set
    var totalGastosSistema by mutableDoubleStateOf(0.0)
        private set

    var sucursalFiltro by mutableStateOf<String?>(null)

    // Parámetros de configuración remotos
    var toleranciaEfectivo by mutableStateOf(10.0)
    var toleranciaTarjeta by mutableStateOf(5.0)

        // Inputs del empleado al contar
        var efectivoContado by mutableStateOf("")
        var tarjetaContada by mutableStateOf("")

        // 2.4 actualizarDenominaciones - actualiza mapa de denominaciones y total contado
        fun actualizarDenominaciones(map: Map<String, Int>) {
            // actualizar el snapshotStateMap con los nuevos valores
            map.forEach { (denom, qty) ->
                denominacionesInput[denom] = qty.toString()
            }
            // calcular total en efectivo
            val total = denominacionesInput.entries.sumOf { (denom, qtyStr) ->
                val denomVal = denom.toIntOrNull() ?: 0
                val qtyVal = qtyStr.toIntOrNull() ?: 0
                denomVal * qtyVal
            }
            efectivoContado = total.toString()
        }


    val resultadoCuadreActual: CuadreCajaManager.ResultadoCuadre?
        get() {
            val turno = turnoActivo ?: return null
            val efContado = efectivoContado.toDoubleOrNull() ?: 0.0
            return CuadreCajaManager.calcularCuadre(
                ventasTotal = java.math.BigDecimal.valueOf(totalEfectivoSistema),
                gastosTotal = java.math.BigDecimal.valueOf(totalGastosSistema),
                fondoInicial = java.math.BigDecimal.valueOf(turno.fondoInicial),
                efectivoEnCaja = java.math.BigDecimal.valueOf(efContado),
                toleranciaEfectivo = java.math.BigDecimal.valueOf(toleranciaEfectivo),
                toleranciaTarjeta = java.math.BigDecimal.valueOf(toleranciaTarjeta)
            )
        }

    val diferenciaCaja: Double
        get() = resultadoCuadreActual?.diferencia?.toDouble() ?: 0.0

    val diferenciaTarjeta: Double
        get() = (tarjetaContada.toDoubleOrNull() ?: 0.0) - totalTarjetaSistema

    val cadraCaja: Boolean
        get() = resultadoCuadreActual?.esCorrecto ?: true

    init {
        escucharTurnoActivo()
        escucharTotalesDia()
        verificarPendientes()
        escucharParametrosCaja()
        iniciarObservacionFallidas()
    }

    private fun iniciarObservacionFallidas() {
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                try {
                    val count = offlineDb.contarVentasFallidas()
                    _conteoVentasFallidas.value = count
                } catch (e: Exception) {
                    timber.log.Timber.e(e, "Error polling failed sales count")
                }
                kotlinx.coroutines.delay(3000L)
            }
        }
    }

    private fun escucharParametrosCaja() {
        listenerParametros?.remove()
        listenerParametros = db.collection(FirestoreCollections.CONFIGURACION).document("parametros_caja")
            .addSnapshotListener { doc, _ ->
                if (doc != null && doc.exists()) {
                    toleranciaEfectivo = doc.getDouble("tolerancia_efectivo") ?: 10.0
                    toleranciaTarjeta = doc.getDouble("tolerancia_tarjeta") ?: 5.0
                }
            }
    }

    fun configurarSucursal(sucursal: String) {
        sucursalFiltro = normalizarSucursal(sucursal)
        escucharTurnoActivo()
        escucharTotalesDia()
        verificarPendientes()
    }

    private fun escucharTurnoActivo() {
        val sucursal = sucursalFiltro ?: return
        val turnoId = "${sucursal}_${SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())}"
        listenerTurno?.remove()
        cargandoTurno = true
        errorTurno = null
        listenerTurno = db.collection(FirestoreCollections.TURNOS_CAJA)
            .document(turnoId)
            .addSnapshotListener { doc, error ->
                if (error != null) {
                    errorTurno = error.message
                    cargarTurnoContingenciaLocal(sucursal)
                    cargandoTurno = false
                    return@addSnapshotListener
                }
                if (doc != null && doc.exists() && doc.getString("estado") == "abierto") {
                    turnoActivo = doc.toObject(TurnoCajaV2::class.java)?.copy(id = doc.id)
                    modoContingenciaLocal = false
                    cargandoTurno = false
                    escucharRetiros()
                } else {
                    viewModelScope.launch {
                        turnoActivo = buscarTurnoAbiertoLegacy(sucursal) ?: cargarTurnoContingenciaLocal(sucursal)
                        cargandoTurno = false
                    }
                }
            }
    }

    private suspend fun buscarTurnoAbiertoLegacy(sucursal: String): TurnoCajaV2? {
        return try {
            val snap = db.collection(FirestoreCollections.TURNOS_CAJA)
                .whereEqualTo("sucursal", sucursal)
                .whereEqualTo("estado", "abierto")
                .limit(1)
                .get()
                .await()
            snap.documents.firstOrNull()?.let { doc ->
                doc.toObject(TurnoCajaV2::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            errorTurno = e.message
            cargarTurnoContingenciaLocal(sucursal)
            null
        }
    }

    private fun cargarTurnoContingenciaLocal(sucursal: String): TurnoCajaV2? {
        val local = offlineDb.obtenerTurnoContingenciaAbierto(normalizarSucursal(sucursal)) ?: return null
        val turno = local.toTurnoCajaV2()
        turnoActivo = turno
        modoContingenciaLocal = true
        return turno
    }

    private fun escucharRetiros() {
        val turno = turnoActivo ?: return
        listenerRetiros?.remove()
        listenerRetiros = db.collection(FirestoreCollections.TURNOS_CAJA)
            .document(turno.id)
            .collection(FirestoreCollections.RETIROS)
            .addSnapshotListener { snap, _ ->
                retiroList.clear()
                snap?.documents?.forEach { doc ->
                    doc.toObject(com.bocatta.pos.domain.model.RetiroParcialV2::class.java)?.let { retiroList.add(it) }
                }
            }
    }

    private fun escucharTotalesDia() {
        val sucursal = sucursalFiltro ?: return
        val inicioDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
        }.timeInMillis

        listenerVentas?.remove()
        listenerVentas = db.collection(FirestoreCollections.VENTAS)
            .whereEqualTo("sucursal", sucursal.lowercase(java.util.Locale.getDefault()))
            .whereGreaterThanOrEqualTo("fecha", inicioDay)
            .addSnapshotListener { snap, _ ->
                viewModelScope.launch(Dispatchers.Default) {
                    var efectivo = 0.0
                    var tarjeta = 0.0
                    snap?.documents?.forEach { doc ->
                        if (doc.getString("estado") == "devuelta") return@forEach
                        val total = doc.getDouble("total") ?: 0.0
                        val metodoPago = doc.getString("metodoPago") ?: "Efectivo"
                        val (efec, tarj) = parsePaymentSplit(metodoPago, total)
                        efectivo += efec
                        tarjeta += tarj
                    }
                    withContext(Dispatchers.Main) {
                        totalEfectivoSistema = efectivo
                        totalTarjetaSistema = tarjeta
                    }
                }
            }

        listenerGastos?.remove()
        listenerGastos = db.collection(FirestoreCollections.GASTOS)
            .whereEqualTo("sucursal", sucursal.lowercase(java.util.Locale.getDefault()))
            .whereGreaterThanOrEqualTo("fecha", inicioDay)
            .addSnapshotListener { snap, _ ->
                viewModelScope.launch(Dispatchers.Default) {
                    val total = snap?.documents?.sumOf { it.getDouble("monto") ?: 0.0 } ?: 0.0
                    withContext(Dispatchers.Main) {
                        totalGastosSistema = total
                    }
                }
            }
    }

    private fun parsePaymentSplit(metodoPago: String, total: Double): Pair<Double, Double> {
        val cleanPago = metodoPago.trim()
        if (cleanPago.startsWith("Mixto:") || cleanPago.startsWith("Dividido:")) {
            val start = cleanPago.indexOf('[')
            val end = cleanPago.indexOf(']')
            if (start != -1 && end != -1 && end > start) {
                val content = cleanPago.substring(start + 1, end)
                val parts = content.split(",")
                var tarjetaAmount = 0.0
                var efectivoAmount = 0.0
                for (part in parts) {
                    val trimmed = part.trim()
                    if (trimmed.contains("Tarjeta", ignoreCase = true) ||
                        trimmed.contains("Rappi", ignoreCase = true) ||
                        trimmed.contains("Uber", ignoreCase = true) ||
                        trimmed.contains("DiDi", ignoreCase = true) ||
                        trimmed.contains("Transferencia", ignoreCase = true)) {
                        val amountStr = trimmed.replace("Tarjeta", "", ignoreCase = true)
                            .replace("Rappi", "", ignoreCase = true)
                            .replace("Uber Eats", "", ignoreCase = true)
                            .replace("UberEats", "", ignoreCase = true)
                            .replace("Uber", "", ignoreCase = true)
                            .replace("DiDi Food", "", ignoreCase = true)
                            .replace("DiDiFood", "", ignoreCase = true)
                            .replace("DiDi", "", ignoreCase = true)
                            .replace("Transferencia", "", ignoreCase = true)
                            .replace("$", "").trim()
                        tarjetaAmount += amountStr.toDoubleOrNull() ?: 0.0
                    } else if (trimmed.contains("Efectivo", ignoreCase = true)) {
                        val amountStr = trimmed.replace("Efectivo", "", ignoreCase = true)
                            .replace("$", "").trim()
                        efectivoAmount += amountStr.toDoubleOrNull() ?: 0.0
                    }
                }
                if (tarjetaAmount > 0.0 || efectivoAmount > 0.0) {
                    return Pair(efectivoAmount, tarjetaAmount)
                }
            }
        }

        if (cleanPago.contains("Tarjeta", ignoreCase = true) ||
            cleanPago.contains("Rappi", ignoreCase = true) ||
            cleanPago.contains("Uber", ignoreCase = true) ||
            cleanPago.contains("DiDi", ignoreCase = true) ||
            cleanPago.contains("Transferencia", ignoreCase = true)) {
            return Pair(0.0, total)
        }

        return Pair(total, 0.0)
    }

    fun abrirTurno(
        fondoInicial: Double,
        sucursal: String,
        usuario: Usuario?,
        onResult: ((Boolean) -> Unit)? = null
    ) {
        val turnoLocal = turnoActivo
        if (turnoLocal != null) {
            mensajeExito = "Turno activo cargado"
            onResult?.invoke(true)
            return
        }

        // VALIDACIÓN DE SEGURIDAD (DUEÑO/EMPLEADO)
        if (fondoInicial < 100.0) {
            mensajeError = "El fondo inicial debe ser de al menos $100.00 pesos."
            onResult?.invoke(false)
            return
        }

        viewModelScope.launch {
            cargando = true
            var exito = false
            try {
                if (usuario == null) {
                    mensajeError = "Usuario de sesión no válido."
                    onResult?.invoke(false)
                    cargando = false
                    return@launch
                }

                // VALIDACIÓN DE PERMISOS DINÁMICOS
                val tienePermiso = authManager.verificarPermiso(usuario, AccionSensible.ABRIR_TURNO)
                if (!tienePermiso) {
                    mensajeError = "No tienes permiso para abrir turno. Requiere rol de encargado o administrador."
                    onResult?.invoke(false)
                    cargando = false
                    return@launch
                }

                val sucursalId = normalizarSucursal(sucursal)
                val id = "${sucursalId}_${SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())}"
                val turnoRef = db.collection(FirestoreCollections.TURNOS_CAJA).document(id)
                val configRef = db.collection(FirestoreCollections.SUCURSAL_CONFIG).document(sucursalId)
                val turno = TurnoCajaV2(
                    id = id,
                    sucursal = sucursalId,
                    fechaApertura = System.currentTimeMillis(),
                    fondoInicial = fondoInicial,
                    usuarioResponsable = usuario.nombre,
                    estado = "abierto"
                )
                val turnoFinal = db.runTransaction { tx ->
                    val actual = tx.get(turnoRef)
                    if (actual.exists() && actual.getString("estado") == "abierto") {
                        actual.toObject(TurnoCajaV2::class.java)?.copy(id = actual.id) ?: turno
                    } else {
                        tx.set(turnoRef, turno)
                        tx.set(
                            configRef,
                            mapOf("abierta" to true, "turnoActivoId" to id, "ultimaApertura" to System.currentTimeMillis()),
                            SetOptions.merge()
                        )
                        turno
                    }
                }.await()
                turnoActivo = turnoFinal
                allocationRepo.redistribuirSucursalesAbiertas(usuario.nombre)

                // Registramos en la bitácora de auditoría
                authManager.registrarAuditoria(
                    empleadoId = usuario.uid,
                    empleadoNombre = usuario.nombre,
                    accion = AccionSensible.ABRIR_TURNO.name,
                    responsable = usuario.nombre,
                    sucursal = sucursalId,
                    detalles = "Apertura de turno exitosa con fondo inicial: $$fondoInicial"
                )

                mensajeExito = "Turno abierto"
                exito = true
            } catch (e: Exception) {
                mensajeError = "Error: ${e.message}"
                cargarTurnoContingenciaLocal(sucursal)
            }
            cargando = false
            onResult?.invoke(exito)
        }
    }

    fun abrirTurnoContingenciaLocal(
        fondoInicial: Double,
        sucursal: String,
        usuario: Usuario?,
        onResult: ((Boolean) -> Unit)? = null
    ) {
        if (fondoInicial < 100.0) {
            mensajeError = "El fondo inicial debe ser de al menos $100.00 pesos."
            onResult?.invoke(false)
            return
        }
        if (usuario == null) {
            mensajeError = "Usuario de sesion no valido para contingencia."
            onResult?.invoke(false)
            return
        }
        if (usuario.rol == Rol.VENDEDOR) {
            mensajeError = "La contingencia local requiere encargado o administrador."
            onResult?.invoke(false)
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val sucursalId = normalizarSucursal(sucursal)
            val fecha = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
            val existente = offlineDb.obtenerTurnoContingenciaAbierto(sucursalId)
            val turnoLocal = existente ?: TurnoContingenciaLocal(
                id = "contingencia_${sucursalId}_$fecha",
                sucursal = sucursalId,
                usuarioId = usuario.uid,
                usuarioNombre = usuario.nombre,
                rol = usuario.rol.name,
                fondoInicial = fondoInicial,
                fechaApertura = System.currentTimeMillis()
            ).also { offlineDb.guardarTurnoContingencia(it) }

            withContext(Dispatchers.Main) {
                turnoActivo = turnoLocal.toTurnoCajaV2()
                modoContingenciaLocal = true
                errorTurno = "Operando con turno local de contingencia. Se sincronizara al volver internet."
                mensajeExito = "Turno local de contingencia abierto"
                onResult?.invoke(true)
            }
        }
    }

    fun cerrarTurno(usuarioNombre: String, esAdmin: Boolean, onExito: (String) -> Unit) {
        val turno = turnoActivo ?: return

        if (modoContingenciaLocal || turno.id.startsWith("contingencia_")) {
            cerrarTurnoContingenciaLocal(turno, onExito)
            return
        }

        if (!esAdmin) {
            if (tieneCancelacionesPendientes) {
                mensajeError = "No puedes cerrar: Hay cancelaciones pendientes de revision por el administrador."
                return
            }
            val resultadoCuadre = resultadoCuadreActual
            if (resultadoCuadre == null || !resultadoCuadre.esCorrecto) {
                mensajeError = "La caja no cuadra. Contacta a un administrador para autorizar el cierre."
                return
            }
        }

        viewModelScope.launch {
            cargando = true
            try {
                val efContado = efectivoContado.toDoubleOrNull() ?: 0.0
                val tarContada = tarjetaContada.toDoubleOrNull() ?: 0.0
                val resultadoCuadre = CuadreCajaManager.calcularCuadre(
                    ventasTotal = java.math.BigDecimal.valueOf(totalEfectivoSistema),
                    gastosTotal = java.math.BigDecimal.valueOf(totalGastosSistema),
                    fondoInicial = java.math.BigDecimal.valueOf(turno.fondoInicial),
                    efectivoEnCaja = java.math.BigDecimal.valueOf(efContado),
                    toleranciaEfectivo = java.math.BigDecimal.valueOf(toleranciaEfectivo),
                    toleranciaTarjeta = java.math.BigDecimal.valueOf(toleranciaTarjeta)
                )
                val denomMap = denominacionesInput
                    .filter { (_, v) -> (v.toIntOrNull() ?: 0) > 0 }
                    .mapValues { (_, v) -> v.toIntOrNull() ?: 0 }
                val updates = mapOf(
                    "fechaCierre" to System.currentTimeMillis(),
                    "totalVentasEfectivo" to totalEfectivoSistema,
                    "totalVentasTarjeta" to totalTarjetaSistema,
                    "totalGastosTurno" to totalGastosSistema,
                    "efectivoContado" to efContado,
                    "tarjetaContada" to tarContada,
                    "denominacionesContadas" to denomMap,
                    "cajaId" to turno.cajaId,
                    "diferenciaEfectivo" to resultadoCuadre.diferencia.toDouble(),
                    "diferenciaTarjeta" to resultadoCuadre.diferencia.toDouble(),
                    "estado" to "cerrado"
                )


                // ATOMIC UPDATE
                db.collection(FirestoreCollections.TURNOS_CAJA).document(turno.id).update(updates).await()
                allocationRepo.liberarSucursal(turno.sucursal, usuarioNombre)

                // Forzar que la sucursal ya no queda abierta en su configuración
                db.collection(FirestoreCollections.SUCURSAL_CONFIG).document(normalizarSucursal(turno.sucursal))
                    .update("abierta", false).await()

                // Auditoría de cierre de turno
                if (!cadraCaja) {
                    authManager.registrarAuditoria(
                        empleadoId = "",
                        empleadoNombre = usuarioNombre,
                        accion = AccionSensible.CERRAR_CAJA.name,
                        responsable = usuarioNombre,
                        sucursal = turno.sucursal,
                        detalles = "Cierre FORZADO de caja (discrepancia detectada). Diferencia efectivo: $diferenciaCaja, diferencia tarjeta: $diferenciaTarjeta"
                    )
                } else {
                    authManager.registrarAuditoria(
                        empleadoId = "",
                        empleadoNombre = usuarioNombre,
                        accion = "CERRAR_CAJA",
                        responsable = usuarioNombre,
                        sucursal = turno.sucursal,
                        detalles = "Cierre de caja exitoso. Diferencia efectivo: $diferenciaCaja, diferencia tarjeta: $diferenciaTarjeta"
                    )
                }

                val efectivoEsperado = turno.fondoInicial + totalEfectivoSistema - totalGastosSistema
                val texto = generarTextoCierre(turno, efectivoEsperado)
                mensajeExito = "Turno cerrado"
                efectivoContado = ""
                tarjetaContada = ""
                onExito(texto)
            } catch (e: Exception) {
                mensajeError = "Error: ${e.message}"
            }
            cargando = false
        }
    }

    private fun cerrarTurnoContingenciaLocal(turno: TurnoCajaV2, onExito: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val ventasLocales = offlineDb.obtenerVentasLocalesDesde(turno.sucursal, turno.fechaApertura)
            val efectivo = ventasLocales
                .filterNot { it.esConsumoEmpleado }
                .filter { !it.metodoPago.contains("Tarjeta", ignoreCase = true) || it.metodoPago.contains("Efectivo", ignoreCase = true) }
                .sumOf { it.total }
            val tarjeta = ventasLocales
                .filterNot { it.esConsumoEmpleado }
                .filter { it.metodoPago.contains("Tarjeta", ignoreCase = true) && !it.metodoPago.contains("Efectivo", ignoreCase = true) }
                .sumOf { it.total }

            offlineDb.cerrarTurnoContingencia(
                id = turno.id,
                efectivoContado = efectivoContado.toDoubleOrNull() ?: 0.0,
                tarjetaContada = tarjetaContada.toDoubleOrNull() ?: 0.0
            )
            val esperado = turno.fondoInicial + efectivo
            val texto = generarTextoCierre(turno.copy(totalVentasEfectivo = efectivo, totalVentasTarjeta = tarjeta), esperado)
            withContext(Dispatchers.Main) {
                totalEfectivoSistema = efectivo
                totalTarjetaSistema = tarjeta
                mensajeExito = "Turno local cerrado"
                turnoActivo = null
                modoContingenciaLocal = false
                efectivoContado = ""
                tarjetaContada = ""
                onExito(texto)
            }
        }
    }

    private fun normalizarSucursal(sucursal: String): String =
        sucursal.trim().lowercase(Locale.ROOT).replace(" ", "_")

    private fun TurnoContingenciaLocal.toTurnoCajaV2(): TurnoCajaV2 {
        return TurnoCajaV2(
            id = id,
            sucursal = sucursal,
            usuarioResponsable = usuarioNombre,
            fechaApertura = fechaApertura,
            fechaCierre = fechaCierre,
            fondoInicial = fondoInicial,
            efectivoContado = efectivoContado,
            tarjetaContada = tarjetaContada,
            estado = estado
        )
    }

private fun generarTextoCierre(turno: TurnoCajaV2, esperado: Double): String {
        val localeMX = java.util.Locale.forLanguageTag("es-MX")
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", localeMX)
        val ef = efectivoContado.toDoubleOrNull() ?: 0.0
        val tar = tarjetaContada.toDoubleOrNull() ?: 0.0
        val difEf = ef - esperado
        val difTar = tar - totalTarjetaSistema
        return """
*CIERRE DE TURNO BOCATTA*
${sdf.format(Date())} - ${turno.sucursal}
---------------------
Fondo inicial: $${"%.2f".format(turno.fondoInicial)}
Ventas efectivo: $${"%.2f".format(totalEfectivoSistema)}
Ventas tarjeta:  $${"%.2f".format(totalTarjetaSistema)}
Gastos del turno: $${"%.2f".format(totalGastosSistema)}
---------------------
Efectivo esperado: $${"%.2f".format(esperado)}
Efectivo contado:  $${"%.2f".format(ef)}
${if (difEf >= 0) "OK" else "REVISAR"} Diferencia efectivo: $${"%.2f".format(difEf)}
---------------------
Tarjeta sistema: $${"%.2f".format(totalTarjetaSistema)}
Tarjeta contada: $${"%.2f".format(tar)}
${if (difTar >= 0) "OK" else "REVISAR"} Diferencia tarjeta: $${"%.2f".format(difTar)}
---------------------
_Responsable: ${turno.usuarioResponsable}_
_Bocatta POS_
        """.trimIndent()
    }

    override fun limpiarMensajes() { mensajeExito = null; mensajeError = null }

    // ---- VENTAS FALLIDAS ----

    /** Carga la lista de ventas fallidas para el diálogo de detalle. Solo admin puede actuar. */
    fun abrirDialogoVentasFallidas() {
        viewModelScope.launch(Dispatchers.IO) {
            val fallidas = offlineDb.obtenerVentasFallidas()
            withContext(Dispatchers.Main) {
                ventasFallidas = fallidas
                mostrarDialogoFallidas = true
            }
        }
    }

    fun cerrarDialogoFallidas() {
        mostrarDialogoFallidas = false
    }

    /** Reintentar sincronización de una venta fallida. Solo admin. */
    fun reintentarVentaFallida(ventaId: String, esAdmin: Boolean) {
        if (!esAdmin) {
            mensajeError = "Solo un administrador puede reintentar ventas fallidas."
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            offlineDb.reintentarVenta(ventaId)
            val fallidas = offlineDb.obtenerVentasFallidas()
            val count = offlineDb.contarVentasFallidas()
            withContext(Dispatchers.Main) {
                ventasFallidas = fallidas
                _conteoVentasFallidas.value = count
                mensajeExito = "Venta marcada para reintento de sincronización."
            }
        }
    }

    fun registrarRetiroParcial(
        pin: String,
        monto: Double,
        motivo: String,
        usuarioNombre: String,
        onResult: ((Boolean) -> Unit)? = null
    ) {
        val turno = turnoActivo ?: run {
            mensajeError = "No hay turno activo"
            onResult?.invoke(false)
            return
        }
        if (monto <= 0) {
            mensajeError = "El monto debe ser mayor a 0"
            onResult?.invoke(false)
            return
        }
        if (motivo.isBlank()) {
            mensajeError = "Debe especificar un motivo"
            onResult?.invoke(false)
            return
        }
        viewModelScope.launch {
            cargando = true
            try {
                authManager.validarConPinYAuditar(
                    pin = pin,
                    accion = AccionSensible.GASTAR_CAJA,
                    usuarioResponsable = usuarioNombre,
                    sucursal = turno.sucursal,
                    detalles = "Retiro parcial: $$monto - $motivo"
                ) { pinOk ->
                    if (!pinOk) {
                        mensajeError = "PIN incorrecto o autorización denegada"
                        onResult?.invoke(false)
                        cargando = false
                        return@validarConPinYAuditar
                    }
                    viewModelScope.launch(Dispatchers.IO) {
                        try {
                            val retiroRef = db.collection(FirestoreCollections.TURNOS_CAJA)
                                .document(turno.id)
                                .collection(FirestoreCollections.RETIROS)
                                .document()
                            val retiro = RetiroParcialV2(
                                id = retiroRef.id,
                                turnoId = turno.id,
                                monto = monto,
                                motivo = motivo,
                                usuarioNombre = usuarioNombre,
                                fecha = System.currentTimeMillis()
                            )
                            db.runTransaction { tx ->
                                tx.set(retiroRef, retiro)
                                val turnoRef = db.collection(FirestoreCollections.TURNOS_CAJA).document(turno.id)
                                tx.update(turnoRef, "totalGastosTurno", FieldValue.increment(monto))
                            }.await()
                            totalGastosSistema += monto
                            mensajeExito = "Retiro registrado: $$monto"
                            onResult?.invoke(true)
                        } catch (e: Exception) {
                            mensajeError = "Error al registrar retiro: ${e.message}"
                            onResult?.invoke(false)
                        }
                        cargando = false
                    }
                }
            } catch (e: Exception) {
                mensajeError = "Error: ${e.message}"
                onResult?.invoke(false)
                cargando = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        listenerTurno?.remove()
        listenerVentas?.remove()
        listenerGastos?.remove()
        listenerCancelaciones?.remove()
        listenerParametros?.remove()
        listenerRetiros?.remove()
    }
}
