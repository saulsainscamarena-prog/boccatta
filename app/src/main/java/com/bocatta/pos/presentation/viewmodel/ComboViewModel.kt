package com.bocatta.pos.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bocatta.pos.data.repository.ComboRepository
import com.bocatta.pos.domain.model.ComboProducto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ComboViewModel(
    private val repository: ComboRepository = ComboRepository()
) : ViewModel() {

    private val _combos = MutableStateFlow<List<ComboProducto>>(emptyList())
    val combos: StateFlow<List<ComboProducto>> = _combos.asStateFlow()

    private val _mensajeFeedback = MutableStateFlow<String?>(null)
    val mensajeFeedback: StateFlow<String?> = _mensajeFeedback.asStateFlow()

    fun cargar() {
        viewModelScope.launch {
            val result = repository.getAll()
            _combos.value = result
        }
    }

    fun guardar(combo: ComboProducto) {
        viewModelScope.launch {
            val success = repository.guardar(combo)
            _mensajeFeedback.value = if (success) "Combo guardado correctamente" else "Error al guardar combo"
        }
    }

    fun eliminar(id: String) {
        viewModelScope.launch {
            val success = repository.eliminar(id)
            _mensajeFeedback.value = if (success) "Combo eliminado" else "Error al eliminar combo"
        }
    }

    fun limpiarFeedback() {
        _mensajeFeedback.value = null
    }

    override fun onCleared() {
        super.onCleared()
        repository.limpiarListener()
    }
}

