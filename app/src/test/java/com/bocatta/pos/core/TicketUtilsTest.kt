package com.bocatta.pos.core

import org.junit.Test
import org.junit.Assert.*

class TicketUtilsTest {

    @Test
    fun generarCodigoTicket_atlixco() {
        val result = TicketUtils.generarCodigoTicket("atlixco", 1)
        assertTrue("Debe empezar con ATL", result.startsWith("ATL"))
    }

    @Test
    fun generarCodigoTicket_metepec() {
        val result = TicketUtils.generarCodigoTicket("metepec", 1)
        assertTrue("Debe empezar con MT", result.startsWith("MT"))
    }

    @Test
    fun generarCodigoTicket_numero5() {
        val result = TicketUtils.generarCodigoTicket("atlixco", 5)
        assertTrue("Debe terminar con 005", result.endsWith("005"))
    }

    @Test
    fun generarCodigoTicket_numero100() {
        val result = TicketUtils.generarCodigoTicket("atlixco", 100)
        assertTrue("Debe terminar con 100", result.endsWith("100"))
    }
}