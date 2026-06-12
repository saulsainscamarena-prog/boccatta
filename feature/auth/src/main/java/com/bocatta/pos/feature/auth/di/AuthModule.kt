package com.bocatta.pos.feature.auth.di
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import com.bocatta.pos.feature.auth.viewmodel.*

val authModule = module {
    viewModel { SessionViewModel(get(), get()) }
    viewModel { AuthViewModelV2(get()) }
}
