package com.bocatta.pos.domain.util

import java.math.BigDecimal
import java.math.RoundingMode

fun Double.fiscalRound(): Double =
    BigDecimal.valueOf(this).setScale(2, RoundingMode.HALF_UP).toDouble()

fun BigDecimal.fiscalRound(): BigDecimal =
    setScale(2, RoundingMode.HALF_UP)
