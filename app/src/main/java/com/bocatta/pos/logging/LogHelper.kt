package com.bocatta.pos.logging

import android.content.Context
import android.util.Log
import timber.log.Timber
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Helper singleton to initialise logging.
 *
 * • En modo debug escribe en Logcat (Timber DebugTree) y en un archivo JSON local.
 * • En modo release puedes plantar un árbol de Crashlytics (aún no incluido).
 * • Al arrancar, elimina automáticamente los archivos de log mayores de 15 días.
 */
object LogHelper {
    /** Initialise Timber with the appropriate trees. */
    fun init(context: Context, isDebug: Boolean) {
        pruneOldLogs(context)                     // 1️⃣ limpia logs viejos
        if (isDebug) {
            Timber.plant(Timber.DebugTree())
            Timber.plant(FileLoggingTree(context))
        } else {
            // TODO: plantar árbol Crashlytics si lo deseas
        }
    }

    /** Elimina los archivos de log que tengan más de 15 días. */
    internal fun pruneOldLogs(context: Context) {
        val logDir = getLogDirectory(context)
        if (!logDir.exists()) return
        val cutoff = System.currentTimeMillis() - 15L * 24 * 60 * 60 * 1000   // 15 días en ms
        logDir.listFiles()?.forEach { file ->
            if (file.isFile && file.lastModified() < cutoff) {
                if (!file.delete()) {
                    Timber.tag("LOG_CLEANUP")
                        .w("No se pudo borrar archivo de log: ${file.absolutePath}")
                }
            }
        }
    }

    /** Directorio de logs: preferimos `externalFilesDir/logs` para que el usuario lo vea. */
    private fun getLogDirectory(context: Context): File {
        // `externalFilesDir` es visible al usuario mediante el explorador de archivos
        val external = context.getExternalFilesDir("logs")
        return external ?: File(context.filesDir, "logs")
    }

    /** Árbol que escribe cada entrada en un archivo JSON (un archivo por día). */
    private class FileLoggingTree(private val ctx: Context) : Timber.Tree() {
        private val logDir: File = getLogDirectory(ctx).apply { if (!exists()) mkdirs() }
        private val dateFmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
        private val fileNameFmt = SimpleDateFormat("yyyyMMdd", Locale.US)

        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            val levelChar = when (priority) {
                Log.VERBOSE -> "V"
                Log.DEBUG -> "D"
                Log.INFO -> "I"
                Log.WARN -> "W"
                Log.ERROR -> "E"
                Log.ASSERT -> "A"
                else -> "U"
            }
            val timestamp = dateFmt.format(Date())
            val json = buildString {
                append('{')
                append("\"ts\":\"").append(timestamp).append("\",")
                append("\"lvl\":\"").append(levelChar).append("\",")
                append("\"tag\":\"").append(tag ?: "APP").append("\",")
                append("\"msg\":\"").append(message.replace("\"", "\\\"")).append('"')
                if (t != null) {
                    append(",\"err\":\"").append(t.stackTraceToString().replace("\"", "\\\"")).append('"')
                }
                append('}')
            }
            val logFile = File(logDir, "app_${fileNameFmt.format(Date())}.log")
            FileWriter(logFile, true).use { writer ->
                writer.appendLine(json)
            }
        }
    }
}

