package com.bocatta.pos.core.database.di

import com.bocatta.pos.data.local.room.BocattaOfflineDatabase
import org.koin.dsl.module

val databaseModule = module {
    single { BocattaOfflineDatabase.getInstance(get()) }
    single { get<BocattaOfflineDatabase>().ventaPendienteDao() }
    single { get<BocattaOfflineDatabase>().operacionPendienteDao() }
    single { get<BocattaOfflineDatabase>().folioDao() }
    single { get<BocattaOfflineDatabase>().turnoContingenciaDao() }
    single { get<BocattaOfflineDatabase>().productoDao() }
}
