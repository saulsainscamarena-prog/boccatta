package com.bocatta.pos.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppNavGraphTest {

    @Test
    fun `Routes sealed class contains all 19 expected members`() {
        val subclasses = Routes::class.sealedSubclasses.map { it.simpleName }.toSet()
        val expected = setOf(
            "Login", "Turnos", "Apertura", "Ventas", "Inventario",
            "Reportes", "Gastos", "Admin", "Devoluciones", "Caja",
            "Clientes", "Compras", "CierreInventario", "AperturaInventario",
            "DashboardBodega", "ReportesInventario", "SyncInventario",
            "GestionarSucursales", "Actividad"
        )
        expected.forEach { name ->
            assertTrue("Routes.$name should exist in sealed subclasses", name in subclasses)
        }
        assertEquals("Expected exactly 19 Routes members", 19, subclasses.size)
    }
}
