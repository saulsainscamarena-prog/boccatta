package com.bocatta.pos.domain.repository

import com.bocatta.pos.domain.model.SyncError

/**
 * Contract for reporting critical synchronization errors to the backend.
 */
interface ISyncErrorRepository {
    suspend fun reportError(error: SyncError): Boolean
}

