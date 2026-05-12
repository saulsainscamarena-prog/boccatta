package com.bocatta.pos.domain.model

/**
 * Represents a critical synchronization error that needs administrative attention.
 */
data class SyncError(
    val deviceId: String,
    val timestamp: Long,
    val errorMessage: String,
    val failedIds: List<Long>,
    val appVersion: String,
    val stackTrace: String? = null
)
