package com.bocatta.pos.feature.admin.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bocatta.pos.core.ui.viewmodel.BaseViewModel
import com.bocatta.pos.domain.model.*
import com.bocatta.pos.network.firebase.FirebaseFirestoreProvider
import com.bocatta.pos.core.constants.FirestoreCollections
import com.google.firebase.firestore.ListenerRegistration
import com.bocatta.pos.data.repository.ReportRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ReportViewModelV2(private val repository: ReportRepository = ReportRepository()) : BaseViewModel() {
    private val db = FirebaseFirestoreProvider.db

    private var salesListener: ListenerRegistration? = null
    private var sales7DaysListener: ListenerRegistration? = null
    private var expenseListener: ListenerRegistration? = null
    private var mermaListener: ListenerRegistration? = null
    private var stockAlertListener: ListenerRegistration? = null

    var ventasBrutas by mutableDoubleStateOf(0.0)
        private set
    var totalGastos by mutableDoubleStateOf(0.0)
        private set
    var totalMermas by mutableDoubleStateOf(0.0)
        private set
    var costoProduccionTeorico by mutableDoubleStateOf(0.0)
        private set
    var utilidadNeta by mutableDoubleStateOf(0.0)
        private set
    var ventasEfectivo by mutableDoubleStateOf(0.0)
        private set
    var ventasTarjeta by mutableDoubleStateOf(0.0)
        private set
    var ticketPromedio by mutableDoubleStateOf(0.0)
        private set
    var mejorVendedor by mutableStateOf("N/A")
        private set
    
    var ventasDelDia = mutableStateListOf<VentaV2>()
        private set
    var productoMasVendido = mutableStateOf("N/A")
        private set
    var ventasPorDia = mutableStateListOf<VentaPorDia>()
        private set
    var topProductos = mutableStateListOf<ProductoMetrica>()
        private set
    var insumosPorResurtir = mutableStateMapOf<String, Double>()
        private set

    fun escucharReporteHoy(sucursal: String) {
        salesListener?.remove()
        sales7DaysListener?.remove()
        expenseListener?.remove()
        mermaListener?.remove()
        stockAlertListener?.remove()
        
        val sucursalId = sucursal.lowercase(java.util.Locale.getDefault()).trim()
        val hoy = java.util.Calendar.getInstance().apply { 
            set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0); set(java.util.Calendar.SECOND, 0) 
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis

        // 1. ESCUCHAR VENTAS
        salesListener = db.collection(FirestoreCollections.VENTAS)
            .whereEqualTo("sucursal", sucursalId)
            .whereGreaterThanOrEqualTo("fecha", hoy)
            .addSnapshotListener { snap, _ ->
                val docs = snap?.documents.orEmpty()
                viewModelScope.launch(Dispatchers.Default) {
                    var bruto = 0.0
                    var efec = 0.0
                    var tarj = 0.0
                    val vendedores = mutableMapOf<String, Double>()
                    val ventas = mutableListOf<VentaV2>()
                    val conteoProductos = mutableMapOf<String, Int>()
                    val ingresosProductos = mutableMapOf<String, Double>()

                    docs.forEach { doc ->
                        val v = doc.toObject(VentaV2::class.java)
                        if (v != null) {
                            ventas.add(v)
                            bruto += v.total
                            vendedores[v.atendio] = (vendedores[v.atendio] ?: 0.0) + v.total
                            if (v.metodoPago == "Tarjeta") tarj += v.total else efec += v.total
                            
                            // MÑtricas de productos (Casting seguro para V2)
                            v.productos.forEach { item ->
                                val nombre = item.nombre.ifBlank { "Desconocido" }
                                val cant = item.cantidad
                                val precioUnitario = item.precioUnitario
                                
                                conteoProductos[nombre] = (conteoProductos[nombre] ?: 0) + cant
                                ingresosProductos[nombre] = (ingresosProductos[nombre] ?: 0.0) + (precioUnitario * cant)
                            }
                        }
                    }
                    val top = conteoProductos.map { (nombre, cant) ->
                        ProductoMetrica(nombre, cant, ingresosProductos[nombre] ?: 0.0)
                    }.sortedByDescending { it.cantidad }.take(5)

                    withContext(Dispatchers.Main) {
                        ventasDelDia.clear()
                        ventasDelDia.addAll(ventas)
                        ventasBrutas = bruto
                        ventasEfectivo = efec
                        ventasTarjeta = tarj
                        mejorVendedor = vendedores.maxByOrNull { it.value }?.key ?: "N/A"
                        ticketPromedio = if (ventas.isNotEmpty()) bruto / ventas.size else 0.0
                        productoMasVendido.value = conteoProductos.maxByOrNull { it.value }?.key ?: "N/A"
                        topProductos.clear()
                        topProductos.addAll(top)
                        recalcularUtilidad()
                    }
                }
            }

        // 1.1 ESCUCHAR VENTAS 7 D͍AS (Para la grÑfica)
        val hace7Dias = java.util.Calendar.getInstance().apply {
            add(java.util.Calendar.DAY_OF_YEAR, -6)
            set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0); set(java.util.Calendar.SECOND, 0)
        }.timeInMillis

        sales7DaysListener = db.collection(FirestoreCollections.VENTAS)
            .whereEqualTo("sucursal", sucursalId)
            .whereGreaterThanOrEqualTo("fecha", hace7Dias)
            .addSnapshotListener { snap, _ ->
                val docs = snap?.documents.orEmpty()
                viewModelScope.launch(Dispatchers.Default) {
                    val mapa = mutableMapOf<String, Double>()
                    val sdf = java.text.SimpleDateFormat("dd/MM", java.util.Locale.getDefault())
                    
                    for (i in 0..6) {
                        val cal = java.util.Calendar.getInstance()
                        cal.add(java.util.Calendar.DAY_OF_YEAR, -(6 - i))
                        mapa[sdf.format(cal.time)] = 0.0
                    }

                    docs.forEach { doc ->
                        val t = doc.getDouble("total") ?: 0.0
                        val f = doc.getLong("fecha") ?: 0L
                        val etiq = sdf.format(java.util.Date(f))
                        if (mapa.containsKey(etiq)) {
                            mapa[etiq] = (mapa[etiq] ?: 0.0) + t
                        }
                    }

                    val serie = mapa.map { (k, v) -> VentaPorDia(k, v) }
                    withContext(Dispatchers.Main) {
                        ventasPorDia.clear()
                        ventasPorDia.addAll(serie)
                    }
                }
            }

        // 2. ESCUCHAR GASTOS
        expenseListener = db.collection(FirestoreCollections.GASTOS)
            .whereEqualTo("sucursal", sucursalId)
            .whereGreaterThanOrEqualTo("fecha", hoy)
            .addSnapshotListener { snap, _ ->
                val docs = snap?.documents.orEmpty()
                viewModelScope.launch(Dispatchers.Default) {
                    val total = docs.sumOf { it.getDouble("monto") ?: 0.0 }
                    withContext(Dispatchers.Main) {
                        totalGastos = total
                        recalcularUtilidad()
                    }
                }
            }

        // 3. ESCUCHAR MERMAS (PÑrdidas)
        mermaListener = db.collection(FirestoreCollections.MERMA_LOGS)
            .whereEqualTo("sucursal", sucursalId)
            .whereGreaterThanOrEqualTo("fecha", hoy)
            .addSnapshotListener { snap, _ ->
                val docs = snap?.documents.orEmpty()
                viewModelScope.launch(Dispatchers.Default) {
                    val total = docs.sumOf { it.getDouble("costo") ?: 0.0 }
                    withContext(Dispatchers.Main) {
                        totalMermas = total
                        recalcularUtilidad()
                    }
                }
            }

        // 4. ESCUCHAR ALERTAS DE STOCK (Insumos por resurtir)
        stockAlertListener = db.collection(FirestoreCollections.INVENTARIO_SUCURSAL)
            .whereEqualTo("sucursal", sucursalId)
            .addSnapshotListener { snap, _ ->
                if (snap != null) {
                    insumosPorResurtir.clear()
                    snap.documents.forEach { doc ->
                        val stock = doc.getDouble("cantidadEnBase") ?: doc.getDouble("cantidadDisponible") ?: 0.0
                        if (stock < 10.0) { // Umbral crÑtico genÑrico V2
                            val nombre = (doc.getString("insumoId") ?: doc.id.removePrefix("${sucursalId}_")).replace("_", " ").uppercase(java.util.Locale.getDefault())
                            insumosPorResurtir[nombre] = stock
                        }
                    }
                }
            }
    }

    private fun recalcularUtilidad() {
        utilidadNeta = ventasBrutas - totalGastos - costoProduccionTeorico
    }

    override fun onCleared() {
        super.onCleared()
        salesListener?.remove()
        sales7DaysListener?.remove()
        expenseListener?.remove()
        mermaListener?.remove()
        stockAlertListener?.remove()
    }

    fun generarTextoCierreWhatsApp(sucursal: String): String {
        val localeMX = java.util.Locale.forLanguageTag("es-MX")
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", localeMX)
        val sb = StringBuilder()
        sb.append("\uD83D\uDCC8 *BOCATTA - REPORTE DE CIERRE V2* \uD83D\uDCC8\n")
        sb.append("\uD83D\uDCC5 Fecha: ${sdf.format(java.util.Date())}\n")
        sb.append("\uD83D\uDD0D Sucursal: ${sucursal.uppercase(java.util.Locale.getDefault())}\n")
        sb.append("\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\n")
        sb.append("\uD83D\uDCC5 Ventas Brutas: $${"%.2f".format(ventasBrutas)}\n")
        sb.append("\uD83D\uDCCB Gastos Hoy: $${"%.2f".format(totalGastos)}\n")
        sb.append("\uD83E\uDD3D Costo Insumos (REAL): $${"%.2f".format(costoProduccionTeorico)}\n")
        sb.append("\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\n")
        sb.append("\u2728 *UTILIDAD NETA: $${"%.2f".format(utilidadNeta)}*\n")
        sb.append("\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\n")
        sb.append("\uD83D\uDCC3 Tarjeta: $${"%.2f".format(ventasTarjeta)}\n")
        sb.append("\uD83D\uDCC4 Efectivo: $${"%.2f".format(ventasEfectivo)}\n")
        sb.append("\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\n")
        sb.append("\uD83D\uDCC9 Ticket Promedio: $${"%.2f".format(ticketPromedio)}\n")
        sb.append("\uD83C\uDFC6 Vendedor Estrella: $mejorVendedor\n")
        sb.append("\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\n")
        sb.append("_Sistema Industrial Bocatta V2_")
        return sb.toString()
    }

    fun generarReporteDiario(sucursal: String, fecha: Long) {
        viewModelScope.launch(safeHandler) {
            cargando = true
            try {
                val resumen = repository.obtenerResumenDiario(sucursal, fecha)
                
                totalGastos = resumen.totalGastos
                ventasBrutas = resumen.ventasBrutas
                ventasEfectivo = resumen.ventasEfectivo
                ventasTarjeta = resumen.ventasTarjeta
                costoProduccionTeorico = resumen.costoProduccionReal
                utilidadNeta = ventasBrutas - totalGastos - costoProduccionTeorico

            } catch (e: Exception) {
                mensajeError = "Error: ${e.message}"
                Timber.tag("ReportVM").e(e, "Error en reporte")
            } finally {
                cargando = false
            }
        }
    }
}





