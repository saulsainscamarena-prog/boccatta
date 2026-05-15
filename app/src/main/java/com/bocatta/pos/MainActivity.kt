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
        com.bocatta.pos.data.FirestoreSeeder.seedV2Collections()

        com.bocatta.pos.data.sync.SyncScheduler.schedule(this)

        setContent {
            val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
            val useDark = sessionVm.modoOscuroManual ?: systemDark

            BocattaTheme(darkTheme = useDark, dynamicColor = true) {
                val navController = rememberNavController()

                LaunchedEffect(authVm.estaLogueado) {
                    if (!authVm.estaLogueado) {
                        navController.navigate("login") { popUpTo(0) { inclusive = true } }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = if (authVm.estaLogueado) "turnos" else "login"
                ) {
                    composable("login") {
                        LoginScreen(vm = authVm, onLoginExitoso = { uid ->
                            sessionVm.cargarUsuario(uid)
                            navController.navigate("turnos") { popUpTo("login") { inclusive = true } }
                        })
                    }

                    composable("turnos") {
                        val ivmLocal: InventoryViewModel = koinViewModel()
                        val turnoIvm: InventoryViewModel = koinViewModel()
                        LaunchedEffect(sessionVm.sucursalActual) {
                            cajaVm.configurarSucursal(sessionVm.sucursalActual)
                            ivmLocal.configurarSucursal(sessionVm.sucursalActual)
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
                            onIniciarTurno = { navController.navigate("apertura") { popUpTo("turnos") { inclusive = true } } },
                            onUnirseTurno = { navController.navigate("ventas") { popUpTo("turnos") { inclusive = true } } },
                            onAdministrarTienda = { if (sessionVm.esAdmin) navController.navigate("admin") { popUpTo("turnos") { inclusive = true } } },
                            onLogout = { sessionVm.cerrarSesion(); authVm.logout() }
                        )
                    }

                    composable("apertura") {
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
                                navController.navigate("ventas") { popUpTo("apertura") { inclusive = true } }
                            }
                        }
                        AperturaDiaScreen(
                            sessionVm = sessionVm,
                            aperturaVmV2 = aperturaVmV2,
                            cajaVm = cajaVm,
                            vm = aperturaIvm,
                            onAperturaCompleta = { navController.navigate("ventas") { popUpTo("apertura") { inclusive = true } } }
                        )
                    }

                    composable("ventas") {
                        if (sessionVm.cargandoSesion) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = BocattaPrimary)
                            }
                            return@composable
                        }
                        SalesScreen(
                            vmV2 = salesVmV2,
                            session = sessionVm,
                            onVerInventario = { navController.navigate("inventario") },
                            onVerReportes = { if (sessionVm.esAdmin) navController.navigate("reportes") },
                            onVerGastos = { navController.navigate("gastos") },
                            onVerAdmin = { if (sessionVm.esAdmin) navController.navigate("admin") },
                            onVerCaja = { navController.navigate("caja") },
                            onVerDevoluciones = { navController.navigate("devoluciones") },
                            onLogout = { sessionVm.cerrarSesion(); authVm.logout() }
                        )
                    }

                    composable("inventario") {
                        LaunchedEffect(sessionVm.sucursalActual) {
                            inventoryVm.configurarSucursal(sessionVm.sucursalActual)
                        }
                        InventoryScreen(
                            vm = inventoryVm,
                            session = sessionVm,
                            onBack = { navController.popBackStack() },
                            onCierreInventario = { navController.navigate("cierre_inventario") },
                            onAperturaInventario = { navController.navigate("apertura_inventario") }
                        )
                    }

                    composable("reportes") {
                        if (!sessionVm.esAdmin) { navController.popBackStack(); return@composable }
                        val reportVmV2: ReportViewModelV2 = koinViewModel()
                        ReportScreen(vmV2 = reportVmV2, sucursal = sessionVm.sucursalActual, onBack = { navController.popBackStack() })
                    }

                    composable("gastos") {
                        val gastosVmV2: ExpensesViewModelV2 = koinViewModel()
                        LaunchedEffect(sessionVm.sucursalActual) { gastosVmV2.cargarGastos(sessionVm.sucursalActual) }
                        GastosScreen(vm = gastosVmV2, session = sessionVm, onBack = { navController.popBackStack() })
                    }

                    composable("admin") {
                        if (!sessionVm.esAdmin) { navController.popBackStack(); return@composable }
                        val adminVmV2: AdminViewModel = koinViewModel()
                        LaunchedEffect(sessionVm.usuario) { adminVmV2.configurarUsuario(sessionVm.usuario) }
                        AdminScreen(
                            vm = adminVmV2,
                            inventoryVm = inventoryVm,
                            session = sessionVm,
                            onBack = { navController.popBackStack() },
                            onVerClientes = { navController.navigate("clientes") },
                            onVerDashboardBodega = { navController.navigate("dashboard_bodega") },
                            onVerReportesInventario = { navController.navigate("reportes_inventario") },
                            onVerSyncInventario = { navController.navigate("sync_inventario") },
                            onVerGestionarSucursales = { navController.navigate("gestionar_sucursales") }
                        )
                    }

                    composable("devoluciones") {
                        val devolucionVm: DevolucionViewModel = koinViewModel()
                        DevolucionesScreen(vm = devolucionVm, session = sessionVm, onBack = { navController.popBackStack() })
                    }

                    composable("caja") {
                        CierreCajaScreen(vm = cajaVm, session = sessionVm, onBack = { navController.popBackStack() })
                    }

                    composable("clientes") {
                        val clienteVm: ClienteViewModel = koinViewModel()
                        ClientesScreen(vm = clienteVm, onBack = { navController.popBackStack() })
                    }

                    composable("compras") {
                        ComprasScreen(onBack = { navController.popBackStack() })
                    }

                    composable("cierre_inventario") {
                        CierreInventarioScreen(vm = inventoryVm, session = sessionVm, onBack = { navController.popBackStack() })
                    }

                    composable("apertura_inventario") {
                        AperturaInventarioScreen(vm = inventoryVm, session = sessionVm, onBack = { navController.popBackStack() })
                    }

                    composable("dashboard_bodega") {
                        if (!sessionVm.esAdmin) { navController.popBackStack(); return@composable }
                        DashboardBodegaScreen(
                            onBack = { navController.popBackStack() },
                            onSync = { navController.navigate("sync_inventario") },
                            onReportes = { navController.navigate("reportes_inventario") }
                        )
                    }

                    composable("reportes_inventario") {
                        if (!sessionVm.esAdmin) { navController.popBackStack(); return@composable }
                        ReportesInventarioScreen(onBack = { navController.popBackStack() })
                    }

                    composable("sync_inventario") {
                        if (!sessionVm.esAdmin) { navController.popBackStack(); return@composable }
                        SyncInventarioScreen(onBack = { navController.popBackStack() })
                    }

                    // ── GESTIÓN DE SUCURSALES (NUEVO) ─────────────────────────
                    composable("gestionar_sucursales") {
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
