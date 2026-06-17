package com.bocatta.pos.data.di

import com.bocatta.pos.data.local.OfflineDatabase
import com.bocatta.pos.data.repository.*
import com.bocatta.pos.data.queue.SQLiteStockAdjustmentQueue
import com.bocatta.pos.data.security.SharedPreferencesPinRateLimitStore
import com.bocatta.pos.domain.repository.*
import com.bocatta.pos.domain.usecase.*
import com.bocatta.pos.domain.usecase.impl.CatalogoOperativoUseCaseImpl
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module

val dataModule = module {
    // REPOSITORIOS
    single { AuthRepository() }
    single<com.bocatta.pos.domain.repository.SalesRepository> { FirebaseSalesRepositoryV2(get(), get()) }
    single { OfflineDatabase.getInstance(get()) }
    single { InventoryRepository(get()) }
    single { MaintenanceRepository() }
    single { ReportRepository() }
    single { PromocionesRepository() }
    single { ThemeRepository() }
    single { CustomerRepository() }
    single { ProductoRepository() }
    single { DataSeederV2(androidApplication(), get()) }
    single { ConfiguracionRepository() }
    single { OperationalCatalogSyncRepository(androidApplication(), get()) }

    // QUEUE BINDING
    single<IStockAdjustmentQueue> { SQLiteStockAdjustmentQueue(androidApplication()) }
    single<ISyncErrorRepository> { SyncErrorRepositoryImpl() }

    // V2 REPOSITORIOS (NUEVA ARQUITECTURA)
    single<IProductRepository> { ProductRepositoryImpl() }
    single<IInventoryRepository> { InventoryRepositoryImpl(get(), get(), get()) }

    // USE CASES
    single { GenerarTicketWhatsAppUseCase() }
    single { PromocionesEngine() }
    single { PromotionsEngineV2 }
    single { SalesFlowUseCase(get(), get(), get()) }
    single { ProductionBatchUseCase(get(), get()) }
    single { RegistrarMermaProductoUseCase(androidApplication(), get(), get()) }
    single<PinRateLimitStore> { SharedPreferencesPinRateLimitStore(androidApplication()) }
    single { AuthorizationManager(get()) }
    single<CatalogoOperativoUseCase> { CatalogoOperativoUseCaseImpl() }
    single { GestionEmpleadosUseCase() }
    single { CheckoutUseCase(androidApplication(), get(), get(), get(), get()) }
    single { RegistrarCancelacionUseCase(androidApplication()) }
    single { com.bocatta.pos.domain.usecase.TenantSessionManager() }
}
