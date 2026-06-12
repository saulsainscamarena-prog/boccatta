package com.bocatta.pos.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import com.bocatta.pos.feature.auth.viewmodel.*
import com.bocatta.pos.feature.ventas.viewmodel.*
import com.bocatta.pos.feature.admin.viewmodel.*
import com.bocatta.pos.feature.inventario.viewmodel.*
import com.bocatta.pos.core.ui.viewmodel.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.bocatta.pos.data.repository.RegistroJornadaRepository
import com.bocatta.pos.domain.model.RegistroJornada
import com.bocatta.pos.feature.admin.ui.screens.admin.AdminScreen
import com.bocatta.pos.feature.admin.ui.screens.admin.GestionSucursalesScreen
import com.bocatta.pos.feature.inventario.ui.screens.bodega.DashboardBodegaScreen
import com.bocatta.pos.feature.admin.ui.screens.caja.CierreCajaScreen
import com.bocatta.pos.feature.ventas.ui.screens.clientes.ClientesScreen
import com.bocatta.pos.feature.inventario.ui.screens.compras.ComprasScreen
import com.bocatta.pos.feature.ventas.ui.screens.devoluciones.DevolucionesScreen
import com.bocatta.pos.feature.admin.ui.screens.gastos.GastosScreen
import com.bocatta.pos.feature.inventario.ui.screens.inventario.AperturaInventarioScreen
import com.bocatta.pos.feature.inventario.ui.screens.inventario.CierreInventarioScreen
import com.bocatta.pos.feature.inventario.ui.screens.inventario.InventoryScreen
import com.bocatta.pos.feature.inventario.ui.screens.inventario.SyncInventarioScreen
import com.bocatta.pos.feature.auth.ui.screens.login.LoginScreen
import com.bocatta.pos.feature.ventas.ui.screens.operacion.AperturaDiaScreen
import com.bocatta.pos.feature.ventas.ui.screens.operacion.TurnosScreen
import com.bocatta.pos.feature.admin.ui.screens.reportes.ReportScreen
import com.bocatta.pos.feature.inventario.ui.screens.reportes.ReportesInventarioScreen
import com.bocatta.pos.feature.ventas.ui.screens.ventas.SalesScreen
import com.bocatta.pos.feature.auth.viewmodel.SessionViewModel
import com.bocatta.pos.feature.ventas.viewmodel.CajaViewModel
import com.bocatta.pos.feature.admin.viewmodel.AdminViewModel
import com.bocatta.pos.feature.admin.viewmodel.ReportViewModelV2
import com.bocatta.pos.feature.admin.viewmodel.ExpensesViewModelV2
import com.bocatta.pos.feature.admin.viewmodel.GestionSucursalesViewModel
import com.bocatta.pos.feature.ventas.viewmodel.DevolucionViewModel
import com.bocatta.pos.feature.ventas.viewmodel.ClienteViewModel
import com.bocatta.pos.feature.ventas.ui.components.DialogCompraUnificado
import com.bocatta.pos.presentation.viewmodel.*
import com.bocatta.pos.presentation.ui.theme.BocattaLightColorScheme
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun AppNavGraph(
    navController: NavHostController,
    authVm: AuthViewModelV2,
    sessionVm: SessionViewModel,
    cajaVm: CajaViewModel,
    inventoryVm: InventoryViewModel,
    salesVmV2: SalesViewModelV2,
    heldOrderVm: HeldOrderViewModel,
    mesaVm: MesaViewModel
) {
    NavHost(
        navController = navController,
        startDestination = if (authVm.estaLogueado) Routes.Turnos else Routes.Login
    ) {
        composable<Routes.Login> {
            LoginScreen(vm = authVm, onLoginExitoso = { uid ->
                sessionVm.cargarUsuario(uid)
                navController.navigate(Routes.Turnos) { popUpTo<Routes.Login> { inclusive = true } }
            })
        }

        composable<Routes.Turnos> {
            val horarioVm: HorarioViewModel = koinViewModel()
            val registroRepo = remember { RegistroJornadaRepository() }
            val scope = rememberCoroutineScope()
            fun registrarJornada(accion: String) {
                scope.launch {
                    registroRepo.guardar(
                        RegistroJornada(
                            usuario = sessionVm.nombreUsuario,
                            sucursal = normalizarSucursalNav(sessionVm.sucursalActual),
                            accion = accion,
                            rol = sessionVm.rol.name,
                            sesionId = sessionVm.uid,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
            }
            LaunchedEffect(sessionVm.sucursalActual, sessionVm.uid) {
                cajaVm.configurarSucursal(sessionVm.sucursalActual)
                inventoryVm.configurarSucursal(sessionVm.sucursalActual)
                if (sessionVm.uid.isNotBlank()) horarioVm.cargarJornadaActiva(sessionVm.uid)
            }
            val participList = remember { mutableStateListOf<RegistroJornada>() }
            LaunchedEffect(sessionVm.sucursalActual) {
                participList.clear()
                participList.addAll(registroRepo.getParticipantes(normalizarSucursalNav(sessionVm.sucursalActual)))
            }
            TurnosScreen(
                sessionVm = sessionVm,
                cajaVm = cajaVm,
                inventarioVm = inventoryVm,
                participantes = participList,
                jornadaActiva = horarioVm.jornadaActiva != null,
                onIniciarTurno = { navController.navigate(Routes.Apertura) { popUpTo<Routes.Turnos> { inclusive = true } } },
                onUnirseTurno = {
                    registrarJornada("unirse_turno")
                    navController.navigate(Routes.Ventas) { popUpTo<Routes.Turnos> { inclusive = true } }
                },
                onAdministrarTienda = { if (sessionVm.esAdmin) navController.navigate(Routes.Admin) { popUpTo<Routes.Turnos> { inclusive = true } } },
                onLogout = { sessionVm.cerrarSesion(); authVm.logout() }
            )
        }

        composable<Routes.Apertura> {
            val aperturaVmV2: AperturaViewModelV2 = koinViewModel()
            val registroRepo = remember { RegistroJornadaRepository() }
            val scope = rememberCoroutineScope()
            if (sessionVm.cargandoSesion) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BocattaLightColorScheme.primary)
                }
                return@composable
            }
            LaunchedEffect(sessionVm.sucursalActual) {
                inventoryVm.configurarSucursal(sessionVm.sucursalActual)
            }
            AperturaDiaScreen(
                sessionVm = sessionVm,
                aperturaVmV2 = aperturaVmV2,
                cajaVm = cajaVm,
                vm = inventoryVm,
                onAperturaCompleta = {
                    scope.launch {
                        registroRepo.guardar(
                            RegistroJornada(
                                usuario = sessionVm.nombreUsuario,
                                sucursal = normalizarSucursalNav(sessionVm.sucursalActual),
                                accion = "inicio_turno",
                                rol = sessionVm.rol.name,
                                sesionId = sessionVm.uid,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    }
                    navController.navigate(Routes.Ventas) {
                        popUpTo<Routes.Apertura> { inclusive = true }
                    }
                }
            )
        }

        composable<Routes.Ventas> {
            if (sessionVm.cargandoSesion) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BocattaLightColorScheme.primary)
                }
                return@composable
            }
            LaunchedEffect(sessionVm.sucursalActual, sessionVm.nombreUsuario, sessionVm.rol) {
                cajaVm.configurarSucursal(sessionVm.sucursalActual)
                inventoryVm.configurarSucursal(sessionVm.sucursalActual)
                salesVmV2.configurar(sessionVm.sucursalActual, sessionVm.nombreUsuario, sessionVm.rol)
            }
            SalesScreen(
                vmV2 = salesVmV2,
                cajaVm = cajaVm,
                heldOrderVm = heldOrderVm,
                session = sessionVm,
                onVerInventario = { navController.navigate(Routes.Inventario) },
                onVerReportes = { if (sessionVm.esAdmin) navController.navigate(Routes.Reportes) },
                onVerGastos = { navController.navigate(Routes.Gastos) },
                onVerAdmin = { if (sessionVm.esAdmin) navController.navigate(Routes.Admin) },
                onVerCaja = { navController.navigate(Routes.Caja) },
                onVerDevoluciones = { navController.navigate(Routes.Devoluciones) },
                onVerActividad = { navController.navigate(Routes.Actividad) },
                onLogout = { sessionVm.cerrarSesion(); authVm.logout() },
                compraRapidaContent = { onDismiss ->
                    val adminVmCompra: AdminViewModel = koinViewModel()
                    DialogCompraUnificado(
                        insumos = adminVmCompra.insumosMaestros,
                        nombreUsuario = sessionVm.nombreUsuario,
                        usuarioId = sessionVm.uid,
                        sucursal = sessionVm.sucursalActual,
                        esAdmin = sessionVm.esAdmin,
                        onConfirmar = { insumoId, insumoNombre, presentacion, cant, cont, precio ->
                            adminVmCompra.registrarCompraRapida(insumoId, insumoNombre, presentacion, cant, cont, precio, sessionVm.uid, sessionVm.nombreUsuario, sessionVm.sucursalActual, sessionVm.esAdmin)
                            onDismiss()
                        },
                        onDismiss = onDismiss
                    )
                }
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
            val reportVmV2: ReportViewModelV2 = koinViewModel()
            ReportScreen(vmV2 = reportVmV2, sucursal = sessionVm.sucursalActual, onBack = { navController.popBackStack() })
        }

        composable<Routes.Gastos> {
            val gastosVmV2: ExpensesViewModelV2 = koinViewModel()
            LaunchedEffect(sessionVm.sucursalActual) { gastosVmV2.cargarGastos(sessionVm.sucursalActual) }
            GastosScreen(vm = gastosVmV2, session = sessionVm, onBack = { navController.popBackStack() })
        }

        composable<Routes.Admin> {
            val adminVmV2: AdminViewModel = koinViewModel()
            LaunchedEffect(sessionVm.usuario, sessionVm.sucursalActual) {
                adminVmV2.configurarUsuario(sessionVm.usuario)
                inventoryVm.configurarSucursal(sessionVm.sucursalActual)
            }
            AdminScreen(
                vm = adminVmV2,
                inventoryVm = inventoryVm,
                session = sessionVm,
                onBack = { navController.popBackStack() },
                onVerClientes = { navController.navigate(Routes.Clientes) },
                onVerDashboardBodega = { if (sessionVm.esAdmin) navController.navigate(Routes.DashboardBodega) },
                onVerReportesInventario = { if (sessionVm.esAdmin) navController.navigate(Routes.ReportesInventario) },
                onVerSyncInventario = { if (sessionVm.esAdmin) navController.navigate(Routes.SyncInventario) },
                onVerGestionarSucursales = { if (sessionVm.esAdmin) navController.navigate(Routes.GestionarSucursales) }
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
            DashboardBodegaScreen(
                onBack = { navController.popBackStack() },
                onSync = { navController.navigate(Routes.SyncInventario) },
                onReportes = { navController.navigate(Routes.ReportesInventario) }
            )
        }

        composable<Routes.ReportesInventario> {
            ReportesInventarioScreen(onBack = { navController.popBackStack() })
        }

        composable<Routes.SyncInventario> {
            SyncInventarioScreen(onBack = { navController.popBackStack() })
        }

        composable<Routes.GestionarSucursales> {
            val sucursalesVm: GestionSucursalesViewModel = koinViewModel()
            GestionSucursalesScreen(
                vm = sucursalesVm,
                onBack = { navController.popBackStack() }
            )
        }

        composable<Routes.Actividad> {
            com.bocatta.pos.feature.ventas.ui.screens.ventas.ActividadScreen(
                mesaVm = mesaVm,
                heldOrderVm = heldOrderVm,
                salesVm = salesVmV2,
                sessionVm = sessionVm,
                onBack = { navController.popBackStack() },
                onNavigateToSales = {
                    navController.navigate(Routes.Ventas) {
                        popUpTo<Routes.Actividad> { inclusive = true }
                    }
                }
            )
        }
    }
}

private fun normalizarSucursalNav(sucursal: String): String =
    sucursal.trim().lowercase(java.util.Locale.ROOT).replace(" ", "_")


