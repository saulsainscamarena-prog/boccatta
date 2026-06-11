package com.bocatta.pos.data.security

import android.content.Context
import com.bocatta.pos.domain.usecase.PinRateLimitStore

class SharedPreferencesPinRateLimitStore(
    context: Context
) : PinRateLimitStore {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override var intentosFallidos: Int
        get() = prefs.getInt(KEY_FAILED_ATTEMPTS, 0)
        set(value) {
            prefs.edit().putInt(KEY_FAILED_ATTEMPTS, value.coerceAtLeast(0)).apply()
        }

    override var cooldownHasta: Long
        get() = prefs.getLong(KEY_COOLDOWN_UNTIL, 0L)
        set(value) {
            prefs.edit().putLong(KEY_COOLDOWN_UNTIL, value.coerceAtLeast(0L)).apply()
        }

    override fun reset() {
        prefs.edit()
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .putLong(KEY_COOLDOWN_UNTIL, 0L)
            .apply()
    }

    private companion object {
        const val PREFS_NAME = "bocatta_pin_rate_limit"
        const val KEY_FAILED_ATTEMPTS = "failed_attempts"
        const val KEY_COOLDOWN_UNTIL = "cooldown_until"
    }
}
