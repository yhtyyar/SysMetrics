package com.sysmetrics.app.domain.analytics

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.sysmetrics.app.data.model.advanced.MonitoringSettings
import com.sysmetrics.app.data.model.advanced.PeakStats
import timber.log.Timber

/**
 * Manages peak notification toasts.
 * Shows periodic summaries of peak metrics.
 */
class PeakNotificationManager(
    private val context: Context,
    private val peakTracker: PeakTracker
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var notificationRunnable: Runnable? = null
    private var isRunning = false
    
    private var intervalMs = 60_000L
    private var toastDurationMs = 5000
    private var showCpu = true
    private var showRam = true
    private var showTemp = true
    private var showNet = true
    private var showFps = false
    
    fun start(settings: MonitoringSettings) {
        if (isRunning) stop()
        
        intervalMs = settings.peakNotificationIntervalMs
        toastDurationMs = settings.toastDurationMs
        showCpu = settings.showCpuPeak
        showRam = settings.showRamPeak
        showTemp = settings.showTempPeak
        showNet = settings.showNetPeak
        showFps = settings.showFpsPeak
        
        isRunning = true
        scheduleNextNotification()
        Timber.d("Peak notifications started (interval: ${intervalMs}ms)")
    }
    
    fun stop() {
        isRunning = false
        notificationRunnable?.let { mainHandler.removeCallbacks(it) }
        notificationRunnable = null
        Timber.d("Peak notifications stopped")
    }
    
    fun updateSettings(settings: MonitoringSettings) {
        if (!settings.peakNotificationsEnabled) {
            stop()
            return
        }
        
        intervalMs = settings.peakNotificationIntervalMs
        toastDurationMs = settings.toastDurationMs
        showCpu = settings.showCpuPeak
        showRam = settings.showRamPeak
        showTemp = settings.showTempPeak
        showNet = settings.showNetPeak
        showFps = settings.showFpsPeak
        
        if (!isRunning) {
            start(settings)
        }
    }
    
    private fun scheduleNextNotification() {
        if (!isRunning) return
        
        notificationRunnable = Runnable {
            showPeakNotification()
            scheduleNextNotification()
        }
        mainHandler.postDelayed(notificationRunnable!!, intervalMs)
    }
    
    private fun showPeakNotification() {
        val stats = peakTracker.getCurrentPeakStats()
        
        // Don't show if no data
        if (stats.cpuPeak == 0f && stats.ramPeakMb == 0L) {
            Timber.d("Skipping notification - no data")
            return
        }
        
        val message = stats.toDisplayString(
            showCpu = showCpu,
            showRam = showRam,
            showTemp = showTemp,
            showNet = showNet,
            showFps = showFps
        )
        
        mainHandler.post {
            val duration = if (toastDurationMs > 4000) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
            Toast.makeText(context, message, duration).show()
        }
        
        // Reset tracker for next window
        peakTracker.reset()
        
        Timber.d("Peak notification shown")
    }
    
    fun showImmediateNotification() {
        showPeakNotification()
    }
}
