package com.bocatta.pos.presentation.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.bocatta.pos.data.local.OfflineDatabase
import com.bocatta.pos.data.repository.HeldOrderRepository
import com.bocatta.pos.domain.model.HeldOrder
import com.bocatta.pos.domain.model.ItemCarritoV2
import com.bocatta.pos.domain.model.ClienteV2
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class HeldOrderViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = HeldOrderRepository(OfflineDatabase.getInstance(application))
    private val json = Json { ignoreUnknownKeys = true }

    val orders = mutableStateListOf<HeldOrder>()

    var mensajeFeedback by mutableStateOf<String?>(null)
        private set

    fun loadOrders() {
        orders.clear()
        orders.addAll(repository.getAll())
    }

    fun saveOrder(
        carrito: List<ItemCarritoV2>,
        cliente: ClienteV2?,
        nota: String,
        sucursal: String,
        total: Double
    ) {
        val id = "held_${System.currentTimeMillis()}_${sucursal}"
        val order = HeldOrder(
            id = id,
            carritoJson = json.encodeToString(carrito),
            clienteJson = cliente?.let { json.encodeToString(it) },
            nota = nota,
            sucursal = sucursal,
            total = total
        )
        repository.save(order)
        orders.add(0, order)
        mensajeFeedback = "✅ Orden apartada"
    }

    fun deleteOrder(id: String) {
        repository.delete(id)
        orders.removeAll { it.id == id }
        mensajeFeedback = "🗑️ Orden eliminada"
    }

    fun getOrder(id: String): HeldOrder? = repository.getById(id)

    fun parseCarrito(jsonStr: String): List<ItemCarritoV2> {
        return try {
            json.decodeFromString(jsonStr)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun parseCliente(jsonStr: String?): ClienteV2? {
        if (jsonStr.isNullOrBlank()) return null
        return try {
            json.decodeFromString(jsonStr)
        } catch (_: Exception) {
            null
        }
    }
}
