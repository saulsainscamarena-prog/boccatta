package com.bocatta.pos.domain.repository

import com.bocatta.pos.domain.model.ConfiguracionSalarial
import com.bocatta.pos.domain.model.RegistroPago

interface ISalarioRepository {
    suspend fun getConfiguracion(empleadoId: String): ConfiguracionSalarial?
    suspend fun guardarConfiguracion(config: ConfiguracionSalarial): Boolean
    suspend fun getPagos(empleadoId: String): List<RegistroPago>
    suspend fun getPagosDelPeriodo(inicio: Long, fin: Long): List<RegistroPago>
    suspend fun registrarPago(pago: RegistroPago): Boolean
    suspend fun marcarPagado(id: String, fecha: Long): Boolean
}
