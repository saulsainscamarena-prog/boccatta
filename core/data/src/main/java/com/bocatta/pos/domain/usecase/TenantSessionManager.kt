package com.bocatta.pos.domain.usecase

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Mantiene la sesión del Tenant (negocio) actual en memoria.
 * Para la fase 1, el default es "tenant_pionero".
 */
class TenantSessionManager {
    
    private val _currentTenantId = MutableStateFlow("tenant_pionero")
    val currentTenantId: StateFlow<String> = _currentTenantId.asStateFlow()

    private val _currentBusinessType = MutableStateFlow("RESTAURANT")
    val currentBusinessType: StateFlow<String> = _currentBusinessType.asStateFlow()

    fun setTenant(tenantId: String, businessType: String) {
        _currentTenantId.value = tenantId
        _currentBusinessType.value = businessType
    }

    fun getTenantId(): String = _currentTenantId.value
    fun getBusinessType(): String = _currentBusinessType.value
}
