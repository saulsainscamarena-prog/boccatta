package com.bocatta.pos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.bocatta.pos.navigation.AppNavGraph
import com.bocatta.pos.navigation.Routes
import com.bocatta.pos.logging.LogHelper
import com.bocatta.pos.network.firebase.FirebaseFirestoreProvider
import com.bocatta.pos.presentation.ui.theme.BocattaTheme
import com.bocatta.pos.presentation.viewmodel.*
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel as androidKoinViewModel

class MainActivity : ComponentActivity() {

    private val sessionVm: SessionViewModel by androidKoinViewModel()
    private val authVm: AuthViewModelV2 by androidKoinViewModel()
    private val cajaVm: CajaViewModel by androidKoinViewModel()
    private val inventoryVm: InventoryViewModel by androidKoinViewModel()
    private val salesVmV2: SalesViewModelV2 by androidKoinViewModel()
    private val heldOrderVm: HeldOrderViewModel by androidKoinViewModel()
    private val mesaVm: MesaViewModel by androidKoinViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LogHelper.recordBreadcrumb("activity_create", "MainActivity")
        enableEdgeToEdge()
        window.isNavigationBarContrastEnforced = false

        try {
            FirebaseFirestoreProvider.db.firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(PersistentCacheSettings.newBuilder()
                    .setSizeBytes(100L * 1024L * 1024L).build())
                .build()
        } catch (e: Exception) {
            android.util.Log.w("MainActivity", "Firestore settings ya configurados", e)
        }

        setContent {
            val appContext = this@MainActivity.applicationContext
            val themeVm: ThemeViewModel = koinViewModel()
            val themeConfig by themeVm.config.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val useDark = sessionVm.modoOscuroManual ?: systemDark

            BocattaTheme(darkTheme = useDark, dynamicColor = true, themeConfig = themeConfig) {
                val navController = rememberNavController()

                LaunchedEffect(navController) {
                    navController.currentBackStackEntryFlow.collect { entry ->
                        LogHelper.recordBreadcrumb(
                            event = "screen",
                            detail = entry.destination.route ?: "destination_${entry.destination.id}"
                        )
                    }
                }

                LaunchedEffect(authVm.estaLogueado) {
                    if (authVm.estaLogueado) {
                        com.bocatta.pos.data.sync.SyncScheduler.schedule(appContext)
                        themeVm.iniciarObservacionSiAutenticado()
                    } else {
                        navController.navigate(Routes.Login) { popUpTo(0) { inclusive = true } }
                    }
                }

                AppNavGraph(
                    navController = navController,
                    authVm = authVm,
                    sessionVm = sessionVm,
                    cajaVm = cajaVm,
                    inventoryVm = inventoryVm,
                    salesVmV2 = salesVmV2,
                    heldOrderVm = heldOrderVm,
                    mesaVm = mesaVm
                )
            }
        }
    }
}
