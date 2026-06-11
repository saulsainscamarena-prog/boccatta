package com.bocatta.pos.domain.model

data class ThemeConfigV2(
    val enabled: Boolean = false,
    val primaryHex: String = "#FFB394",
    val secondaryHex: String = "#5B4035",
    val tertiaryHex: String = "#F2C078",
    val updatedAt: Long = 0L,
    val updatedBy: String = ""
)

