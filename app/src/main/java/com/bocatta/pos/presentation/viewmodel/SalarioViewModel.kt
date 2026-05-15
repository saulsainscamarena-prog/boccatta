package com.bocatta.pos.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.viewModelScope
import com.bocatta.pos.domain.model.ConfiguracionSalarial
import com.bocatta.pos.domain.model.RegistroPago
import com.bocatta.pos.data.repository.ConfiguracionSalarialRepository
import kotlinx.coroutines.launch

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
}
