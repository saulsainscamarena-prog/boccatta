package com.bocatta.pos.di

import com.bocatta.pos.feature.ventas.di.SalesDependencies
import com.bocatta.pos.data.local.OfflineDatabase
import com.bocatta.pos.data.local.room.BocattaRoomDatabase
import com.bocatta.pos.data.repository.AuthRepository
import com.bocatta.pos.data.repository.ConfiguracionRepository
import com.bocatta.pos.data.repository.DataSeederV2
import com.bocatta.pos.data.repository.FirebaseSalesRepositoryV2
import com.bocatta.pos.data.repository.InventoryRepository
import com.bocatta.pos.data.repository.MaintenanceRepository
import com.bocatta.pos.data.repository.OperationalCatalogSyncRepository
import com.bocatta.pos.data.repository.PromocionesRepository
import com.bocatta.pos.data.repository.ReportRepository
import com.bocatta.pos.data.repository.ThemeRepository
import com.bocatta.pos.data.repository.CustomerRepository
import com.bocatta.pos.data.repository.ProductRepositoryImpl
import com.bocatta.pos.data.repository.InventoryRepositoryImpl
import com.bocatta.pos.domain.repository.IInventoryRepository
import com.bocatta.pos.domain.repository.IProductRepository
import com.bocatta.pos.domain.usecase.GenerarTicketWhatsAppUseCase
import com.bocatta.pos.domain.usecase.PromocionesEngine
import com.bocatta.pos.domain.usecase.PromotionsEngineV2
import com.bocatta.pos.domain.usecase.ProductionBatchUseCase
import com.bocatta.pos.domain.usecase.RegistrarMermaProductoUseCase
import com.bocatta.pos.domain.usecase.SalesFlowUseCase
import com.bocatta.pos.domain.usecase.AuthorizationManager
import com.bocatta.pos.domain.usecase.CatalogoOperativoUseCase
import com.bocatta.pos.domain.usecase.GestionEmpleadosUseCase
import com.bocatta.pos.feature.ventas.usecase.CartManager
import com.bocatta.pos.domain.usecase.CheckoutUseCase
import com.bocatta.pos.domain.usecase.impl.CatalogoOperativoUseCaseImpl
import com.bocatta.pos.network.NetworkStateProvider
import com.bocatta.pos.feature.admin.viewmodel.AdminViewModel
import com.bocatta.pos.feature.admin.viewmodel.ConfigGlobalViewModel
import com.bocatta.pos.feature.admin.viewmodel.SalarioViewModel
import com.bocatta.pos.feature.admin.viewmodel.SolicitudViewModel
import com.bocatta.pos.feature.ventas.viewmodel.AperturaViewModelV2
import com.bocatta.pos.presentation.viewmodel.AuditoriaViewModel
import com.bocatta.pos.feature.auth.viewmodel.AuthViewModelV2
import com.bocatta.pos.feature.ventas.viewmodel.CajaViewModel
import com.bocatta.pos.feature.ventas.viewmodel.ClienteViewModel
import com.bocatta.pos.feature.inventario.viewmodel.ComprasViewModel
import com.bocatta.pos.feature.admin.viewmodel.ConfigNegocioViewModel
import com.bocatta.pos.feature.admin.viewmodel.ConfigViewModel
import com.bocatta.pos.feature.inventario.viewmodel.DashboardBodegaViewModel
import com.bocatta.pos.feature.ventas.viewmodel.DevolucionViewModel
import com.bocatta.pos.feature.admin.viewmodel.EmployeeViewModelV2
import com.bocatta.pos.feature.admin.viewmodel.ExpensesViewModelV2
import com.bocatta.pos.feature.admin.viewmodel.GestionSucursalesViewModel
import com.bocatta.pos.feature.ventas.viewmodel.HeldOrderViewModel
import com.bocatta.pos.feature.ventas.viewmodel.HorarioViewModel
import com.bocatta.pos.feature.inventario.viewmodel.InventarioAdminViewModel
import com.bocatta.pos.feature.inventario.viewmodel.InventoryAdjustmentViewModelV2
import com.bocatta.pos.feature.inventario.viewmodel.InventoryViewModel
import com.bocatta.pos.feature.ventas.viewmodel.MesaViewModel
import com.bocatta.pos.presentation.viewmodel.MenuViewModel
import com.bocatta.pos.feature.admin.viewmodel.ReportViewModelV2
import com.bocatta.pos.feature.inventario.viewmodel.ReportesInventarioViewModel
import com.bocatta.pos.feature.ventas.viewmodel.SalesViewModelV2
import com.bocatta.pos.feature.auth.viewmodel.SessionViewModel
import com.bocatta.pos.feature.inventario.viewmodel.SyncInventarioViewModel
import com.bocatta.pos.feature.admin.viewmodel.ThemeViewModel
import com.bocatta.pos.data.queue.SQLiteStockAdjustmentQueue
import com.bocatta.pos.domain.repository.IStockAdjustmentQueue
import com.bocatta.pos.domain.repository.ISyncErrorRepository
import com.bocatta.pos.data.repository.SyncErrorRepositoryImpl
import com.bocatta.pos.data.repository.ProductoRepository
import com.bocatta.pos.data.security.SharedPreferencesPinRateLimitStore
import com.bocatta.pos.domain.usecase.PinRateLimitStore
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // REPOSITORIOS
    single { AuthRepository() }
    single<com.bocatta.pos.domain.repository.SalesRepository> { FirebaseSalesRepositoryV2(get(), get()) }
    single { OfflineDatabase.getInstance(get()) }
    // Room database definition moved to :core:database
    single { InventoryRepository(get()) }
    single { MaintenanceRepository() }
    single { ReportRepository() }
    single { PromocionesRepository() }
    single { ThemeRepository() }
    single { CustomerRepository() }
    single { ProductoRepository() }
    single { DataSeederV2(androidApplication(), get()) }       // recibe Context y ProductoRepository inyectado
    single { ConfiguracionRepository() }
    single { OperationalCatalogSyncRepository(androidApplication(), get()) }

    // QUEUE BINDING
    single<IStockAdjustmentQueue> { SQLiteStockAdjustmentQueue(androidApplication()) }
    single<ISyncErrorRepository> { SyncErrorRepositoryImpl() }

    // V2 REPOSITORIOS (NUEVA ARQUITECTURA)
    single<IProductRepository> { ProductRepositoryImpl() }
    single<IInventoryRepository> { InventoryRepositoryImpl(get(), get(), get()) }

    // NETWORK module extracted to :core:network

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
    factory { CartManager() }
    single { CheckoutUseCase(androidApplication(), get(), get(), get(), get()) }
    single { com.bocatta.pos.domain.usecase.TenantSessionManager() }

    // SALES DEPENDENCIES
    single {
        SalesDependencies(
            productRepo = get(),
            inventoryRepo = get(),
            salesFlowUseCase = get(),
            repository = get(),
            generarTicketWhatsAppUseCase = get(),
            registrarMermaProductoUseCase = get(),
            promocionesEngine = get(),
            promocionesRepository = get()
        )
    }

    // VIEWMODELS
    viewModel { SessionViewModel(get(), get()) }
    viewModel { SalesViewModelV2(get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { CajaViewModel(get(), get(), get()) }
    viewModel { AdminViewModel(get(), get(), get(), get()) }
    viewModel { InventoryViewModel(get()) }
    viewModel { ReportViewModelV2(get()) }
    viewModel { AperturaViewModelV2() }
    viewModel { ExpensesViewModelV2() }
    viewModel { DevolucionViewModel() }
    viewModel { ClienteViewModel() }
    viewModel { AuthViewModelV2(get()) }
    viewModel { DashboardBodegaViewModel() }
    viewModel { ReportesInventarioViewModel() }
    viewModel { InventoryAdjustmentViewModelV2() }
    viewModel { EmployeeViewModelV2(get(), get()) }
    viewModel { ComprasViewModel() }
    viewModel { SyncInventarioViewModel() }
    viewModel { MenuViewModel() }
    viewModel { ConfigViewModel() }
    viewModel { AuditoriaViewModel() }
    viewModel { HorarioViewModel() }
    viewModel { InventarioAdminViewModel(get()) }
    viewModel { ConfigNegocioViewModel(get()) }
    viewModel { GestionSucursalesViewModel(get(), get()) }  // seeder + productoRepo
    viewModel { ThemeViewModel(get()) }
    viewModel { MesaViewModel() }
    viewModel { HeldOrderViewModel(androidApplication()) }
    viewModel { ConfigGlobalViewModel() }
    viewModel { SolicitudViewModel() }
    viewModel { SalarioViewModel() }
}

