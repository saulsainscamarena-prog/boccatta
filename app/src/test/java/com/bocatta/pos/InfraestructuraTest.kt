package com.bocatta.pos

import com.bocatta.pos.core.constants.SucursalConfig
import com.bocatta.pos.core.TicketUtils
import com.bocatta.pos.domain.model.ItemVendidoV2
import org.junit.Test
import org.junit.Assert.*

/**
 * Tests unitarios para modelos de dominio y utilidades centrales.
 *
 * Cubre: SucursalConfig, TicketUtils, e ItemVendidoV2.
 */
class SucursalConfigTest {

    @Test
    fun prefijoPorSucursal_retornaATL_paraAtlixco() {
        assertEquals("ATL", SucursalConfig.prefijoPorSucursal("atlixco"))
    }

    @Test
    fun prefijoPorSucursal_retornaMT_paraMetepec() {
        assertEquals("MT", SucursalConfig.prefijoPorSucursal("metepec"))
    }

    @Test
    fun prefijoPorSucursal_esCaseInsensitive() {
        assertEquals("ATL", SucursalConfig.prefijoPorSucursal("ATLIXCO"))
        assertEquals("MT", SucursalConfig.prefijoPorSucursal("Metepec"))
    }

    @Test
    fun prefijoPorSucursal_retornaBC_paraSucursalDesconocida() {
        assertEquals("BC", SucursalConfig.prefijoPorSucursal("unknown_city"))
        assertEquals("BC", SucursalConfig.prefijoPorSucursal(""))
    }

    @Test
    fun extraerInsumoIdDeDocId_eliminaPrefijoDeSucursal() {
        assertEquals("crema_batida", SucursalConfig.extraerInsumoIdDeDocId("atlixco_crema_batida"))
        assertEquals("azucar_glass", SucursalConfig.extraerInsumoIdDeDocId("metepec_azucar_glass"))
    }

    @Test
    fun extraerInsumoIdDeDocId_retornaOriginal_sinPrefijo() {
        assertEquals("insumo_sin_sucursal", SucursalConfig.extraerInsumoIdDeDocId("insumo_sin_sucursal"))
    }

    @Test
    fun buildDocId_construyeFormatoCorrectamente() {
        assertEquals("atlixco_crema_batida", SucursalConfig.buildDocId("atlixco", "crema_batida"))
        assertEquals("metepec_azucar", SucursalConfig.buildDocId("METEPEC", "azucar"))
    }
}

class TicketUtilsTest {

    @Test
    fun generarCodigoTicket_usaPrefijoCorrecto_paraAtlixco() {
        val codigo = TicketUtils.generarCodigoTicket("atlixco", 1L)
        assertTrue("Debe empezar con ATL", codigo.startsWith("ATL"))
    }

    @Test
    fun generarCodigoTicket_usaPrefijoCorrecto_paraMetepec() {
        val codigo = TicketUtils.generarCodigoTicket("metepec", 1L)
        assertTrue("Debe empezar con MT", codigo.startsWith("MT"))
    }

    @Test
    fun generarCodigoTicket_paddea3Digitos() {
        val codigo = TicketUtils.generarCodigoTicket("atlixco", 5L)
        assertTrue("El número debe tener 3 dígitos con padding", codigo.endsWith("-005"))
    }

    @Test
    fun generarCodigoTicket_manejaNumeroGrande() {
        val codigo = TicketUtils.generarCodigoTicket("atlixco", 1000L)
        assertTrue("Número grande no debe romperse", codigo.endsWith("-1000"))
    }
}

class ItemVendidoV2Test {

    @Test
    fun subtotal_calculaCorrectamentePrecioXCantidad() {
        val item = ItemVendidoV2(
            productoId = "test",
            nombre = "Carlota",
            cantidad = 3,
            precioUnitario = 75.0
        )

        assertEquals(225.0, item.subtotal, 0.01)
    }

    @Test
    fun subtotal_esZero_cuandoPrecioEsZero() {
        val item = ItemVendidoV2(productoId = "gratis", cantidad = 5, precioUnitario = 0.0)
        assertEquals(0.0, item.subtotal, 0.01)
    }

    @Test
    fun deducciones_defaultEsMapaVacio() {
        val item = ItemVendidoV2(productoId = "simple")
        assertTrue("Deducciones deben ser vacías por defecto", item.deducciones.isEmpty())
    }

    @Test
    fun toppings_defaultEsListaVacia() {
        val item = ItemVendidoV2(productoId = "simple")
        assertTrue("Toppings deben ser vacíos por defecto", item.toppings.isEmpty())
    }

    @Test
    fun campos_opcionales_aceptanNull() {
        val item = ItemVendidoV2(
            productoId = "sin-extras",
            base = null,
            aderezo = null,
            recetaId = null
        )
        assertNull(item.base)
        assertNull(item.aderezo)
        assertNull(item.recetaId)
    }
}
