package com.bocatta.pos.logging

import android.content.Context
import android.os.Build
import android.util.Log
import com.bocatta.pos.BuildConfig
import timber.log.Timber
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Logging local para diagnostico operativo del POS.
 * - Escribe Timber a Logcat en debug y a archivo local en debug/release.
 * - Guarda breadcrumbs de ultimas acciones.
 * - Captura crashes fatales en archivo antes de cerrar.
 * - Limpia logs mayores de 15 dias.
 */
object LogHelper {
    private const val MAX_BREADCRUMBS = 40
    private const val MAX_REPORT_CHARS = 120_000
    private val breadcrumbs = ArrayDeque<String>(MAX_BREADCRUMBS)
    private val lock = Any()

    fun init(context: Context, isDebug: Boolean) {
        pruneOldLogs(context)
        if (isDebug) {
            Timber.plant(Timber.DebugTree())
        }
        Timber.plant(FileLoggingTree(context.applicationContext))
        installCrashHandler(context.applicationContext)
        recordBreadcrumb("app_start", "version=${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
    }

    fun recordBreadcrumb(event: String, detail: String = "") {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
        val line = if (detail.isBlank()) "$timestamp | $event" else "$timestamp | $event | $detail"
        synchronized(lock) {
            if (breadcrumbs.size >= MAX_BREADCRUMBS) breadcrumbs.removeFirst()
            breadcrumbs.addLast(line.take(500))
        }
        Timber.tag("BREADCRUMB").i(line)
    }

    fun buildDiagnosticReport(context: Context, maxChars: Int = MAX_REPORT_CHARS): String {
        val header = buildString {
            appendLine("BOCATTA POS - DIAGNOSTICO")
            appendLine("Fecha: ${Date()}")
            appendLine("App: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            appendLine("Build: ${if (BuildConfig.DEBUG) "debug" else "release"}")
            appendLine("Android: ${Build.VERSION.RELEASE} / SDK ${Build.VERSION.SDK_INT}")
            appendLine("Dispositivo: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine()
            appendLine("ULTIMAS ACCIONES")
            snapshotBreadcrumbs().ifEmpty { listOf("Sin acciones registradas") }.forEach { appendLine(it) }
            appendLine()
            appendLine("LOGS RECIENTES")
        }
        val logs = getRecentLogText(context, maxChars - header.length)
        return (header + logs).take(maxChars)
    }

    internal fun pruneOldLogs(context: Context) {
        val logDir = getLogDirectory(context)
        if (!logDir.exists()) return
        val cutoff = System.currentTimeMillis() - 15L * 24 * 60 * 60 * 1000
        logDir.listFiles()?.forEach { file ->
            if (file.isFile && file.lastModified() < cutoff && !file.delete()) {
                Timber.tag("LOG_CLEANUP").w("No se pudo borrar archivo de log: ${file.absolutePath}")
            }
        }
    }

    internal fun getLogDirectory(context: Context): File {
        val external = context.getExternalFilesDir("logs")
        return external ?: File(context.filesDir, "logs")
    }

    private fun installCrashHandler(context: Context) {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        if (previous is BocattaCrashHandler) return
        Thread.setDefaultUncaughtExceptionHandler(BocattaCrashHandler(context, previous))
    }

    private fun snapshotBreadcrumbs(): List<String> = synchronized(lock) { breadcrumbs.toList() }

    private fun getRecentLogText(context: Context, maxChars: Int): String {
        if (maxChars <= 0) return ""
        val files = getLogDirectory(context).listFiles()
            ?.filter { it.isFile && (it.name.startsWith("app_") || it.name.startsWith("crash_")) }
            ?.sortedByDescending { it.lastModified() }
            ?.take(3)
            ?: return "Sin archivos de log locales."

        val builder = StringBuilder()
        for (file in files) {
            if (builder.length >= maxChars) break
            builder.appendLine("---- ${file.name} ----")
            val text = runCatching { file.readText() }.getOrElse { "No se pudo leer: ${it.message}" }
            val remaining = maxChars - builder.length
            builder.appendLine(text.takeLast(remaining.coerceAtLeast(0)))
        }
        return builder.toString().takeLast(maxChars)
    }

    private fun jsonEscape(value: String): String {
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    private class BocattaCrashHandler(
        private val context: Context,
        private val previous: Thread.UncaughtExceptionHandler?
    ) : Thread.UncaughtExceptionHandler {
        override fun uncaughtException(thread: Thread, throwable: Throwable) {
            runCatching {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val dir = getLogDirectory(context).apply { if (!exists()) mkdirs() }
                val file = File(dir, "crash_$timestamp.log")
                file.writeText(
                    buildString {
                        appendLine("BOCATTA POS - CRASH FATAL")
                        appendLine("Fecha: ${Date()}")
                        appendLine("Thread: ${thread.name}")
                        appendLine("App: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                        appendLine("Android: ${Build.VERSION.RELEASE} / SDK ${Build.VERSION.SDK_INT}")
                        appendLine("Dispositivo: ${Build.MANUFACTURER} ${Build.MODEL}")
                        appendLine()
                        appendLine("ULTIMAS ACCIONES")
                        snapshotBreadcrumbs().forEach { appendLine(it) }
                        appendLine()
                        appendLine("STACKTRACE")
                        appendLine(throwable.stackTraceToString())
                    }
                )
            }
            previous?.uncaughtException(thread, throwable) ?: kotlin.system.exitProcess(10)
        }
    }

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
                append("\"tag\":\"").append(jsonEscape(tag ?: "APP")).append("\",")
                append("\"msg\":\"").append(jsonEscape(message)).append('"')
                if (t != null) {
                    append(",\"err\":\"").append(jsonEscape(t.stackTraceToString())).append('"')
                }
                append('}')
            }
            val logFile = File(logDir, "app_${fileNameFmt.format(Date())}.log")
            runCatching {
                FileWriter(logFile, true).use { writer ->
                    writer.appendLine(json)
                }
            }
        }
    }
}
