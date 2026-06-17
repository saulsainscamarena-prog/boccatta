package com.bocatta.pos.core.model.navigation

import kotlinx.serialization.Serializable

@Serializable sealed class Routes {
    @Serializable data object Login : Routes()
    @Serializable data object SetNip : Routes()
    @Serializable data object Turnos : Routes()
    @Serializable data object Apertura : Routes()
    @Serializable data object Ventas : Routes()
    @Serializable data object Inventario : Routes()
    @Serializable data object Reportes : Routes()
    @Serializable data object Gastos : Routes()
    @Serializable data object Admin : Routes()
    @Serializable data object Devoluciones : Routes()
    @Serializable data object Caja : Routes()
    @Serializable data object Clientes : Routes()
    @Serializable data object Compras : Routes()
    @Serializable data object CierreInventario : Routes()
    @Serializable data object AperturaInventario : Routes()
    @Serializable data object DashboardBodega : Routes()
    @Serializable data object ReportesInventario : Routes()
    @Serializable data object SyncInventario : Routes()
    @Serializable data object GestionarSucursales : Routes()
    @Serializable data object Actividad : Routes()
    @Serializable data object Kds : Routes()
}
