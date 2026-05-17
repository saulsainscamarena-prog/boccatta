package com.bocatta.pos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.koin.androidx.compose.koinViewModel
import com.bocatta.pos.presentation.ui.screens.inventario.InicioDiaScreen
import com.bocatta.pos.presentation.ui.screens.compras.ComprasScreen
import com.bocatta.pos.presentation.ui.screens.admin.AdminScreen
import com.bocatta.pos.presentation.ui.screens.admin.GestionSucursalesScreen
import com.bocatta.pos.presentation.ui.screens.login.LoginScreen
import com.bocatta.pos.presentation.ui.screens.caja.CierreCajaScreen
import com.bocatta.pos.presentation.ui.screens.clientes.ClientesScreen
import com.bocatta.pos.presentation.ui.screens.devoluciones.DevolucionesScreen
import com.bocatta.pos.presentation.ui.screens.gastos.GastosScreen
import com.bocatta.pos.presentation.ui.screens.inventario.CierreInventarioScreen
import com.bocatta.pos.presentation.ui.screens.inventario.InventoryScreen
import com.bocatta.pos.presentation.ui.screens.inventario.AperturaInventarioScreen
import com.bocatta.pos.presentation.ui.screens.reportes.ReportScreen
import com.bocatta.pos.presentation.ui.screens.reportes.ReportesInventarioScreen
import com.bocatta.pos.presentation.ui.screens.inventario.SyncInventarioScreen
import com.bocatta.pos.presentation.ui.screens.bodega.DashboardBodegaScreen
import com.bocatta.pos.presentation.ui.screens.ventas.SalesScreen
import com.bocatta.pos.presentation.ui.screens.operacion.AperturaDiaScreen
import com.bocatta.pos.presentation.ui.screens.operacion.TurnosScreen
import com.bocatta.pos.data.repository.RegistroJornadaRepository
import com.bocatta.pos.domain.model.RegistroJornada
import com.bocatta.pos.presentation.ui.theme.BocattaTheme
import com.bocatta.pos.presentation.ui.theme.BocattaPrimary
import com.bocatta.pos.presentation.viewmodel.*
import com.bocatta.pos.navigation.Routes
import com.bocatta.pos.network.firebase.FirebaseFirestoreProvider
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import org.koin.androidx.viewmodel.ext.android.viewModel as androidKoinViewModel

class MainActivity : ComponentActivity() {

    private val sessionVm: SessionViewModel by androidKoinViewModel()
    private val authVm: AuthViewModelV2 by androidKoinViewModel()
    private val cajaVm: CajaViewModel by androidKoinViewModel()
    private val inventoryVm: InventoryViewModel by androidKoinViewModel()
    private val salesVmV2: SalesViewModelV2 by androidKoinViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.isNavigationBarContrastEnforced = false

