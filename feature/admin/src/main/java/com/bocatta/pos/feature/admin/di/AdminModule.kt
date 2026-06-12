package com.bocatta.pos.feature.admin.di
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import com.bocatta.pos.feature.admin.viewmodel.*

val adminModule = module {
    viewModel { AdminViewModel(get(), get(), get(), get()) }
    viewModel { ExpensesViewModelV2() }
    viewModel { ReportViewModelV2(get()) }
    viewModel { EmployeeViewModelV2(get(), get()) }
    viewModel { GestionSucursalesViewModel(get(), get()) }
    viewModel { ConfigGlobalViewModel() }
    viewModel { ConfigNegocioViewModel(get()) }
    viewModel { ConfigViewModel() }
    viewModel { SalarioViewModel() }
    viewModel { SolicitudViewModel() }
    viewModel { ThemeViewModel(get()) }
}
