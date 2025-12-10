package com.sysmetrics.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.preference.PreferenceManager
import com.sysmetrics.app.service.MinimalistOverlayService
import timber.log.Timber

/**
 * Boot complete receiver for auto-starting overlay service
 * Following TvOverlay_cpu pattern
 */
class BootCompleteReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        Timber.d("BootCompleteReceiver triggered: ${intent.action}")

        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON" -> {
                handleBootComplete(context)
            }
        }
    }

    private fun handleBootComplete(context: Context) {
        try {
            // Check if auto-start is enabled in preferences
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val autoStartEnabled = prefs.getBoolean("auto_start_enabled", false)
            val overlayEnabled = prefs.getBoolean("overlay_enabled", false)

            Timber.d("Auto-start enabled: $autoStartEnabled, Overlay enabled: $overlayEnabled")

            if (autoStartEnabled && overlayEnabled) {
                startOverlayService(context)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to handle boot complete")
        }
    }

    private fun startOverlayService(context: Context) {
        try {
            val serviceIntent = Intent(context, com.sysmetrics.app.service.MinimalistOverlayService::class.java)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }

            Timber.d("Minimalist overlay service started on boot")
        } catch (e: Exception) {
            Timber.e(e, "Failed to start overlay service")
        }
    }
}
