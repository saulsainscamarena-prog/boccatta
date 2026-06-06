package com.bocatta.pos.domain.model

enum class ConfigFieldType(val firestoreKey: String) { SINGLE_CHIP("single_chip"), MULTI_CHIP("multi_chip"), MULTI_CHECKBOX("multi_checkbox"), TEXT("text") }

data class DescuentoOpcion(
    val insumoId: String = "",
    val cantidad: Double = 0.0,
    val unidad: String = "g"
)

data class ConfigOptionGroup(
    val key: String = "",
    val title: String = "",
    val type: ConfigFieldType = ConfigFieldType.SINGLE_CHIP,
    val options: List<String> = emptyList(),
    val required: Boolean = false,
    val multiMax: Int? = null,
    val defaultValue: String? = null,
    val preciosExtra: Map<String, Double> = emptyMap(),
    val descuentosInsumo: Map<String, DescuentoOpcion> = emptyMap()
)

typealias ConfigResult = Map<String, List<String>>
