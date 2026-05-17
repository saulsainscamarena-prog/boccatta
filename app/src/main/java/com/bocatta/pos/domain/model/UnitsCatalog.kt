package com.bocatta.pos.domain.model

data class UnitsCatalog(
    val dimensions: Map<String, Map<String, UnitDefV2>> = emptyMap()
)

data class UnitDefV2(
    val code: String = "",
    val dimension: String = "",
    val factorToBase: Double = 1.0,
    val symbol: String = ""
)

