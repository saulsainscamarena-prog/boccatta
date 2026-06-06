package com.bocatta.pos.domain.usecase

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.analytics.FirebaseAnalytics
import timber.log.Timber

/**
 * Gestiona el consentimiento del usuario para la recolección de datos de Firebase Analytics.
 *
 * Por defecto, la recolección está **deshabilitada** hasta que el usuario
 * otorgue consentimiento explícito. Esto cumple con buenas prácticas de privacidad
 * para una app POS que maneja datos de transacciones.
 *
 * Uso:
 * - Llamar [initializeWithSavedPreference] en Application.onCreate()
 * - Llamar [setConsentGranted] cuando el usuario acepte/rechace
 */
object AnalyticsConsentManager {

    private const val PREFS_NAME = "bocatta_analytics_prefs"
    private const val KEY_CONSENT = "analytics_consent_granted"

    /**
     * Inicializa Firebase Analytics con la preferencia guardada.
     * Si no hay preferencia guardada, desactiva la recolección por defecto.
     */
    fun initializeWithSavedPreference(context: Context) {
        val prefs = getPrefs(context)
        val consentGranted = prefs.getBoolean(KEY_CONSENT, false)
        applyConsent(context, consentGranted)
        Timber.tag("ANALYTICS").i("Analytics consent initialized: granted=$consentGranted")
    }

    /**
     * Establece el consentimiento del usuario y persiste la preferencia.
     * @param granted true si el usuario acepta la recolección de datos analíticos
     */
    fun setConsentGranted(context: Context, granted: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_CONSENT, granted).apply()
        applyConsent(context, granted)
        Timber.tag("ANALYTICS").i("Analytics consent updated: granted=$granted")
    }

    /** Consulta si el consentimiento fue otorgado. */
    fun isConsentGranted(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_CONSENT, false)
    }

    private fun applyConsent(context: Context, granted: Boolean) {
        try {
            FirebaseAnalytics.getInstance(context).setAnalyticsCollectionEnabled(granted)
        } catch (e: Exception) {
            Timber.tag("ANALYTICS").w(e, "Could not configure Firebase Analytics")
        }
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}
