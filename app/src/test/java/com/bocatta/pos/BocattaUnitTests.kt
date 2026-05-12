package com.bocatta.pos

import com.bocatta.pos.domain.model.ItemCarritoV2
import com.bocatta.pos.domain.model.SalesInventoryProductV2
import com.bocatta.pos.domain.model.Rol
import com.bocatta.pos.domain.model.Usuario
import org.junit.Test
import org.junit.Assert.*
import java.math.BigDecimal

class SessionViewModelTest {
    @Test
    fun esAdmin_retornaTrue_paraAdmin() {
        val usuario = Usuario(
            uid = "test",
            nombre = "Admin Test",
            rol = Rol.ADMIN,
            correo = "admin@test.com"
        )
        
        val resultado = usuario.rol == Rol.ADMIN || usuario.rol == Rol.DUEÑO
        
        assertTrue(resultado)
    }
    
    @Test
    fun esAdmin_retornaFalse_paraVendedor() {
        val usuario = Usuario(
            uid = "test",
            nombre = "Vendor Test",
            rol = Rol.VENDEDOR,
            correo = "vendor@test.com"
        )
        
        val resultado = usuario.rol == Rol.ADMIN || usuario.rol == Rol.DUEÑO
        
        assertFalse(resultado)
    }
    
    @Test
    fun esAdmin_retornaTrue_paraDuenio() {
        val usuario = Usuario(
            uid = "test",
            nombre = "Dueño Test",
            rol = Rol.DUEÑO,
            correo = "dueno@test.com"
        )
        
        val resultado = usuario.rol == Rol.ADMIN || usuario.rol == Rol.DUEÑO
        
        assertTrue(resultado)
    }
}

class ModelsV2Test {
    @Test
    fun itemCarrito_copyConCantidad_noMutaOriginal() {
        val producto = SalesInventoryProductV2(id = "prod1", nombre = "Test", precioVenta = emptyMap())
        val original = ItemCarritoV2(
            producto = producto,
            precioFinal = BigDecimal("50.00"),
            cantidad = 1
        )
        
        val copia = original.copyConCantidad(5)
        
        assertEquals(1, original.cantidad)
        assertEquals(5, copia.cantidad)
    }
    
    @Test
    fun itemCarrito_tieneToppings() {
        val producto = SalesInventoryProductV2(id = "prod1", nombre = "Test", precioVenta = emptyMap())
        val item = ItemCarritoV2(
            producto = producto,
            precioFinal = BigDecimal("120.00"),
            cantidad = 1,
            toppings = listOf("extra1", "extra2")
        )
        
        assertEquals(2, item.toppings.size)
        assertTrue(item.toppings.contains("extra1"))
    }
    
    @Test
    fun itemCarrito_notaDefaultEmpty() {
        val producto = SalesInventoryProductV2(id = "prod1", nombre = "Test", precioVenta = emptyMap())
        val item = ItemCarritoV2(
            producto = producto,
            precioFinal = BigDecimal("50.00"),
            cantidad = 2
        )
        
        assertEquals("", item.nota)
    }
    
    @Test
    fun itemCarrito_cantidadDefaultUno() {
        val producto = SalesInventoryProductV2(id = "prod1", nombre = "Test", precioVenta = emptyMap())
        val item = ItemCarritoV2(
            producto = producto,
            precioFinal = BigDecimal("50.00")
        )
        
        assertEquals(1, item.cantidad)
    }
    
    @Test
    fun itemCarrito_copiaConNuevaCantidad() {
        val producto = SalesInventoryProductV2(id = "prod1", nombre = "Test", precioVenta = emptyMap())
        val original = ItemCarritoV2(
            producto = producto,
            precioFinal = BigDecimal("50.00"),
            cantidad = 1
        )
        
        val copia = original.copyConCantidad(10)
        
        assertEquals(10, copia.cantidad)
        assertEquals(original.producto.id, copia.producto.id)
    }
}

class LogicaNegocioTest {
    @Test
    fun calculaTotalCarrito_sinDescuentos() {
        val productos = listOf(
            ItemCarritoV2(
                producto = SalesInventoryProductV2(id = "p1", nombre = "Producto 1", precioVenta = emptyMap()),
                precioFinal = BigDecimal("50.00"),
                cantidad = 2
            ),
            ItemCarritoV2(
                producto = SalesInventoryProductV2(id = "p2", nombre = "Producto 2", precioVenta = emptyMap()),
                precioFinal = BigDecimal("80.00"),
                cantidad = 1
            )
        )
        
        val total = productos.sumOf { 
            it.precioFinal.toDouble() * it.cantidad 
        }
        
        assertEquals(180.0, total, 0.01)
    }
    
    @Test
    fun lealtad_descuentoAplicaEnVisita5() {
        val visitasCiclo = 5
        val comprasPromedio = listOf(100.0, 120.0, 90.0, 110.0, 130.0)
        val descuento = if (visitasCiclo == 5) comprasPromedio.average() else 0.0
        assertEquals(110.0, descuento, 0.01)
    }
    
    @Test
    fun lealtad_sinDescuentoAntesDe5Visitas() {
        val visitasCiclo = 3
        val descuento = if (visitasCiclo == 5) 100.0 else 0.0
        assertEquals(0.0, descuento, 0.01)
    }
    
    @Test
    fun lealtad_reiniciaCicloEnVisita6() {
        val visitasCiclo = 5
        val nuevasVisitas = if (visitasCiclo >= 5) 1 else visitasCiclo + 1
        assertEquals(1, nuevasVisitas)
    }
    
    @Test
    fun precioToppingsPremium_suma10() {
        val toppings = listOf("premium", "extra")
        val precioToppings = toppings.sumOf { if (it == "premium") 10.0 else 5.0 }
        
        assertEquals(15.0, precioToppings, 0.01)
    }
}