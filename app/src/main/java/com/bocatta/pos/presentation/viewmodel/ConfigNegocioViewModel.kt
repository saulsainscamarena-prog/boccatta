package com.bocatta.pos.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.bocatta.pos.data.repository.ConfiguracionRepository
import com.bocatta.pos.presentation.ui.screens.admin.ConfigItem
import kotlinx.coroutines.launch
import timber.log.Timber

class ConfigNegocioViewModel(private val repository: ConfiguracionRepository = ConfiguracionRepository()) : BaseViewModel() {

    var insumosApertura by mutableStateOf<List<ConfigItem>>(emptyList())
        private set

    var mapeoAderezos by mutableStateOf<List<ConfigItem>>(emptyList())
        private set

    var mapeoToppings by mutableStateOf<List<ConfigItem>>(emptyList())
        private set

    var nombreNegocio by mutableStateOf("")
    var giroNegocio by mutableStateOf("")
    var fondoMinimo by mutableStateOf("500")
    var cicloMembresia by mutableStateOf("6")
    var porcentajeDescuento by mutableStateOf("10")
    var pagoEfectivo by mutableStateOf(true)
    var pagoTarjeta by mutableStateOf(true)
    var pagoTransferencia by mutableStateOf(false)
    var pagoRappi by mutableStateOf(false)
    var pagoUber by mutableStateOf(false)
    var pagoDidi by mutableStateOf(false)
    var toleranciaEfectivo by mutableStateOf("10.0")
    var toleranciaTarjeta by mutableStateOf("5.0")
    var isSaving by mutableStateOf(false)

    init {
        cargarDatos()
    }

    fun cargarDatos() {
        viewModelScope.launch {
            cargando = true
            try {
                insumosApertura = repository.getInsumosApertura()
                mapeoAderezos = repository.getMapeoAderezos()
                mapeoToppings = repository.getMapeoToppings()
                
                val parametros = repository.getParametrosCaja()
                toleranciaEfectivo = parametros["tolerancia_efectivo"].toString()
                toleranciaTarjeta = parametros["tolerancia_tarjeta"].toString()
            } catch (e: Exception) {
                mensajeError = "Error: ${e.message}"
                Timber.e(e, "Error cargando datos de configuracion")
            }
            cargando = false
        }
    }

    fun guardarItem(
        section: Int,
        id: String,
        nombre: String,
        unidad: String,
        insumoId: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            isSaving = true
            try {
                when (section) {
                    0 -> repository.guardarInsumoApertura(id, nombre, unidad)
                    1 -> repository.guardarMapeo("mapeo_aderezos", nombre, insumoId)
                    2 -> repository.guardarMapeo("mapeo_toppings", nombre, insumoId)
                }
                cargarDatos()
                onSuccess()
            } catch (e: Exception) {
                mensajeError = "Error: ${e.message}"
                Timber.e(e, "Error guardando item de configuracion")
            } finally {
                isSaving = false
            }
        }
    }

    fun guardarConfiguracion() {
        viewModelScope.launch {
            isSaving = true
            try {
                val params = mapOf(
                    "nombreNegocio" to nombreNegocio,
                    "giroNegocio" to giroNegocio,
                    "fondoMinimo" to (fondoMinimo.toDoubleOrNull() ?: 500.0),
                    "cicloMembresia" to (cicloMembresia.toIntOrNull() ?: 6),
                    "porcentajeDescuento" to (porcentajeDescuento.toDoubleOrNull() ?: 10.0)
                )
                repository.guardarParametrosCaja(params)
                mensajeExito = "Configuracion guardada"
            } catch (e: Exception) {
                mensajeError = "Error: ${e.message}"
            } finally {
                isSaving = false
            }
        }
    }

    fun eliminarItem(section: Int, item: ConfigItem) {
        viewModelScope.launch {
            isSaving = true
            try {
                when (section) {
                    0 -> repository.eliminarInsumoApertura(item.id)
                    1 -> repository.eliminarMapeo("mapeo_aderezos", item.nombre)
                    2 -> repository.eliminarMapeo("mapeo_toppings", item.nombre)
                }
                cargarDatos()
            } catch (e: Exception) {
                mensajeError = "Error: ${e.message}"
                Timber.e(e, "Error eliminando item de configuracion")
            } finally {
                isSaving = false
            }
        }
    }

    fun guardarParametrosCaja(onSuccess: () -> Unit) {
        viewModelScope.launch {
            isSaving = true
            try {
                val tEfe = toleranciaEfectivo.toDoubleOrNull() ?: 10.0
                val tTar = toleranciaTarjeta.toDoubleOrNull() ?: 5.0
                repository.guardarParametrosCaja(tEfe, tTar)
                onSuccess()
            } catch (e: Exception) {
                mensajeError = "Error: ${e.message}"
                Timber.e(e, "Error guardando parametros de caja")
            } finally {
                isSaving = false
            }
        }
    }


}

