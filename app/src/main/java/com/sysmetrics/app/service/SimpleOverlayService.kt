package com.sysmetrics.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.sysmetrics.app.R
import com.sysmetrics.app.SysMetricsApp
import com.sysmetrics.app.data.source.SystemDataSource
import com.sysmetrics.app.ui.MainActivity
import com.sysmetrics.app.utils.MetricsCollector
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

/**
 * Simplified Overlay Service following TvOverlay_cpu pattern
 * Uses Handler for periodic updates instead of Coroutines
 * Uses XML layout instead of custom View
 */
@AndroidEntryPoint
class SimpleOverlayService : Service() {

    companion object {
        private const val UPDATE_INTERVAL_MS = 500L
        private const val OVERLAY_WIDTH = 300
        private const val OVERLAY_HEIGHT = WindowManager.LayoutParams.WRAP_CONTENT
    }

    @Inject
    lateinit var systemDataSource: SystemDataSource

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: LinearLayout
    private lateinit var metricsCollector: MetricsCollector

    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            updateMetrics()
            handler.postDelayed(this, UPDATE_INTERVAL_MS)
        }
    }

    // View references
    private lateinit var cpuValue: TextView
    private lateinit var cpuProgress: ProgressBar
    private lateinit var cpuIndicator: TextView
    private lateinit var ramValue: TextView
    private lateinit var ramProgress: ProgressBar
    private lateinit var ramIndicator: TextView
    private lateinit var tempValue: TextView
    private lateinit var coresInfo: TextView

    override fun onCreate() {
        super.onCreate()
        Timber.d("SimpleOverlayService created")

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        metricsCollector = MetricsCollector(this, systemDataSource)

        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("SimpleOverlayService started")

        // Start foreground
        val notification = createNotification()
        startForeground(SysMetricsApp.NOTIFICATION_ID, notification)

        // Create and show overlay
        if (!::overlayView.isInitialized) {
            createOverlayView()
            handler.post(updateRunnable)
        }

        return START_STICKY
    }

    override fun onDestroy() {
        Timber.d("SimpleOverlayService destroyed")

        // Stop updates
        handler.removeCallbacks(updateRunnable)

        // Remove overlay
        try {
            if (::overlayView.isInitialized) {
                windowManager.removeView(overlayView)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to remove overlay view")
        }

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * Create overlay view from XML layout
     */
    private fun createOverlayView() {
        try {
            // Inflate layout
            overlayView = LayoutInflater.from(this)
                .inflate(R.layout.overlay_metrics, null) as LinearLayout

            // Get view references
            cpuValue = overlayView.findViewById(R.id.cpu_value)
            cpuProgress = overlayView.findViewById(R.id.cpu_progress)
            cpuIndicator = overlayView.findViewById(R.id.cpu_indicator)
            ramValue = overlayView.findViewById(R.id.ram_value)
            ramProgress = overlayView.findViewById(R.id.ram_progress)
            ramIndicator = overlayView.findViewById(R.id.ram_indicator)
            tempValue = overlayView.findViewById(R.id.temp_value)
            coresInfo = overlayView.findViewById(R.id.cores_info)

            // Create window params
            val params = createLayoutParams()

            // Add view to window
            windowManager.addView(overlayView, params)

            Timber.d("Overlay view created and added")
        } catch (e: Exception) {
            Timber.e(e, "Failed to create overlay view")
        }
    }

    /**
     * Create WindowManager.LayoutParams for overlay
     * Fixed for Android TV compatibility
     */
    private fun createLayoutParams(): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        }

        return WindowManager.LayoutParams().apply {
            this.type = type
            format = PixelFormat.TRANSLUCENT
            
            // Flags for non-interactive overlay
            flags = (WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                    or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED)
            
            width = dpToPx(OVERLAY_WIDTH)
            height = OVERLAY_HEIGHT
            gravity = Gravity.TOP or Gravity.START
            x = dpToPx(20)
            y = dpToPx(50)
        }
    }
    
    /**
     * Convert dp to pixels
     */
    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }

    /**
     * Update metrics display
     */
    private fun updateMetrics() {
        try {
            // Get metrics
            val cpuPercent = metricsCollector.getCpuUsage()
            val ramData = metricsCollector.getRamUsage()
            val temperature = metricsCollector.getTemperature()
            val cores = metricsCollector.getCoreCount()

            // Update CPU with color indicator
            cpuValue.text = String.format("%.1f%%", cpuPercent)
            val cpuColor = getColorForValue(cpuPercent)
            cpuIndicator.setTextColor(cpuColor)
            cpuProgress.progress = cpuPercent.toInt().coerceIn(0, 100)

            // Update RAM - Used / Total format with color indicator
            val (usedMb, totalMb, ramPercent) = ramData
            ramValue.text = String.format("%d / %d MB", usedMb, totalMb)
            val ramColor = getColorForValue(ramPercent)
            ramIndicator.setTextColor(ramColor)
            ramProgress.progress = ramPercent.toInt().coerceIn(0, 100)

            // Update Temperature
            if (temperature > 0) {
                tempValue.text = String.format("%.0f°C", temperature)
                tempValue.setTextColor(getColorForTemperature(temperature))
            } else {
                tempValue.text = getString(R.string.not_available)
            }

            // Update cores info
            coresInfo.text = getString(R.string.cores_format, cores)

        } catch (e: Exception) {
            Timber.e(e, "Failed to update metrics")
        }
    }

    /**
     * Get color based on percentage value
     */
    private fun getColorForValue(percent: Float): Int {
        return when {
            percent < 50 -> getColor(R.color.metric_success)
            percent < 80 -> getColor(R.color.metric_warning)
            else -> getColor(R.color.metric_error)
        }
    }

    /**
     * Get color based on temperature
     */
    private fun getColorForTemperature(temp: Float): Int {
        return when {
            temp < 60 -> getColor(R.color.metric_success)
            temp < 80 -> getColor(R.color.metric_warning)
            else -> getColor(R.color.metric_error)
        }
    }

    /**
     * Create notification channel (Android 8+)
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                SysMetricsApp.NOTIFICATION_CHANNEL_ID,
                "SysMetrics Overlay",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Real-time system metrics overlay"
                enableVibration(false)
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Create notification for foreground service
     */
    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, SysMetricsApp.NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(R.drawable.ic_monitor)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
