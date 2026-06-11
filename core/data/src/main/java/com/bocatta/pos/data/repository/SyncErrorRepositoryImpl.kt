package com.bocatta.pos.data.repository

import com.bocatta.pos.domain.model.SyncError
import com.bocatta.pos.domain.repository.ISyncErrorRepository
import com.bocatta.pos.network.firebase.FirebaseFirestoreProvider
import com.bocatta.pos.core.constants.FirestoreCollections
import kotlinx.coroutines.tasks.await

class SyncErrorRepositoryImpl : ISyncErrorRepository {
    private val db = FirebaseFirestoreProvider.db

    override suspend fun reportError(error: SyncError): Boolean {
        return try {
            val docRef = db.collection("sync_errors").document()
            docRef.set(mapOf(
                "id" to docRef.id,
                "deviceId" to error.deviceId,
                "timestamp" to error.timestamp,
                "errorMessage" to error.errorMessage,
                "failedIds" to error.failedIds,
                "failedSaleIds" to error.failedSaleIds,
                "appVersion" to error.appVersion,
                "stackTrace" to error.stackTrace
            )).await()
            true
        } catch (e: Exception) {
            false
        }
    }
}

