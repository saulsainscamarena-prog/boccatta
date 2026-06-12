package com.bocatta.pos.feature.ventas.viewmodel

import com.bocatta.pos.core.ui.viewmodel.BaseViewModel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.viewModelScope
import com.bocatta.pos.domain.model.Mesa
import com.bocatta.pos.domain.model.TransferenciaMesa
import com.bocatta.pos.domain.model.Zona
import com.bocatta.pos.data.repository.MesaRepository
import com.bocatta.pos.data.repository.ZonaRepository
import com.bocatta.pos.network.firebase.FirebaseFirestoreProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MesaViewModel(
    private val zonaRepo: ZonaRepository = ZonaRepository(),
    private val mesaRepo: MesaRepository = MesaRepository()
) : BaseViewModel() {

    var zonas = mutableStateListOf<Zona>()
        private set
    var mesas = mutableStateListOf<Mesa>()
        private set

    init {
        load()
    }

    private fun load() {
        launchIO {
            cargarZonas()
            cargarMesas()
        }
    }

    fun cargarZonas() {
        viewModelScope.launch {
            cargando = true
            try {
                zonas.clear()
                zonas.addAll(zonaRepo.getAll())
            } catch (e: Exception) { mensajeError = "Error: ${e.message}" }
            cargando = false
        }
    }

    fun guardarZona(zona: Zona) {
        viewModelScope.launch {
            cargando = true
            try {
                if (zonaRepo.guardar(zona)) {
                    cargarZonas()
                    mensajeExito = "Zona guardada"
                }
            } catch (e: Exception) { mensajeError = "Error: ${e.message}" }
            cargando = false
        }
    }

    fun eliminarZona(id: String) {
        viewModelScope.launch {
            cargando = true
            try {
                if (zonaRepo.eliminar(id)) {
                    cargarZonas()
                    mensajeExito = "Zona eliminada"
                }
            } catch (e: Exception) { mensajeError = "Error: ${e.message}" }
            cargando = false
        }
    }

    fun cargarMesas() {
        viewModelScope.launch {
            cargando = true
            try {
                mesas.clear()
                mesas.addAll(mesaRepo.getAll())
            } catch (e: Exception) { mensajeError = "Error: ${e.message}" }
            cargando = false
        }
    }

    fun guardarMesa(mesa: Mesa) {
        viewModelScope.launch {
            cargando = true
            try {
                if (mesaRepo.guardar(mesa)) {
                    cargarMesas()
                    mensajeExito = "Mesa guardada"
                }
            } catch (e: Exception) { mensajeError = "Error: ${e.message}" }
            cargando = false
        }
    }

    fun eliminarMesa(id: String) {
        viewModelScope.launch {
            cargando = true
            try {
                if (mesaRepo.eliminar(id)) {
                    cargarMesas()
                    mensajeExito = "Mesa eliminada"
                }
            } catch (e: Exception) { mensajeError = "Error: ${e.message}" }
            cargando = false
        }
    }

    fun actualizarEstadoMesa(id: String, estado: String) {
        viewModelScope.launch {
            try {
                if (mesaRepo.actualizarEstado(id, estado)) {
                    cargarMesas()
                }
            } catch (e: Exception) { mensajeError = "Error: ${e.message}" }
        }
    }

    fun vincularOrdenAMesa(id: String, estado: String, ordenId: String?) {
        viewModelScope.launch {
            try {
                if (mesaRepo.vincularOrden(id, estado, ordenId)) {
                    cargarMesas()
                }
            } catch (e: Exception) { mensajeError = "Error al vincular orden: ${e.message}" }
        }
    }


    fun transferirMesa(origenZonaId: String, destinoZonaId: String, mesaId: String, empleadoOrigen: String, empleadoDestino: String, motivo: String) {
        viewModelScope.launch {
            cargando = true
            try {
                val transferencia = TransferenciaMesa(
                    mesaId = mesaId,
                    zonaOrigenId = origenZonaId,
                    zonaDestinoId = destinoZonaId,
                    empleadoOrigenId = empleadoOrigen,
                    empleadoDestinoId = empleadoDestino,
                    motivo = motivo
                )
                val db = com.bocatta.pos.network.firebase.FirebaseFirestoreProvider.db
                db.collection("v2_transferencias_mesa").document().set(transferencia).await()
                mesaRepo.actualizarEstado(mesaId, "LIBRE")
                cargarMesas()
                mensajeExito = "Mesa transferida"
            } catch (e: Exception) { mensajeError = "Error: ${e.message}" }
            cargando = false
        }
    }
}



