package com.bocatta.pos.feature.inventario.di
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import com.bocatta.pos.feature.inventario.viewmodel.*

val inventarioModule = module {
    viewModel { InventoryViewModel(get()) }
    viewModel { DashboardBodegaViewModel() }
    viewModel { ReportesInventarioViewModel() }
    viewModel { InventoryAdjustmentViewModelV2() }
    viewModel { ComprasViewModel() }
    viewModel { SyncInventarioViewModel() }
    viewModel { InventarioAdminViewModel(get()) }
}
