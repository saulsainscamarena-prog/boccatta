package com.bocatta.pos.feature.ventas.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bocatta.pos.domain.model.VentaV2
import com.bocatta.pos.domain.repository.SalesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

data class KdsUiState(
    val pendingOrders: List<VentaV2> = emptyList(),
    val preparingOrders: List<VentaV2> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

class KdsViewModel(
    private val salesRepository: SalesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(KdsUiState())
    val uiState: StateFlow<KdsUiState> = _uiState.asStateFlow()
    private var currentSucursal: String = ""

    fun loadOrders(sucursal: String) {
        if (currentSucursal == sucursal) return
        currentSucursal = sucursal
        if (sucursal.isNotEmpty()) {
            listenToOrders(sucursal)
        } else {
            _uiState.value = _uiState.value.copy(error = "Sucursal no configurada", isLoading = false)
        }
    }

    private fun listenToOrders(sucursal: String) {
        viewModelScope.launch {
            try {
                salesRepository.getActiveKdsOrders(sucursal).collectLatest { orders ->
                    val pending = orders.filter { it.estadoCocina == "pendiente" }
                        .sortedBy { it.fecha }
                    val preparing = orders.filter { it.estadoCocina == "preparando" }
                        .sortedBy { it.fecha }

                    _uiState.value = _uiState.value.copy(
                        pendingOrders = pending,
                        preparingOrders = preparing,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Error listening to KDS orders")
                _uiState.value = _uiState.value.copy(error = e.message, isLoading = false)
            }
        }
    }

    fun markAsPreparing(ventaId: String) {
        viewModelScope.launch {
            val success = salesRepository.updateKdsOrderStatus(ventaId, "preparando", currentSucursal)
            if (!success) {
                _uiState.value = _uiState.value.copy(error = "No se pudo actualizar la orden a Preparando")
            }
        }
    }

    fun markAsReady(ventaId: String) {
        viewModelScope.launch {
            val success = salesRepository.updateKdsOrderStatus(ventaId, "listo", currentSucursal)
            if (!success) {
                _uiState.value = _uiState.value.copy(error = "No se pudo actualizar la orden a Listo")
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
