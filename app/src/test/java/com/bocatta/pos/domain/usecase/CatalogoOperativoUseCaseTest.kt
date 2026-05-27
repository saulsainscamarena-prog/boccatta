package com.bocatta.pos.domain.usecase

import com.bocatta.pos.domain.model.SalesInventoryProductV2
import com.bocatta.pos.domain.usecase.impl.CatalogoOperativoUseCaseImpl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogoOperativoUseCaseTest {

    private val useCase: CatalogoOperativoUseCase = CatalogoOperativoUseCaseImpl()
    private val sucursalTest = "sucursal_a"

    @Test
    fun `debe filtrar productos inactivos`() {
        val productos = listOf(
            SalesInventoryProductV2(id = "1", nombre = "Crepa Activa", activo = true, precioVenta = mapOf(sucursalTest to 50.0)),
            SalesInventoryProductV2(id = "2", nombre = "Crepa Inactiva", activo = false, precioVenta = mapOf(sucursalTest to 50.0))
        )

        val resultado = useCase.clasificarYOrdenar(productos, emptyMap(), sucursalTest)

        assertEquals(1, resultado.vendibles.size)
        assertEquals("1", resultado.vendibles.first().id)
        assertEquals(1, resultado.excluidos.size)
        assertEquals("2", resultado.excluidos.first().id)
    }

    @Test
    fun `debe filtrar productos sin precio o precio menor o igual a cero para la sucursal`() {
        val productos = listOf(
            SalesInventoryProductV2(id = "1", nombre = "Crepa Con Precio", activo = true, precioVenta = mapOf(sucursalTest to 50.0)),
            SalesInventoryProductV2(id = "2", nombre = "Crepa Sin Precio", activo = true, precioVenta = emptyMap()),
            SalesInventoryProductV2(id = "3", nombre = "Crepa Precio Cero", activo = true, precioVenta = mapOf(sucursalTest to 0.0)),
            SalesInventoryProductV2(id = "4", nombre = "Crepa Otra Sucursal", activo = true, precioVenta = mapOf("otra_sucursal" to 60.0))
        )

        val resultado = useCase.clasificarYOrdenar(productos, emptyMap(), sucursalTest)

        assertEquals(1, resultado.vendibles.size)
        assertEquals("1", resultado.vendibles.first().id)
        assertEquals(3, resultado.excluidos.size)
        assertTrue(resultado.excluidos.any { it.id == "2" })
        assertTrue(resultado.excluidos.any { it.id == "3" })
        assertTrue(resultado.excluidos.any { it.id == "4" })
    }

    @Test
    fun `debe excluir materias primas del catalogo de ventas`() {
        val productos = listOf(
            SalesInventoryProductV2(id = "1", nombre = "Crepa de Cajeta", tipoProducto = "PREPARADO", precioVenta = mapOf(sucursalTest to 50.0)),
            SalesInventoryProductV2(id = "2", nombre = "Harina de Trigo", tipoProducto = " materia_prima ", precioVenta = mapOf(sucursalTest to 20.0))
        )

        val resultado = useCase.clasificarYOrdenar(productos, emptyMap(), sucursalTest)

        assertEquals(1, resultado.vendibles.size)
        assertEquals("1", resultado.vendibles.first().id)
        assertTrue(resultado.excluidos.any { it.id == "2" })
    }

    @Test
    fun `debe excluir toppings sueltos`() {
        val productos = listOf(
            SalesInventoryProductV2(id = "1", nombre = "Waffle Clasico", esProductoTopping = false, precioVenta = mapOf(sucursalTest to 60.0)),
            SalesInventoryProductV2(id = "2", nombre = "Nutella Extra", esProductoTopping = true, precioVenta = mapOf(sucursalTest to 15.0))
        )

        val resultado = useCase.clasificarYOrdenar(productos, emptyMap(), sucursalTest)

        assertEquals(1, resultado.vendibles.size)
        assertEquals("1", resultado.vendibles.first().id)
        assertTrue(resultado.excluidos.any { it.id == "2" })
    }

    @Test
    fun `debe ordenar por historial y alfabeticamente mostrando todos los vendibles`() {
        val productos = listOf(
            SalesInventoryProductV2(id = "A", nombre = "Crepa A", precioVenta = mapOf(sucursalTest to 50.0)),
            SalesInventoryProductV2(id = "B", nombre = "Crepa B", precioVenta = mapOf(sucursalTest to 55.0)),
            SalesInventoryProductV2(id = "C", nombre = "Crepa C", precioVenta = mapOf(sucursalTest to 60.0)),
            SalesInventoryProductV2(id = "D", nombre = "Crepa D", precioVenta = mapOf(sucursalTest to 65.0))
        )

        val historial = mapOf(
            "A" to 10,
            "C" to 25
        )

        val resultado = useCase.clasificarYOrdenar(productos, historial, sucursalTest)

        // El orden esperado de frecuentes debe ser:
        // 1. C (25 ventas)
        // 2. A (10 ventas)
        // 3. B y D ordenados alfabÃ©ticamente (0 ventas cada uno) -> B, luego D.
        assertEquals(4, resultado.frecuentes.size)
        assertEquals("C", resultado.frecuentes[0].id)
        assertEquals("A", resultado.frecuentes[1].id)
        assertEquals("B", resultado.frecuentes[2].id)
        assertEquals("D", resultado.frecuentes[3].id)
    }

    @Test
    fun `debe ordenar todos alfabeticamente si el historial viene vacio`() {
        val productos = listOf(
            SalesInventoryProductV2(id = "C", nombre = "Crepa C", precioVenta = mapOf(sucursalTest to 60.0)),
            SalesInventoryProductV2(id = "A", nombre = "Crepa A", precioVenta = mapOf(sucursalTest to 50.0)),
            SalesInventoryProductV2(id = "B", nombre = "Crepa B", precioVenta = mapOf(sucursalTest to 55.0))
        )

        val resultado = useCase.clasificarYOrdenar(productos, emptyMap(), sucursalTest)

        assertEquals(3, resultado.frecuentes.size)
        assertEquals("A", resultado.frecuentes[0].id)
        assertEquals("B", resultado.frecuentes[1].id)
        assertEquals("C", resultado.frecuentes[2].id)
    }
}
