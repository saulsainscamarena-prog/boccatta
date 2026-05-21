package com.bocatta.pos.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bocatta.pos.data.repository.PromocionesRepository
import com.bocatta.pos.domain.model.PromocionUniversal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PromocionViewModel : ViewModel() {

    private val repository = PromocionesRepository()

    private val _promociones = MutableStateFlow<List<PromocionUniversal>>(emptyList())
    val promociones: StateFlow<List<PromocionUniversal>> = _promociones.asStateFlow()

    private val _mensajeFeedback = MutableStateFlow<String?>(null)
    val mensajeFeedback: StateFlow<String?> = _mensajeFeedback.asStateFlow()

    init { cargar() }

    fun cargar() {
        viewModelScope.launch { _promociones.value = repository.getAllPromociones() }
    }

    fun guardar(promo: PromocionUniversal) {
        viewModelScope.launch {
            repository.guardarPromocion(promo)
            cargar()
            _mensajeFeedback.value = "Promoción ${if (promo.id.isBlank()) "creada" else "actualizada"}"
        }
    }

    fun eliminar(id: String) {
        viewModelScope.launch {
            repository.eliminarPromocion(id)
            cargar()
            _mensajeFeedback.value = "Promoción eliminada"
        }
    }

    fun limpiarFeedback() { _mensajeFeedback.value = null }
}

