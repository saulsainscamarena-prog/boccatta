package com.bocatta.pos.presentation.ui.screens.admin

import androidx.compose.runtime.Composable
import com.bocatta.pos.presentation.viewmodel.AdminViewModel
import com.bocatta.pos.presentation.viewmodel.InventoryViewModel
import com.bocatta.pos.presentation.viewmodel.SessionViewModel

sealed class AdminSection {
    data object Dashboard : AdminSection()
    data object Menu : AdminSection()
    data object Recetas : AdminSection()
    data object Inventario : AdminSection()
    data object Empleados : AdminSection()
    data object Reportes : AdminSection()
    data object Config : AdminSection()
}

@Composable
fun AdminContent(
    section: AdminSection,
    vm: AdminViewModel,
    inventoryVm: InventoryViewModel,
    session: SessionViewModel,
    subTabSeleccionado: Int,
    onSubTabSelect: (Int) -> Unit,
    onNavigateToDashboard: () -> Unit,
    onNavigateToSync: () -> Unit,
    onNavigateToClientes: () -> Unit,
    onNavigateToGestionSucursales: () -> Unit,
    onNavigateToReportesInventario: () -> Unit,
    onComenzarConfiguracion: () -> Unit,
    onValidarPin: (String, (Boolean) -> Unit) -> Unit
) {
    when (section) {
        AdminSection.Dashboard -> TabDashboard(
            vm = vm,
            onComenzarConfiguracion = onComenzarConfiguracion,
            onValidarPin = onValidarPin
        )
        AdminSection.Menu -> TabMenu(vm)
        AdminSection.Recetas -> {
            // TODO: implement Recetas sub‑tabs inside AdminScreen
        }
        AdminSection.Inventario -> TabBodegaGeneral(
            vm = vm,
            onVerDashboardBodega = onNavigateToDashboard,
            onVerGestionarSucursales = onNavigateToGestionSucursales,
            onVerReportesInventario = onNavigateToReportesInventario,
            onVerSyncInventario = onNavigateToSync
        )
        AdminSection.Empleados -> {
            // TODO: implement Empleados sub‑tabs inside AdminScreen
        }
        AdminSection.Reportes -> {
            // TODO: implement Reportes sub‑tabs inside AdminScreen
        }
        AdminSection.Config -> {
            // TODO: implement Config sub‑tabs inside AdminScreen
        }
    }
}
