package com.bocatta.pos.core.network.di

import com.bocatta.pos.network.NetworkStateProvider
import org.koin.dsl.module

val networkModule = module {
    single { NetworkStateProvider(get()) }
}
