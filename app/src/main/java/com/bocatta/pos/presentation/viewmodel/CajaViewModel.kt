package com.bocatta.pos.presentation.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.viewModelScope
import com.bocatta.pos.data.repository.StockAllocationRepository
import com.bocatta.pos.domain.model.TurnoCajaV2
import com.bocatta.pos.network.firebase.FirebaseFirestoreProvider
import com.bocatta.pos.core.constants.FirestoreCollections
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class CajaViewModel : BaseViewModel() {
    private val db = FirebaseFirestoreProvider.db
    private val allocationRepo = StockAllocationRepository()

    private var listenerTurno: ListenerRegistration? = null
    private var listenerVentas: ListenerRegistration? = null
    private var listenerGastos: ListenerRegistration? = null
    private var listenerCancelaciones: ListenerRegistration? = null
    private var listenerParametros: ListenerRegistration? = null

    var turnoActivo by mutableStateOf<TurnoCajaV2?>(null)
        private set
    var cargandoTurno by mutableStateOf(false)
        private set
    var errorTurno by mutableStateOf<String?>(null)
        private set

    // CANDADO DE SEGURIDAD
    var tieneCancelacionesPendientes by mutableStateOf(false)
        private set
    var numCancelacionesPendientes by mutableIntStateOf(0)
        private set

    fun verificarPendientes() {
        listenerCancelaciones?.remove()
        listenerCancelaciones = db.collection(FirestoreCollections.CANCELACIONES)
            .whereEqualTo("estado", "pendiente_revision")
            .addSnapshotListener { snap, _ ->
                numCancelacionesPendientes = snap?.size() ?: 0
                tieneCancelacionesPendientes = numCancelacionesPendientes > 0
            }
    }

    // Totales del día calculados en tiempo real
    var totalEfectivoSistema by mutableStateOf(0.0)
        private set
    var totalTarjetaSistema by mutableStateOf(0.0)
        private set
    var totalGastosSistema by mutableStateOf(0.0)
        private set
    
    var sucursalFiltro by mutableStateOf<String?>(null)

    // Parámetros de configuración remotos
    var toleranciaEfectivo by mutableStateOf(10.0)
    var toleranciaTarjeta by mutableStateOf(5.0)

    // Inputs del empleado al contar
    var efectivoContado by mutableStateOf("")
    var tarjetaContada by mutableStateOf("")

    val diferenciaCaja: Double
        get() {
            val turno = turnoActivo ?: return 0.0
            val efectivoEsperado = turno.fondoInicial + totalEfectivoSistema - totalGastosSistema
            return (efectivoContado.toDoubleOrNull() ?: 0.0) - efectivoEsperado
        }

    val diferenciaTarjeta: Double
        get() = (tarjetaContada.toDoubleOrNull() ?: 0.0) - totalTarjetaSistema

    val cadraCaja: Boolean
        get() = kotlin.math.abs(diferenciaCaja) <= toleranciaEfectivo && kotlin.math.abs(diferenciaTarjeta) <= toleranciaTarjeta

    init {
        escucharTurnoActivo()
        escucharTotalesDia()
        verificarPendientes()
        escucharParametrosCaja()
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
                    cargandoTurno = false
                    return@addSnapshotListener
                }
                if (doc != null && doc.exists() && doc.getString("estado") == "abierto") {
                    turnoActivo = doc.toObject(TurnoCajaV2::class.java)?.copy(id = doc.id)
                    cargandoTurno = false
                } else {
                    viewModelScope.launch {
                        turnoActivo = buscarTurnoAbiertoLegacy(sucursal)
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
            null
        }
    }

    private fun escucharTotalesDia() {
        val sucursal = sucursalFiltro ?: return
        val inicioDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
        }.timeInMillis

        listenerVentas?.remove()
        listenerVentas = db.collection(FirestoreCollections.VENTAS)
            .whereEqualTo("sucursal", sucursal.lowercase())
            .whereGreaterThanOrEqualTo("fecha", inicioDay)
            .addSnapshotListener { snap, _ ->
                viewModelScope.launch(Dispatchers.Default) {
                    var efectivo = 0.0
                    var tarjeta = 0.0
                    snap?.documents?.forEach { doc ->
                        if (doc.getString("estado") == "devuelta") return@forEach
                        val total = doc.getDouble("total") ?: 0.0
                        if (doc.getString("metodoPago") == "Tarjeta") tarjeta += total else efectivo += total
                    }
                    withContext(Dispatchers.Main) {
                        totalEfectivoSistema = efectivo
                        totalTarjetaSistema = tarjeta
                    }
                }
            }

        listenerGastos?.remove()
        listenerGastos = db.collection(FirestoreCollections.GASTOS)
            .whereEqualTo("sucursal", sucursal.lowercase())
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

    fun abrirTurno(
        fondoInicial: Double,
        sucursal: String,
        usuarioNombre: String,
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
                val sucursalId = normalizarSucursal(sucursal)
                val id = "${sucursalId}_${SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())}"
                val turnoRef = db.collection(FirestoreCollections.TURNOS_CAJA).document(id)
                val configRef = db.collection(FirestoreCollections.SUCURSAL_CONFIG).document(sucursalId)
                val turno = TurnoCajaV2(
                    id = id,
                    sucursal = sucursalId,
                    fechaApertura = System.currentTimeMillis(),
                    fondoInicial = fondoInicial,
                    usuarioResponsable = usuarioNombre,
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
                allocationRepo.redistribuirSucursalesAbiertas(usuarioNombre)
                mensajeExito = "Turno abierto"
                exito = true
            } catch (e: Exception) {
                mensajeError = "Error: ${e.message}"
            }
            cargando = false
            onResult?.invoke(exito)
        }
    }

    fun cerrarTurno(usuarioNombre: String, esAdmin: Boolean, onExito: (String) -> Unit) {
        val turno = turnoActivo ?: return

        if (!esAdmin) {
            if (tieneCancelacionesPendientes) {
                mensajeError = "No puedes cerrar: Hay cancelaciones pendientes de revision por el administrador."
                return
            }
            if (!cadraCaja) {
                mensajeError = "La caja no cuadra. Contacta a un administrador para autorizar el cierre."
                return
            }
        }

        viewModelScope.launch {
            cargando = true
            try {
                val efectivoEsperado = turno.fondoInicial + totalEfectivoSistema - totalGastosSistema
                val updates = mapOf(
                    "fechaCierre" to System.currentTimeMillis(),
                    "totalVentasEfectivo" to totalEfectivoSistema,
                    "totalVentasTarjeta" to totalTarjetaSistema,
                    "totalGastosTurno" to totalGastosSistema,
                    "efectivoContado" to (efectivoContado.toDoubleOrNull() ?: 0.0),
                    "tarjetaContada" to (tarjetaContada.toDoubleOrNull() ?: 0.0),
                    "diferenciaEfectivo" to diferenciaCaja,
                    "diferenciaTarjeta" to diferenciaTarjeta,
                    "estado" to "cerrado"
                )
                
                // ATOMIC UPDATE
                db.collection(FirestoreCollections.TURNOS_CAJA).document(turno.id).update(updates).await()
                allocationRepo.liberarSucursal(turno.sucursal, usuarioNombre)
                
                // Forzar que la sucursal ya no queda abierta en su configuración
                db.collection(FirestoreCollections.SUCURSAL_CONFIG).document(normalizarSucursal(turno.sucursal))
                    .update("abierta", false).await()

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

    private fun normalizarSucursal(sucursal: String): String =
        sucursal.trim().lowercase(Locale.ROOT).replace(" ", "_")

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

    override fun onCleared() {
        super.onCleared()
        listenerTurno?.remove()
        listenerVentas?.remove()
        listenerGastos?.remove()
        listenerCancelaciones?.remove()
        listenerParametros?.remove()
    }
}



