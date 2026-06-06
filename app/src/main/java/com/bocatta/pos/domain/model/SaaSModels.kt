package com.bocatta.pos.domain.model

data class Tenant(
    val id: String = "",
    val nombre: String = "",
    val businessType: String = "RESTAURANT", // RESTAURANT, RETAIL, SERVICES, GENERAL
    val planId: String = "PLAN_ARRANQUE",
    val estado: String = "ACTIVE",
    val createdAt: Long = System.currentTimeMillis()
)

data class PlanEntitlement(
    val id: String = "",
    val nombre: String = "",
    val maxSucursales: Int = 1,
    val maxUsuarios: Int = 3,
    val featuresActivas: List<String> = emptyList(),
    val precioMensual: Double = 0.0
)

data class TenantUser(
    val uid: String = "",
    val tenantId: String = "",
    val role: String = "OWNER",
    val nombre: String = "",
    val email: String = "",
    val estado: String = "ACTIVE"
)
