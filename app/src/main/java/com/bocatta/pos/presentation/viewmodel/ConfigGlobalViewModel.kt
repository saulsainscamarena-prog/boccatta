package com.bocatta.pos.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bocatta.pos.data.repository.ConfigGlobalRepository
import com.bocatta.pos.domain.model.GrupoConfiguracionGlobal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ConfigGlobalViewModel : ViewModel() {

    private val repository = ConfigGlobalRepository()

    private val _grupos = MutableStateFlow<List<GrupoConfiguracionGlobal>>(emptyList())
    val grupos: StateFlow<List<GrupoConfiguracionGlobal>> = _grupos.asStateFlow()

    private val _mensajeFeedback = MutableStateFlow<String?>(null)
    val mensajeFeedback: StateFlow<String?> = _mensajeFeedback.asStateFlow()

    init { cargar() }

    fun cargar() {
        viewModelScope.launch {
            _grupos.value = repository.getAll()
        }
    }

    fun guardar(grupo: GrupoConfiguracionGlobal) {
        viewModelScope.launch {
            val operacion = if (grupo.id.isBlank()) "Creado" else "Actualizado"
            val ok = repository.guardar(grupo)
            if (ok) {
                cargar()
                _mensajeFeedback.value = "$operacion correctamente"
            } else {
                _mensajeFeedback.value = "Error al guardar"
            }
        }
    }

    fun eliminar(id: String) {
        viewModelScope.launch {
            val ok = repository.eliminar(id)
            if (ok) {
                cargar()
                _mensajeFeedback.value = "Eliminado correctamente"
            } else {
                _mensajeFeedback.value = "Error al eliminar"
            }
        }
    }

    fun limpiarFeedback() { _mensajeFeedback.value = null }
}
