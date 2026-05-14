package com.bocatta.pos.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bocatta.pos.data.repository.CatalogoRepository
import com.bocatta.pos.domain.model.OpcionCatalogo
import com.bocatta.pos.domain.model.TipoCatalogo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CatalogoViewModel : ViewModel() {

    private val repository = CatalogoRepository()

    private val _opciones = MutableStateFlow<List<OpcionCatalogo>>(emptyList())
    val opciones: StateFlow<List<OpcionCatalogo>> = _opciones.asStateFlow()

    private val _mensajeFeedback = MutableStateFlow<String?>(null)
    val mensajeFeedback: StateFlow<String?> = _mensajeFeedback.asStateFlow()

    init { cargar() }

    fun cargar() {
        viewModelScope.launch {
            _opciones.value = repository.getAll()
        }
    }

    fun opcionesPorTipo(tipo: TipoCatalogo): List<OpcionCatalogo> {
        return opciones.value.filter { it.tipo == tipo && it.activo }
    }

    fun opcionesPorNombre(tipo: TipoCatalogo, query: String): List<OpcionCatalogo> {
        val q = query.lowercase()
        return opcionesPorTipo(tipo).filter { it.nombre.lowercase().contains(q) }
    }

    fun guardar(opcion: OpcionCatalogo) {
        viewModelScope.launch {
            repository.guardar(opcion)
            cargar()
            _mensajeFeedback.value = "${if (opcion.id.isBlank()) "Creado" else "Actualizado"} correctamente"
        }
    }

    fun eliminar(id: String) {
        viewModelScope.launch {
            repository.eliminar(id)
            cargar()
            _mensajeFeedback.value = "Eliminado correctamente"
        }
    }

    fun limpiarFeedback() { _mensajeFeedback.value = null }
}
