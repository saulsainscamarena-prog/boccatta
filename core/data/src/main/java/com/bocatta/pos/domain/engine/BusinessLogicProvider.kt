package com.bocatta.pos.domain.engine

/**
 * Define las características activas según el giro (businessType) del tenant actual.
 * Permite ocultar/mostrar módulos en la UI (ej. AdminScreen, menú lateral).
 */
data class BusinessFeatures(
    val usaRecetas: Boolean,
    val usaKDS: Boolean,
    val usaVariantesRetail: Boolean,
    val usaAgendaServicios: Boolean,
    val labelCatalogo: String, // "Menú", "Productos", "Servicios"
    val showCartBases: Boolean // True en restaurante, False en retail
)

object BusinessLogicProvider {
    fun getFeaturesForType(type: String): BusinessFeatures {
        return when (type.uppercase(java.util.Locale.getDefault())) {
            "RESTAURANT", "FOOD_TRUCK", "CAFE" -> BusinessFeatures(
                usaRecetas = true,
                usaKDS = true,
                usaVariantesRetail = false,
                usaAgendaServicios = false,
                labelCatalogo = "Menú",
                showCartBases = true
            )
            "RETAIL", "GROCERY" -> BusinessFeatures(
                usaRecetas = false,
                usaKDS = false,
                usaVariantesRetail = true,
                usaAgendaServicios = false,
                labelCatalogo = "Productos",
                showCartBases = false
            )
            "SERVICES", "BARBERSHOP", "CLINIC" -> BusinessFeatures(
                usaRecetas = false,
                usaKDS = false,
                usaVariantesRetail = false,
                usaAgendaServicios = true,
                labelCatalogo = "Servicios",
                showCartBases = false
            )
            else -> BusinessFeatures(
                usaRecetas = false,
                usaKDS = false,
                usaVariantesRetail = false,
                usaAgendaServicios = false,
                labelCatalogo = "Catálogo",
                showCartBases = false
            )
        }
    }
}
