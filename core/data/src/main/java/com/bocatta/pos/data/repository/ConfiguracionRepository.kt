package com.bocatta.pos.data.repository

import com.bocatta.pos.core.constants.FirestoreCollections
import com.bocatta.pos.network.firebase.FirebaseFirestoreProvider
import com.bocatta.pos.core.model.ConfigItem
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.tasks.await
import timber.log.Timber

class ConfiguracionRepository {
    private val db = FirebaseFirestoreProvider.db

    suspend fun getInsumosApertura(): List<ConfigItem> {
        val result = mutableListOf<ConfigItem>()
        try {
            val snapshot = db.collection(FirestoreCollections.CONFIGURACION)
                .document("apertura_insumos")
                .collection("items")
                .orderBy("orden")
                .get()
                .await()
            snapshot.documents.forEach { d ->
                result.add(ConfigItem(d.id, d.getString("nombre") ?: "", d.getString("unidad") ?: ""))
            }
        } catch (e: Exception) {
            Timber.tag("CONFIG").w(e, "Error al obtener insumos de apertura")
        }
        return result
    }

    suspend fun getMapeoAderezos(): List<ConfigItem> {
        val result = mutableListOf<ConfigItem>()
        try {
            val doc = db.collection(FirestoreCollections.CONFIGURACION)
                .document("mapeo_aderezos")
                .get()
                .await()
            doc.data?.forEach { (nombre, insumoId) ->
                if (insumoId is String) result.add(ConfigItem(nombre, nombre, insumoId))
            }
        } catch (e: Exception) {
            Timber.tag("CONFIG").w(e, "Error al obtener mapeo de aderezos")
        }
        return result
    }

    suspend fun getMapeoToppings(): List<ConfigItem> {
        val result = mutableListOf<ConfigItem>()
        try {
            val doc = db.collection(FirestoreCollections.CONFIGURACION)
                .document("mapeo_toppings")
                .get()
                .await()
            doc.data?.forEach { (nombre, insumoId) ->
                if (insumoId is String) result.add(ConfigItem(nombre, nombre, insumoId))
            }
        } catch (e: Exception) {
            Timber.tag("CONFIG").w(e, "Error al obtener mapeo de toppings")
        }
        return result
    }

    suspend fun guardarInsumoApertura(id: String, nombre: String, unidad: String) {
        val data = mapOf(
            "id" to id,
            "nombre" to nombre,
            "unidad" to unidad,
            "orden" to System.currentTimeMillis()
        )
        db.collection(FirestoreCollections.CONFIGURACION)
            .document("apertura_insumos")
            .collection("items")
            .document(id)
            .set(data)
            .await()
    }

    suspend fun guardarMapeo(coleccion: String, nombre: String, insumoId: String) {
        try {
            db.collection(FirestoreCollections.CONFIGURACION).document(coleccion)
                .update(nombre, insumoId).await()
        } catch (e: Exception) {
            // Document doesn't exist, create it
            db.collection(FirestoreCollections.CONFIGURACION).document(coleccion)
                .set(mapOf(nombre to insumoId)).await()
        }
    }

    suspend fun eliminarInsumoApertura(id: String) {
        db.collection(FirestoreCollections.CONFIGURACION)
            .document("apertura_insumos")
            .collection("items")
            .document(id)
            .delete()
            .await()
    }

    suspend fun eliminarMapeo(coleccion: String, nombre: String) {
        db.collection(FirestoreCollections.CONFIGURACION).document(coleccion)
            .update(nombre, FieldValue.delete())
            .await()
    }

    suspend fun getParametrosCaja(): Map<String, Double> {
        return try {
            val doc = db.collection(FirestoreCollections.CONFIGURACION)
                .document("parametros_caja")
                .get()
                .await()
            mapOf(
                "tolerancia_efectivo" to (doc.getDouble("tolerancia_efectivo") ?: 10.0),
                "tolerancia_tarjeta" to (doc.getDouble("tolerancia_tarjeta") ?: 5.0)
            )
        } catch (e: Exception) {
            mapOf("tolerancia_efectivo" to 10.0, "tolerancia_tarjeta" to 5.0)
        }
    }

    suspend fun guardarParametrosCaja(toleranciaEfectivo: Double, toleranciaTarjeta: Double) {
        val data = mapOf(
            "tolerancia_efectivo" to toleranciaEfectivo,
            "tolerancia_tarjeta" to toleranciaTarjeta
        )
        db.collection(FirestoreCollections.CONFIGURACION)
            .document("parametros_caja")
            .set(data)
            .await()
    }

    suspend fun guardarParametrosCaja(params: Map<String, Any?>) {
        db.collection(FirestoreCollections.CONFIGURACION)
            .document("parametros_caja")
            .set(params)
            .await()
    }
}


