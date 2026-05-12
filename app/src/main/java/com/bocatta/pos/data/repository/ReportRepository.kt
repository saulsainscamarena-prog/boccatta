package com.bocatta.pos.data.repository

import com.bocatta.pos.network.firebase.FirebaseFirestoreProvider
import kotlinx.coroutines.tasks.await
import com.bocatta.pos.core.constants.FirestoreCollections

class ReportRepository {
    private val db = FirebaseFirestoreProvider.db

    suspend fun obtenerResumenDiario(sucursal: String, fecha: Long): ReporteDiarioResumen {
        return try {
            val vSnap = db.collection(FirestoreCollections.VENTAS)
                .whereEqualTo("sucursal", sucursal)
                .whereGreaterThanOrEqualTo("fecha", fecha)
                .get().await()
            
            val gSnap = db.collection(FirestoreCollections.GASTOS)
                .whereEqualTo("sucursal", sucursal)
                .whereGreaterThanOrEqualTo("fecha", fecha)
                .get().await()

            // Cargar datos de costos (Productos e Insumos)
            val pSnap = db.collection(FirestoreCollections.PRODUCTOS).get().await()
            val sSnap = db.collection(FirestoreCollections.INSUMOS).get().await()
            
            val costosInsumos = sSnap.documents.associate { it.id to (it.getDouble("costoUnitarioBase") ?: 0.0) }

            var bruto = 0.0
            var efec = 0.0
            var tarj = 0.0
            var costoTotal = 0.0

            vSnap.documents.forEach { doc ->
                if (doc.getString("estado") == "devuelta") return@forEach
                val t = doc.getDouble("total") ?: 0.0
                bruto += t
                if (doc.getString("metodoPago") == "Tarjeta") tarj += t else efec += t

                @Suppress("UNCHECKED_CAST")
                val productos = doc.get("productos") as? List<Map<String, Any>> ?: emptyList()
                productos.forEach { linea ->
                    @Suppress("UNCHECKED_CAST")
                    val deducciones = linea["deducciones"] as? Map<String, Any> ?: emptyMap()
                    deducciones.forEach { (insumoId, cantidad) ->
                        val qty = (cantidad as? Number)?.toDouble() ?: 0.0
                        costoTotal += (costosInsumos[insumoId] ?: 0.0) * qty
                    }
                }
            }

            val totalGastos = gSnap.documents.sumOf { it.getDouble("monto") ?: 0.0 }
            
            ReporteDiarioResumen(
                ventasBrutas = bruto,
                ventasEfectivo = efec,
                ventasTarjeta = tarj,
                totalGastos = totalGastos,
                costoProduccionReal = costoTotal
            )
        } catch (e: Exception) {
            android.util.Log.e("ReportRepo", "Error generando reporte", e)
            ReporteDiarioResumen()
        }
    }
}

data class ReporteDiarioResumen(
    val ventasBrutas: Double = 0.0,
    val ventasEfectivo: Double = 0.0,
    val ventasTarjeta: Double = 0.0,
    val totalGastos: Double = 0.0,
    val costoProduccionReal: Double = 0.0
)


