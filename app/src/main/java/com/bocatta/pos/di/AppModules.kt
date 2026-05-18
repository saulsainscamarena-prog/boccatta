package com.bocatta.pos.di

import com.bocatta.pos.data.local.OfflineDatabase
import com.bocatta.pos.data.repository.AuthRepository
import com.bocatta.pos.data.repository.ConfiguracionRepository
import com.bocatta.pos.data.repository.DataSeederV2
import com.bocatta.pos.data.repository.FirebaseSalesRepositoryV2
import com.bocatta.pos.data.repository.InventoryRepository
import com.bocatta.pos.data.repository.MaintenanceRepository
import com.bocatta.pos.data.repository.PromocionesRepository
import com.bocatta.pos.data.repository.ReportRepository
import com.bocatta.pos.data.repository.ProductRepositoryImpl
import com.bocatta.pos.data.repository.InventoryRepositoryImpl
import com.bocatta.pos.domain.repository.IInventoryRepository
import com.bocatta.pos.domain.repository.IProductRepository
import com.bocatta.pos.domain.usecase.GenerarTicketWhatsAppUseCase
import com.bocatta.pos.domain.usecase.PromocionesEngine
import com.bocatta.pos.domain.usecase.PromotionsEngineV2
import com.bocatta.pos.domain.usecase.ProductionBatchUseCase
import com.bocatta.pos.domain.usecase.SalesFlowUseCase
import com.bocatta.pos.network.NetworkStateProvider
import com.bocatta.pos.presentation.viewmodel.AdminViewModel
import com.bocatta.pos.presentation.viewmodel.AperturaViewModelV2
import com.bocatta.pos.presentation.viewmodel.AuditoriaViewModel
import com.bocatta.pos.presentation.viewmodel.AuthViewModelV2
import com.bocatta.pos.presentation.viewmodel.CajaViewModel
import com.bocatta.pos.presentation.viewmodel.ClienteViewModel
import com.bocatta.pos.presentation.viewmodel.ConfigNegocioViewModel
import com.bocatta.pos.presentation.viewmodel.ConfigViewModel
import com.bocatta.pos.presentation.viewmodel.DashboardBodegaViewModel
import com.bocatta.pos.presentation.viewmodel.DevolucionViewModel
import com.bocatta.pos.presentation.viewmodel.EmployeeViewModelV2
import com.bocatta.pos.presentation.viewmodel.ExpensesViewModelV2
import com.bocatta.pos.presentation.viewmodel.GestionSucursalesViewModel
import com.bocatta.pos.presentation.viewmodel.HorarioViewModel
import com.bocatta.pos.presentation.viewmodel.InventarioAdminViewModel
import com.bocatta.pos.presentation.viewmodel.InventoryAdjustmentViewModelV2
import com.bocatta.pos.presentation.viewmodel.InventoryViewModel
import com.bocatta.pos.presentation.viewmodel.MenuViewModel
import com.bocatta.pos.presentation.viewmodel.PurchasesViewModel
import com.bocatta.pos.presentation.viewmodel.ReportViewModelV2
import com.bocatta.pos.presentation.viewmodel.ReportesInventarioViewModel
import com.bocatta.pos.presentation.viewmodel.SalesViewModelV2
import com.bocatta.pos.presentation.viewmodel.SessionViewModel
import com.bocatta.pos.data.queue.SQLiteStockAdjustmentQueue
import com.bocatta.pos.domain.repository.IStockAdjustmentQueue
import com.bocatta.pos.domain.repository.ISyncErrorRepository
import com.bocatta.pos.data.repository.SyncErrorRepositoryImpl
import com.bocatta.pos.data.repository.ProductoRepository
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // ── REPOSITORIOS ─────────────────────────────────────────────────────────
    single { AuthRepository() }
    single<com.bocatta.pos.domain.repository.SalesRepository> { FirebaseSalesRepositoryV2() }
    single { OfflineDatabase.getInstance(get()) }
    // Removed AppDatabase binding (Room) – using native SQLite queue instead
    single { InventoryRepository(get()) }
    single { MaintenanceRepository() }
    single { ReportRepository() }
    single { PromocionesRepository() }
    single { ProductoRepository() }
    single { DataSeederV2(get()) }       // recibe ProductoRepository inyectado
    single { ConfiguracionRepository() }

    // ── QUEUE BINDING ────────────────────────────────────────────────────────
    single<IStockAdjustmentQueue> { SQLiteStockAdjustmentQueue(androidApplication()) }
    single<ISyncErrorRepository> { SyncErrorRepositoryImpl() }

    // ── V2 REPOSITORIOS (NUEVA ARQUITECTURA) ──────────────────────────────────
    single<IProductRepository> { ProductRepositoryImpl() }
    single<IInventoryRepository> { InventoryRepositoryImpl(get(), get()) }

    // ── NETWORK ───────────────────────────────────────────────────────────────
    single { NetworkStateProvider(get()) }

    // ── USE CASES ─────────────────────────────────────────────────────────────
    single { GenerarTicketWhatsAppUseCase() }
    single { PromocionesEngine() }
    single { PromotionsEngineV2 }
    single { SalesFlowUseCase(get(), get(), get()) }
    single { ProductionBatchUseCase(get(), get()) }

    // ── SALES DEPENDENCIES ─────────────────────────────────────────────────────
    single {
        SalesDependencies(
            productRepo = get(),
            inventoryRepo = get(),
            salesFlowUseCase = get(),
            repository = get(),
            generarTicketWhatsAppUseCase = get(),
            promocionesEngine = get(),
            promocionesRepository = get()
        )
    }

    // ── VIEWMODELS ────────────────────────────────────────────────────────────
    viewModel { SessionViewModel(get()) }
    viewModel { SalesViewModelV2(get(), get(), get()) }
    viewModel { CajaViewModel() }
    viewModel { AdminViewModel(get(), get()) }
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
    viewModel { EmployeeViewModelV2(get()) }
    viewModel { PurchasesViewModel(get()) }
    viewModel { MenuViewModel() }
    viewModel { ConfigViewModel() }
    viewModel { AuditoriaViewModel() }
    viewModel { HorarioViewModel() }
    viewModel { InventarioAdminViewModel(get()) }
    viewModel { ConfigNegocioViewModel(get()) }
    viewModel { GestionSucursalesViewModel(get(), get()) }  // seeder + productoRepo
}

