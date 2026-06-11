package com.bocatta.pos

import android.app.Application
import com.bocatta.pos.di.appModule
import com.bocatta.pos.logging.LogHelper
import com.bocatta.pos.logging.LogCleanupWorker
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class BocattaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Inicializa Timber y el arbol de archivo. DebugTree solo se habilita en debug.
        LogHelper.init(this, BuildConfig.DEBUG)
        // Programa limpieza diaria de logs (>15 días) mediante WorkManager
        // Programa limpieza diaria de logs (>15 días) mediante WorkManager
        val cleanupRequest = PeriodicWorkRequestBuilder<LogCleanupWorker>(1, TimeUnit.DAYS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "log_cleanup",
            ExistingPeriodicWorkPolicy.KEEP,
            cleanupRequest
        )
        // Inicializar Koin
        startKoin {
            androidContext(this@BocattaApp)
            modules(
                appModule,
                com.bocatta.pos.core.database.di.databaseModule,
                com.bocatta.pos.core.network.di.networkModule
            )
        }
    }
}