        try {
            FirebaseFirestoreProvider.db.firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(PersistentCacheSettings.newBuilder()
                    .setSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED).build())
                .build()
        } catch (e: Exception) {
            android.util.Log.w("MainActivity", "Firestore settings ya configurados", e)
        }

        // Ejecutar el seeder para cargar documentos a Firestore (Se puede comentar despues de la primera ejecucion)
        if (BuildConfig.DEBUG) {
            com.bocatta.pos.data.FirestoreSeeder.seedV2Collections()
        }

        com.bocatta.pos.data.sync.SyncScheduler.schedule(this)

        setContent {
            val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
            val useDark = sessionVm.modoOscuroManual ?: systemDark

            BocattaTheme(darkTheme = useDark, dynamicColor = true) {
                val navController = rememberNavController()

                LaunchedEffect(authVm.estaLogueado) {
                    if (!authVm.estaLogueado) {
                        navController.navigate(Routes.Login) { popUpTo(0) { inclusive = true } }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = if (authVm.estaLogueado) Routes.Turnos else Routes.Login
                ) {
                    composable<Routes.Login> {
                        LoginScreen(vm = authVm, onLoginExitoso = { uid ->
                            sessionVm.cargarUsuario(uid)
                            navController.navigate(Routes.Turnos) { popUpTo("login") { inclusive = true } }
                        })
                    }

                    composable<Routes.Turnos> {
                        val ivmLocal: InventoryViewModel = koinViewModel()
                        val horarioVm: HorarioViewModel = koinViewModel()
                        LaunchedEffect(sessionVm.sucursalActual) {
                            cajaVm.configurarSucursal(sessionVm.sucursalActual)
                            ivmLocal.configurarSucursal(sessionVm.sucursalActual)
                            if (sessionVm.uid != null) horarioVm.cargarJornadaActiva(sessionVm.uid!!)
                        }
                        val participList = remember { mutableStateListOf<RegistroJornada>() }
                        LaunchedEffect(Unit) {
                            val repo = RegistroJornadaRepository()
                            participList.clear()
                            participList.addAll(repo.getParticipantes(sessionVm.sucursalActual))
                        }
                        TurnosScreen(
                            sessionVm = sessionVm,
                            cajaVm = cajaVm,
                            inventarioVm = ivmLocal,
                            participantes = participList,
                            jornadaActiva = horarioVm.jornadaActiva != null,
                            onIniciarTurno = { navController.navigate(Routes.Apertura) { popUpTo("turnos") { inclusive = true } } },
                            onUnirseTurno = { navController.navigate(Routes.Ventas) { popUpTo("turnos") { inclusive = true } } },
                            onAdministrarTienda = { if (sessionVm.esAdmin) navController.navigate(Routes.Admin) { popUpTo("turnos") { inclusive = true } } },
                            onLogout = { sessionVm.cerrarSesion(); authVm.logout() }
                        )
                    }

                    composable<Routes.Apertura> {
                        val aperturaVmV2: AperturaViewModelV2 = koinViewModel()
                        val aperturaIvm: com.bocatta.pos.presentation.viewmodel.InventoryViewModel = koinViewModel()
                        if (sessionVm.cargandoSesion) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = BocattaPrimary)
                            }
                            return@composable
                        }
                        LaunchedEffect(sessionVm.sucursalActual) {
                            cajaVm.configurarSucursal(sessionVm.sucursalActual)
                        }
                        if (cajaVm.turnoActivo != null) {
                            LaunchedEffect(Unit) {
                                navController.navigate(Routes.Ventas) { popUpTo("apertura") { inclusive = true } }
                            }
                        }
                        AperturaDiaScreen(
                            sessionVm = sessionVm,
                            aperturaVmV2 = aperturaVmV2,
                            cajaVm = cajaVm,
                            vm = aperturaIvm,
                            onAperturaCompleta = { navController.navigate(Routes.Ventas) { popUpTo("apertura") { inclusive = true } } }
                        )
                    }

                    composable<Routes.Ventas> {
                        if (sessionVm.cargandoSesion) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = BocattaPrimary)
                            }
                            return@composable
                        }
                        SalesScreen(
                            vmV2 = salesVmV2,
                            session = sessionVm,
                            onVerInventario = { navController.navigate(Routes.Inventario) },
                            onVerReportes = { if (sessionVm.esAdmin) navController.navigate(Routes.Reportes) },
                            onVerGastos = { navController.navigate(Routes.Gastos) },
                            onVerAdmin = { if (sessionVm.esAdmin) navController.navigate(Routes.Admin) },
                            onVerCaja = { navController.navigate(Routes.Caja) },
                            onVerDevoluciones = { navController.navigate(Routes.Devoluciones) },
                            onLogout = { sessionVm.cerrarSesion(); authVm.logout() }
                        )
                    }

                    composable<Routes.Inventario> {
                        LaunchedEffect(sessionVm.sucursalActual) {
                            inventoryVm.configurarSucursal(sessionVm.sucursalActual)
                        }
                        InventoryScreen(
                            vm = inventoryVm,
                            session = sessionVm,
                            onBack = { navController.popBackStack() },
                            onCierreInventario = { navController.navigate(Routes.CierreInventario) },
                            onAperturaInventario = { navController.navigate(Routes.AperturaInventario) }
                        )
                    }

                    composable<Routes.Reportes> {
                        if (!sessionVm.esAdmin) { navController.popBackStack(); return@composable }
                        val reportVmV2: ReportViewModelV2 = koinViewModel()
                        ReportScreen(vmV2 = reportVmV2, sucursal = sessionVm.sucursalActual, onBack = { navController.popBackStack() })
                    }

                    composable<Routes.Gastos> {
                        val gastosVmV2: ExpensesViewModelV2 = koinViewModel()
                        LaunchedEffect(sessionVm.sucursalActual) { gastosVmV2.cargarGastos(sessionVm.sucursalActual) }
                        GastosScreen(vm = gastosVmV2, session = sessionVm, onBack = { navController.popBackStack() })
                    }

                    composable<Routes.Admin> {
                        if (!sessionVm.esAdmin) { navController.popBackStack(); return@composable }
                        val adminVmV2: AdminViewModel = koinViewModel()
                        LaunchedEffect(sessionVm.usuario) { adminVmV2.configurarUsuario(sessionVm.usuario) }
                        AdminScreen(
                            vm = adminVmV2,
                            inventoryVm = inventoryVm,
                            session = sessionVm,
                            onBack = { navController.popBackStack() },
                            onVerClientes = { navController.navigate(Routes.Clientes) },
                            onVerDashboardBodega = { navController.navigate(Routes.DashboardBodega) },
                            onVerReportesInventario = { navController.navigate(Routes.ReportesInventario) },
                            onVerSyncInventario = { navController.navigate(Routes.SyncInventario) },
                            onVerGestionarSucursales = { navController.navigate(Routes.GestionarSucursales) }
                        )
                    }

                    composable<Routes.Devoluciones> {
                        val devolucionVm: DevolucionViewModel = koinViewModel()
                        DevolucionesScreen(vm = devolucionVm, session = sessionVm, onBack = { navController.popBackStack() })
                    }

                    composable<Routes.Caja> {
                        CierreCajaScreen(vm = cajaVm, session = sessionVm, onBack = { navController.popBackStack() })
                    }

                    composable<Routes.Clientes> {
                        val clienteVm: ClienteViewModel = koinViewModel()
                        ClientesScreen(vm = clienteVm, onBack = { navController.popBackStack() })
                    }

                    composable<Routes.Compras> {
                        ComprasScreen(onBack = { navController.popBackStack() })
                    }

                    composable<Routes.CierreInventario> {
                        CierreInventarioScreen(vm = inventoryVm, session = sessionVm, onBack = { navController.popBackStack() })
                    }

                    composable<Routes.AperturaInventario> {
                        AperturaInventarioScreen(vm = inventoryVm, session = sessionVm, onBack = { navController.popBackStack() })
                    }

                    composable<Routes.DashboardBodega> {
                        if (!sessionVm.esAdmin) { navController.popBackStack(); return@composable }
                        DashboardBodegaScreen(
                            onBack = { navController.popBackStack() },
                            onSync = { navController.navigate(Routes.SyncInventario) },
                            onReportes = { navController.navigate(Routes.ReportesInventario) }
                        )
                    }

                    composable<Routes.ReportesInventario> {
                        if (!sessionVm.esAdmin) { navController.popBackStack(); return@composable }
                        ReportesInventarioScreen(onBack = { navController.popBackStack() })
                    }

                    composable<Routes.SyncInventario> {
                        if (!sessionVm.esAdmin) { navController.popBackStack(); return@composable }
                        SyncInventarioScreen(onBack = { navController.popBackStack() })
                    }

                    // ── GESTIÓN DE SUCURSALES (NUEVO) ─────────────────────────
                    composable<Routes.GestionarSucursales> {
                        if (!sessionVm.esAdmin) { navController.popBackStack(); return@composable }
                        val sucursalesVm: GestionSucursalesViewModel = koinViewModel()
                        GestionSucursalesScreen(
                            vm = sucursalesVm,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}



