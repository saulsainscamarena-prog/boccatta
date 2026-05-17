package com.bocatta.pos.domain.model

/**
 * Represents a critical synchronization error that needs administrative attention.
 */
data class SyncError(
    val deviceId: String = "",
    val timestamp: Long = 0L,
    val errorMessage: String = "",
    val failedIds: List<Long> = emptyList(),
    val appVersion: String = "",
    val stackTrace: String? = null
)

