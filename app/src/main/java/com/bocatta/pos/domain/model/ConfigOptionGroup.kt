package com.bocatta.pos.domain.model

enum class ConfigFieldType { SINGLE_CHIP, MULTI_CHIP, MULTI_CHECKBOX, TEXT }

data class ConfigOptionGroup(
    val key: String,
    val title: String,
    val type: ConfigFieldType,
    val options: List<String> = emptyList(),
    val required: Boolean = false,
    val multiMax: Int? = null,
    val defaultValue: String? = null,
    val preciosExtra: Map<String, Double> = emptyMap()
)

typealias ConfigResult = Map<String, List<String>>
