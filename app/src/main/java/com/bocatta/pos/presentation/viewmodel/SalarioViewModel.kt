package com.bocatta.pos.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.viewModelScope
import com.bocatta.pos.domain.model.ConfiguracionSalarial
import com.bocatta.pos.domain.model.RegistroPago
import com.bocatta.pos.domain.model.Usuario
import com.bocatta.pos.data.repository.ConfiguracionSalarialRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SalarioViewModel(
    private val repository: ConfiguracionSalarialRepository = ConfiguracionSalarialRepository()
) : BaseViewModel() {

    var configuracion by mutableStateOf<ConfiguracionSalarial?>(null)
        private set
    var pagos = mutableStateListOf<RegistroPago>()
        private set

    fun cargarConfiguracion(empleadoId: String) {
        viewModelScope.launch {
            cargando = true
            try {
                configuracion = repository.getConfiguracion(empleadoId)
            } catch (e: Exception) { mensajeError = "Error: ${e.message}" }
            cargando = false
        }
    }

    fun guardarConfiguracion(config: ConfiguracionSalarial) {
        viewModelScope.launch {
            cargando = true
            try {
                if (repository.guardarConfiguracion(config)) {
                    configuracion = config
                    mensajeExito = "Configuracion guardada"
                }
            } catch (e: Exception) { mensajeError = "Error: ${e.message}" }
            cargando = false
        }
    }

    fun cargarPagos(empleadoId: String) {
        viewModelScope.launch {
            cargando = true
            try {
                pagos.clear()
                pagos.addAll(repository.getPagos(empleadoId))
            } catch (e: Exception) { mensajeError = "Error: ${e.message}" }
            cargando = false
        }
    }

    fun cargarPagosPeriodo(inicio: Long, fin: Long) {
        viewModelScope.launch {
            cargando = true
            try {
                pagos.clear()
                pagos.addAll(repository.getPagosDelPeriodo(inicio, fin))
            } catch (e: Exception) { mensajeError = "Error: ${e.message}" }
            cargando = false
        }
    }

    fun registrarPago(pago: RegistroPago) {
        viewModelScope.launch {
            cargando = true
            try {
                if (repository.registrarPago(pago)) {
                    cargarPagos(pago.empleadoId)
                    mensajeExito = "Pago registrado"
                }
            } catch (e: Exception) { mensajeError = "Error: ${e.message}" }
            cargando = false
        }
    }

    fun registrarPagoPeriodo(
        empleadoId: String,
        empleadoNombre: String,
        inicio: Long,
        fin: Long
    ) {
        viewModelScope.launch {
            cargando = true
            try {
                val config = repository.getConfiguracion(empleadoId)
                if (config == null || config.salarioBase <= 0.0) {
                    mensajeError = "Configura el salario antes de registrar el pago"
                    cargando = false
                    return@launch
                }

                val deducciones = config.deduccionIsr + config.deduccionImss + config.deduccionPrestamo
                val pago = RegistroPago(
                    empleadoId = empleadoId,
                    empleadoNombre = empleadoNombre,
                    periodoInicio = inicio,
                    periodoFin = fin,
                    salarioBruto = config.salarioBase,
                    deducciones = deducciones,
                    salarioNeto = (config.salarioBase - deducciones).coerceAtLeast(0.0),
                    formaPago = config.formaPago,
                    pagado = false
                )

                if (repository.registrarPago(pago)) {
                    pagos.clear()
                    pagos.addAll(repository.getPagosDelPeriodo(inicio, fin))
                    mensajeExito = "Pago registrado"
                }
            } catch (e: Exception) { mensajeError = "Error: ${e.message}" }
            cargando = false
        }
    }

    fun marcarPagado(id: String, fecha: Long, empleadoId: String) {
        viewModelScope.launch {
            try {
                if (repository.marcarPagado(id, fecha)) {
                    cargarPagos(empleadoId)
                    mensajeExito = "Pago marcado como pagado"
                }
            } catch (e: Exception) { mensajeError = "Error: ${e.message}" }
        }
    }

    fun marcarPagadoPeriodo(id: String, fecha: Long, inicio: Long, fin: Long) {
        viewModelScope.launch {
            try {
                if (repository.marcarPagado(id, fecha)) {
                    pagos.clear()
                    pagos.addAll(repository.getPagosDelPeriodo(inicio, fin))
                    mensajeExito = "Pago marcado como pagado"
                }
            } catch (e: Exception) { mensajeError = "Error: ${e.message}" }
        }
    }

    fun calcularPagoPeriodo(empleadoId: String, inicio: Long, fin: Long) {
        viewModelScope.launch {
            cargando = true
            try {
                val config = repository.getConfiguracion(empleadoId)
                val pagosPeriodo = repository.getPagosDelPeriodo(inicio, fin)
                pagos.clear()
                pagos.addAll(pagosPeriodo)
            } catch (e: Exception) { mensajeError = "Error: ${e.message}" }
            cargando = false
        }
    }

    fun generarTextoNominaWhatsApp(
        sucursal: String,
        usuarios: List<Usuario>,
        inicio: Long,
        fin: Long
    ): String {
        val localeMX = Locale.forLanguageTag("es-MX")
        val sdf = SimpleDateFormat("dd/MM/yyyy", localeMX)
        val pagosPeriodo = pagos.toList()
        val ultimoPagoPorEmpleado = usuarios.associateWith { user ->
            pagosPeriodo
                .filter { it.empleadoId == user.uid }
                .maxByOrNull { it.periodoFin.takeIf { finPago -> finPago > 0L } ?: it.periodoInicio }
        }

        val pagosRegistrados = ultimoPagoPorEmpleado.values.filterNotNull()
        val pagados = pagosRegistrados.filter { it.pagado }
        val pendientes = pagosRegistrados.filter { !it.pagado }
        val sinPago = usuarios.size - pagosRegistrados.size
        val totalBruto = pagosRegistrados.sumOf { it.salarioBruto }
        val totalDeducciones = pagosRegistrados.sumOf { it.deducciones }
        val totalNeto = pagosRegistrados.sumOf { it.salarioNeto }
        val totalPagado = pagados.sumOf { it.salarioNeto }
        val totalPendiente = pendientes.sumOf { it.salarioNeto }

        return buildString {
            append("💼 *BOCATTA - REPORTE DE NÓMINA* 💼\n")
            append("📅 Periodo: ${sdf.format(Date(inicio))} - ${sdf.format(Date(fin))}\n")
            append("🏪 Sucursal: ${sucursal.uppercase()}\n")
            append("────────────────────\n")
            append("👥 Empleados: ${usuarios.size}\n")
            append("✅ Pagados: ${pagados.size}\n")
            append("⏳ Pendientes: ${pendientes.size}\n")
            append("⚪ Sin pago registrado: $sinPago\n")
            append("────────────────────\n")
            append("💵 Bruto: $${"%.2f".format(totalBruto)}\n")
            append("➖ Deducciones: $${"%.2f".format(totalDeducciones)}\n")
            append("✨ Neto nómina: $${"%.2f".format(totalNeto)}\n")
            append("✅ Pagado: $${"%.2f".format(totalPagado)}\n")
            append("⏳ Por pagar: $${"%.2f".format(totalPendiente)}\n")
            append("────────────────────\n")
            append("*Detalle por empleado*\n")

            usuarios.forEach { user ->
                val pago = ultimoPagoPorEmpleado[user]
                append("\n• ${user.nombre.ifBlank { "Empleado" }} (${user.rol.name})\n")
                if (pago == null) {
                    append("  Estado: Sin pago registrado\n")
                } else {
                    append("  Estado: ${if (pago.pagado) "Pagado" else "Pendiente"}\n")
                    append("  Bruto: $${"%.2f".format(pago.salarioBruto)} | Deducciones: $${"%.2f".format(pago.deducciones)}\n")
                    append("  Neto: *$${"%.2f".format(pago.salarioNeto)}* | ${pago.formaPago.name}\n")
                    pago.fechaPago?.let { append("  Fecha pago: ${sdf.format(Date(it))}\n") }
                    if (pago.notas.isNotBlank()) append("  Nota: ${pago.notas}\n")
                }
            }

            append("\n────────────────────\n")
            append("_Sistema Industrial Bocatta V2_")
        }
    }
}

