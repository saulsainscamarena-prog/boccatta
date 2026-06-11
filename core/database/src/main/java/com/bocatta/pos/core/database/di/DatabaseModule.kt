package com.bocatta.pos.core.database.di

import com.bocatta.pos.data.local.room.BocattaRoomDatabase
import org.koin.dsl.module

val databaseModule = module {
    single { BocattaRoomDatabase.getInstance(get()) }
    single { get<BocattaRoomDatabase>().ventaPendienteDao() }
    single { get<BocattaRoomDatabase>().operacionPendienteDao() }
    single { get<BocattaRoomDatabase>().folioDao() }
}
