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

import com.bocatta.pos.data.seeder.StockCatalogSeeder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BocattaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        LogHelper.init(this, BuildConfig.DEBUG)
        val cleanupRequest = PeriodicWorkRequestBuilder<LogCleanupWorker>(1, TimeUnit.DAYS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "log_cleanup",
            ExistingPeriodicWorkPolicy.KEEP,
            cleanupRequest
        )
        startKoin {
            androidContext(this@BocattaApp)
            modules(
                appModule,
                com.bocatta.pos.core.database.di.databaseModule,
                com.bocatta.pos.core.network.di.networkModule
            )
        }
        // Crear doc pos_stock_catalog en Firestore si no existe (idempotente)
        CoroutineScope(Dispatchers.IO).launch {
            StockCatalogSeeder.ensureExists()
        }
    }
}
