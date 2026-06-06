package com.bocatta.pos.di

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bocatta.pos.presentation.viewmodel.HeldOrderViewModel
import com.bocatta.pos.presentation.viewmodel.MesaViewModel
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.koinApplication

@RunWith(AndroidJUnit4::class)
class KoinAndroidModuleTest {

    @Test
    fun activityViewModels_resolveFromAppModule() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        val koinApp = koinApplication {
            androidContext(context)
            modules(appModule)
        }

        assertNotNull(koinApp.koin.get<MesaViewModel>())
        assertNotNull(koinApp.koin.get<HeldOrderViewModel>())

        koinApp.close()
    }
}
