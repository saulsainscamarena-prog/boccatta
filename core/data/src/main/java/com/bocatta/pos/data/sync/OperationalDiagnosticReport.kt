package com.bocatta.pos.data.sync

import android.content.Context
import android.os.Build
import com.bocatta.pos.data.local.OfflineDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object OperationalDiagnosticReport {

    suspend fun build(context: Context): String = withContext(Dispatchers.IO) {
        val database = OfflineDatabase.getInstance(context.applicationContext)
        val version = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull().orEmpty().ifBlank { "desconocida" }

        buildString {
            appendLine("BOCATTA POS - DIAGNOSTICO OPERATIVO")
            appendLine(
                "Fecha: ${
                    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                }"
            )
            appendLine("App: $version")
            appendLine("Android: ${Build.VERSION.RELEASE} / SDK ${Build.VERSION.SDK_INT}")
            appendLine("Dispositivo: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine()
            appendLine("SINCRONIZACION")
            appendLine("Ventas pendientes: ${database.contarPendientes()}")
            appendLine("Ventas fallidas: ${database.obtenerVentasFallidas().size}")
            appendLine("Operaciones pendientes: ${database.contarOperacionesPendientes()}")
            appendLine("Turnos de contingencia pendientes: ${database.contarTurnosContingenciaPendientes()}")
            appendLine()
            append("El reporte no incluye PIN, credenciales ni datos completos de clientes.")
        }
    }
}
