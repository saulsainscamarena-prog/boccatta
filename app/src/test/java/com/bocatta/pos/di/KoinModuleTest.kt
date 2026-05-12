package com.bocatta.pos.di

import com.bocatta.pos.domain.usecase.GenerarTicketWhatsAppUseCase
import com.bocatta.pos.domain.usecase.PromocionesEngine
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.koin.dsl.koinApplication
import org.koin.dsl.module

class KoinModuleTest {

    @Test
    fun promocionesEngine_seResuelve() {
        val koinApp = koinApplication {
            modules(module {
                single { PromocionesEngine() }
            })
        }
        assertNotNull(koinApp.koin.get<PromocionesEngine>())
        koinApp.close()
    }

    @Test
    fun generarTicketUseCase_seResuelve() {
        val koinApp = koinApplication {
            modules(module {
                single { GenerarTicketWhatsAppUseCase() }
            })
        }
        assertNotNull(koinApp.koin.get<GenerarTicketWhatsAppUseCase>())
        koinApp.close()
    }

    @Test
    fun modulosKotlinPuros_seResuelven() {
        val koinApp = koinApplication {
            modules(module {
                single { PromocionesEngine() }
                single { GenerarTicketWhatsAppUseCase() }
            })
        }
        assertNotNull(koinApp.koin.get<PromocionesEngine>())
        assertNotNull(koinApp.koin.get<GenerarTicketWhatsAppUseCase>())
        koinApp.close()
    }

    @Test
    fun moduloCompleto_seCargaSinError() {
        val koinApp = koinApplication {
            modules(appModule)
        }
        assertNotNull(koinApp)
        assertNotNull(koinApp.koin)
        koinApp.close()
    }
}
