package com.bocatta.pos.feature.ventas.di
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.koin.android.ext.koin.androidApplication
import com.bocatta.pos.feature.ventas.viewmodel.*

val ventasModule = module {
    viewModel { SalesViewModelV2(get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { CajaViewModel(get(), get(), get()) }
    viewModel { ClienteViewModel() }
    viewModel { DevolucionViewModel() }
    viewModel { HeldOrderViewModel(androidApplication()) }
    viewModel { MesaViewModel() }
    viewModel { AperturaViewModelV2() }
    viewModel { HorarioViewModel() }
}
