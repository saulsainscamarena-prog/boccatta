package com.bocatta.pos.core

import android.content.Context
import android.content.Intent
import android.util.Log

object ShareUtils {
    private const val TAG = "ShareUtils"

    /**
     * Attempts to share text via WhatsApp. If WhatsApp is not installed or fails,
     * falls back to the generic system chooser.
     */
    fun shareTextWhatsAppFallback(
        context: Context,
        text: String,
        subject: String? = null,
        logContext: String = "Share"
    ) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
                subject?.let { putExtra(Intent.EXTRA_SUBJECT, it) }
                setPackage("com.whatsapp")
            }
            context.startActivity(intent)
            Log.d(TAG, "$logContext: Shared successfully via WhatsApp")
        } catch (e: Exception) {
            Log.w(TAG, "$logContext: WhatsApp not available or error, falling back to Chooser", e)
            shareTextGeneric(context, text, subject, logContext)
        }
    }

    /**
     * Shares text using the generic system chooser.
     */
    fun shareTextGeneric(
        context: Context,
        text: String,
        subject: String? = null,
        logContext: String = "Share"
    ) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
                subject?.let { putExtra(Intent.EXTRA_SUBJECT, it) }
            }
            val chooserTitle = subject ?: "Compartir"
            val chooserIntent = Intent.createChooser(intent, chooserTitle)
            context.startActivity(chooserIntent)
            Log.d(TAG, "$logContext: Shared via Chooser")
        } catch (e: Exception) {
            Log.e(TAG, "$logContext: Error sharing via Chooser", e)
        }
    }
}
