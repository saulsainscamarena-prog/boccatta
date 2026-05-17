package com.bocatta.pos.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bocatta.pos.data.repository.PromocionesRepository
import com.bocatta.pos.domain.engine.CostCalculator
import com.bocatta.pos.domain.engine.CostoProducto
import com.bocatta.pos.domain.model.PromocionUniversal
import com.bocatta.pos.domain.model.SalesInventoryProductV2
import com.bocatta.pos.domain.model.ItemCombo
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

    fun calcularRentabilidad(
        productoIds: List<String>,
        productos: List<SalesInventoryProductV2>,
        precioPromo: Double
    ): CostoProducto? {
        if (productoIds.isEmpty()) return null
        val costoTotal = productoIds.sumOf { id ->
            productos.find { it.id == id }?.let { prod ->
                prod.precioVenta.values.firstOrNull() ?: 0.0
            } ?: 0.0
        }
        return CostoProducto(
            nombre = "Promoción",
            costoTotal = costoTotal,
            precioVenta = precioPromo,
            margen = precioPromo - costoTotal,
            margenPorcentaje = if (precioPromo > 0) ((precioPromo - costoTotal) / precioPromo * 100) else 0.0
        )
    }

    fun limpiarFeedback() { _mensajeFeedback.value = null }
}

