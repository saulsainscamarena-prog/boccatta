package com.bocatta.pos.data.repository

import com.bocatta.pos.domain.model.ConfiguracionSalarial
import com.bocatta.pos.domain.model.RegistroPago
import com.bocatta.pos.domain.repository.ISalarioRepository
import com.bocatta.pos.network.firebase.FirebaseFirestoreProvider
import kotlinx.coroutines.tasks.await
import timber.log.Timber

class ConfiguracionSalarialRepository : ISalarioRepository {
    private val configCol = FirebaseFirestoreProvider.db.collection("v2_configuracion_salarial")
    private val pagosCol = FirebaseFirestoreProvider.db.collection("v2_pagos")

    override suspend fun getConfiguracion(empleadoId: String): ConfiguracionSalarial? = try {
        val snap = configCol.whereEqualTo("empleadoId", empleadoId).limit(1).get().await()
        snap.documents.firstOrNull()?.toObject(ConfiguracionSalarial::class.java)
    } catch (e: Exception) { Timber.e(e, "Error getConfiguracion"); null }

    override suspend fun guardarConfiguracion(config: ConfiguracionSalarial): Boolean = try {
        val doc = configCol.document()
        doc.set(config).await(); true
    } catch (e: Exception) { Timber.e(e, "Error guardarConfiguracion"); false }

    override suspend fun getPagos(empleadoId: String): List<RegistroPago> = try {
        pagosCol.whereEqualTo("empleadoId", empleadoId)
            .orderBy("periodoInicio", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get().await().documents.mapNotNull {
                it.toObject(RegistroPago::class.java)?.copy(id = it.id)
            }
    } catch (e: Exception) { Timber.e(e, "Error getPagos"); emptyList() }

    override suspend fun getPagosDelPeriodo(inicio: Long, fin: Long): List<RegistroPago> = try {
        pagosCol.whereGreaterThanOrEqualTo("periodoInicio", inicio)
            .whereLessThanOrEqualTo("periodoFin", fin)
            .get().await().documents.mapNotNull {
                it.toObject(RegistroPago::class.java)?.copy(id = it.id)
            }
    } catch (e: Exception) { Timber.e(e, "Error getPagosDelPeriodo"); emptyList() }

    override suspend fun registrarPago(pago: RegistroPago): Boolean = try {
        val doc = pagosCol.document()
        doc.set(pago.copy(id = doc.id)).await(); true
    } catch (e: Exception) { Timber.e(e, "Error registrarPago"); false }

    override suspend fun marcarPagado(id: String, fecha: Long): Boolean = try {
        pagosCol.document(id).update("pagado", true, "fechaPago", fecha).await(); true
    } catch (e: Exception) { false }
}

