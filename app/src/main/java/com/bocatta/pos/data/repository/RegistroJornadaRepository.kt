package com.bocatta.pos.data.repository

import com.bocatta.pos.data.local.OfflineDatabase
import com.bocatta.pos.domain.model.RegistroJornada
import com.bocatta.pos.domain.repository.IRegistroJornadaRepository
import com.bocatta.pos.network.firebase.FirebaseFirestoreProvider
import kotlinx.coroutines.tasks.await
import timber.log.Timber

class RegistroJornadaRepository(
    private val dbHelper: OfflineDatabase = OfflineDatabase.getInstance(FirebaseFirestoreProvider.db.app.applicationContext)
) : IRegistroJornadaRepository {

    private val firestore = FirebaseFirestoreProvider.db.collection("v2_registro_jornadas")

    override suspend fun guardar(registro: RegistroJornada): Boolean {
        return try {
            val doc = if (registro.id.isBlank()) firestore.document() else firestore.document(registro.id)
            val final = registro.copy(id = doc.id)
            doc.set(final).await()
            guardarLocal(final)
            true
        } catch (e: Exception) {
            Timber.e(e, "Error guardando registro jornada, guardando offline")
            guardarLocal(registro.copy(id = registro.id.ifBlank { "offline_${System.currentTimeMillis()}" }))
            false
        }
    }

    override suspend fun getParticipantes(sucursal: String): List<RegistroJornada> {
        return try {
            val snap = firestore
                .whereEqualTo("sucursal", sucursal)
                .whereIn("accion", listOf("inicio_turno", "unirse_turno"))
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get().await()
            snap.documents.mapNotNull { it.toObject(RegistroJornada::class.java)?.copy(id = it.id) }
        } catch (e: Exception) {
            Timber.e(e, "Error obteniendo participantes")
            getParticipantesLocal(sucursal)
        }
    }

    override suspend fun getHistorial(usuario: String): List<RegistroJornada> {
        return try {
            val snap = firestore
                .whereEqualTo("usuario", usuario)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(50)
                .get().await()
            snap.documents.mapNotNull { it.toObject(RegistroJornada::class.java)?.copy(id = it.id) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun guardarLocal(registro: RegistroJornada) {
        try {
            val db = dbHelper.writableDatabase
            val cv = android.content.ContentValues().apply {
                put("id", registro.id)
                put("usuario", registro.usuario)
                put("sucursal", registro.sucursal)
                put("accion", registro.accion)
                put("rol", registro.rol)
                put("sesionId", registro.sesionId)
                put("timestamp", registro.timestamp)
            }
            db.insertWithOnConflict("registro_jornadas", null, cv, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
        } catch (e: Exception) {
            Timber.e(e, "Error guardando registro local")
        }
    }

    private fun getParticipantesLocal(sucursal: String): List<RegistroJornada> {
        val list = mutableListOf<RegistroJornada>()
        try {
            val db = dbHelper.readableDatabase
            val cursor = db.rawQuery(
                "SELECT * FROM registro_jornadas WHERE sucursal = ? AND accion IN ('inicio_turno','unirse_turno') ORDER BY timestamp DESC",
                arrayOf(sucursal)
            )
            cursor.use { c ->
                while (c.moveToNext()) {
                    list.add(RegistroJornada(
                        id = c.getString(c.getColumnIndexOrThrow("id")),
                        usuario = c.getString(c.getColumnIndexOrThrow("usuario")),
                        sucursal = c.getString(c.getColumnIndexOrThrow("sucursal")),
                        accion = c.getString(c.getColumnIndexOrThrow("accion")),
                        rol = c.getString(c.getColumnIndexOrThrow("rol")),
                        sesionId = c.getString(c.getColumnIndexOrThrow("sesionId")),
                        timestamp = c.getLong(c.getColumnIndexOrThrow("timestamp"))
                    ))
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error leyendo participantes locales")
        }
        return list
    }
}
