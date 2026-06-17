package com.bocatta.pos.core.session.di

import com.bocatta.pos.core.session.viewmodel.SessionViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val sessionModule = module {
    viewModel { SessionViewModel(get(), get()) }
}
