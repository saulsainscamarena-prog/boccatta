package com.bocatta.pos.logging

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import timber.log.Timber

/**
 * Worker that removes log files older than 15 days.
 * Invoked daily by WorkManager from BocattaApp.
 */
class LogCleanupWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {
    override fun doWork(): Result {
        return try {
            LogHelper.pruneOldLogs(applicationContext)
            Timber.tag("LOG_CLEANUP").i("Limpieza de logs completada")
            Result.success()
        } catch (e: Exception) {
            Timber.tag("LOG_CLEANUP").e(e, "Error durante la limpieza de logs")
            Result.failure()
        }
    }
}


