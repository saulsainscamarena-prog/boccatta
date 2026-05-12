package com.bocatta.pos.domain

import org.junit.Test
import org.junit.Assert.*
import com.bocatta.pos.domain.model.Rol

class SessionLogicTest {

    @Test
    fun adminEsAdmin() {
        assertTrue(esAdminPorRol(Rol.ADMIN))
    }

    @Test
    fun duenoEsAdmin() {
        assertTrue(esAdminPorRol(Rol.DUEÑO))
    }

    @Test
    fun vendedorNoEsAdmin() {
        assertFalse(esAdminPorRol(Rol.VENDEDOR))
    }

    @Test
    fun soloEsDueno() {
        assertFalse(esDuenoPorRol(Rol.ADMIN))
        assertTrue(esDuenoPorRol(Rol.DUEÑO))
    }

    private fun esAdminPorRol(rol: Rol) = rol == Rol.ADMIN || rol == Rol.DUEÑO

    private fun esDuenoPorRol(rol: Rol) = rol == Rol.DUEÑO
}