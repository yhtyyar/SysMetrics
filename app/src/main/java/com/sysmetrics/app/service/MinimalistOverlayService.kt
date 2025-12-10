package com.sysmetrics.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import com.sysmetrics.app.R
import com.sysmetrics.app.data.source.SystemDataSource
import com.sysmetrics.app.utils.AppStats
import com.sysmetrics.app.utils.MetricsCollector
import com.sysmetrics.app.utils.ProcessStatsCollector
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

/**
 * Optimized overlay service - Production ready
 * Shows system metrics + top-3 apps by CPU/RAM usage
 * Efficient under load with accurate measurements
 */
@AndroidEntryPoint
class MinimalistOverlayService : Service() {

    @Inject
    lateinit var systemDataSource: SystemDataSource

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: LinearLayout
    private lateinit var metricsCollector: MetricsCollector
    private lateinit var processStatsCollector: ProcessStatsCollector

    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            updateMetrics()
            handler.postDelayed(this, UPDATE_INTERVAL_MS)
        }
    }

    // View references
    private lateinit var cpuText: TextView
    private lateinit var ramText: TextView
    private lateinit var selfStatsText: TextView
    private lateinit var topAppsContainer: LinearLayout

    private var topAppsCount = 3  // Default, configurable via settings
    private var topAppsSortBy = "combined"  // Default sorting
    private var isBaselineInitialized = false

    override fun onCreate() {
        super.onCreate()
        Timber.d("MinimalistOverlayService created")

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        metricsCollector = MetricsCollector(this, systemDataSource)
        processStatsCollector = ProcessStatsCollector(this)

        loadSettings()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        createOverlayView()
        
        // Initialize baseline with proper timing
        initializeBaseline()
    }

    /**
     * Initialize CPU baseline with proper 2-step measurement
     * Required for accurate delta calculation
     */
    private fun initializeBaseline() {
        handler.postDelayed({
            // First baseline measurement
            metricsCollector.getCpuUsage()
            processStatsCollector.initializeBaseline()
            
            Timber.d("Baseline initialized - first measurement")
            
            // Second measurement after interval to establish delta
            handler.postDelayed({
                metricsCollector.getCpuUsage()
                processStatsCollector.warmUpCache()
                isBaselineInitialized = true
                
                Timber.d("Baseline ready - starting metrics updates")
                
                // Start regular updates
                handler.post(updateRunnable)
            }, BASELINE_INIT_DELAY)
        }, 100L)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateRunnable)
        
        try {
            windowManager.removeView(overlayView)
        } catch (e: Exception) {
            Timber.e(e, "Failed to remove overlay view")
        }
        
        Timber.d("MinimalistOverlayService destroyed")
    }

    /**
     * Load settings from preferences
     * Supports dynamic configuration of top apps count and sorting
     */
    private fun loadSettings() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        
        // Load top apps configuration
        topAppsCount = prefs.getString("top_apps_count", "3")?.toIntOrNull() ?: 3
        topAppsSortBy = prefs.getString("top_apps_sort", "combined") ?: "combined"
        
        // Apply overlay opacity if overlayView is already created
        val opacity = prefs.getInt("overlay_opacity", 95)
        if (::overlayView.isInitialized) {
            overlayView.alpha = opacity / 100f
        }
        
        Timber.d("Settings loaded: topAppsCount=$topAppsCount, sortBy=$topAppsSortBy, opacity=$opacity")
    }

    /**
     * Create notification channel for foreground service
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "System Monitoring",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows system metrics overlay"
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Create notification for foreground service
     */
    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SysMetrics Running")
            .setContentText("Monitoring system metrics")
            .setSmallIcon(R.drawable.ic_monitor)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    /**
     * Create overlay view
     */
    private fun createOverlayView() {
        try {
            // Inflate layout
            overlayView = LayoutInflater.from(this)
                .inflate(R.layout.overlay_minimalist, null) as LinearLayout

            // Get view references
            cpuText = overlayView.findViewById(R.id.cpu_text)
            ramText = overlayView.findViewById(R.id.ram_text)
            selfStatsText = overlayView.findViewById(R.id.self_stats_text)
            topAppsContainer = overlayView.findViewById(R.id.top_apps_container)

            // Create window params
            val params = createLayoutParams()

            // Add view to window
            windowManager.addView(overlayView, params)

            Timber.d("Minimalist overlay view created")
        } catch (e: Exception) {
            Timber.e(e, "Failed to create overlay view")
        }
    }

    /**
     * Create WindowManager.LayoutParams for overlay
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
            
            flags = (WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                    or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED)
            
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            gravity = Gravity.TOP or Gravity.START
            x = dpToPx(10)
            y = dpToPx(40)
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
            // System metrics
            val cpuPercent = metricsCollector.getCpuUsage()
            val (usedMb, totalMb, ramPercent) = metricsCollector.getRamUsage()

            // Update CPU with color indicator
            cpuText.text = String.format("CPU: %.0f%%", cpuPercent)
            cpuText.setTextColor(getColorForValue(cpuPercent))

            // Update RAM with color indicator
            ramText.text = String.format("RAM: %d/%d MB", usedMb, totalMb)
            ramText.setTextColor(getColorForValue(ramPercent))

            // SysMetrics self stats with color (compact format)
            val selfStats = processStatsCollector.getSelfStats()
            selfStatsText.text = String.format(
                "Self: %.1f%% / %dM",
                selfStats.cpuPercent,
                selfStats.ramMb
            )
            selfStatsText.setTextColor(getColorForValue(selfStats.cpuPercent))

            // Top apps
            updateTopApps()

        } catch (e: Exception) {
            Timber.e(e, "Failed to update metrics")
        }
    }

    /**
     * Update top-N apps list (configurable count and sorting)
     * Supports dynamic configuration from settings
     */
    private fun updateTopApps() {
        try {
            if (topAppsCount <= 0) {
                // Clear all apps if count is 0
                val childCount = topAppsContainer.childCount
                if (childCount > 1) {
                    topAppsContainer.removeViews(1, childCount - 1)
                }
                return
            }
            
            // Get top-N apps by configured sorting method
            val topApps = processStatsCollector.getTopApps(topAppsCount, topAppsSortBy)
            
            // Clear previous views (keep title)
            val childCount = topAppsContainer.childCount
            if (childCount > 1) {
                topAppsContainer.removeViews(1, childCount - 1)
            }

            // Add top-3 apps
            for (app in topApps) {
                val appView = createAppView(app)
                topAppsContainer.addView(appView)
            }

        } catch (e: Exception) {
            Timber.e(e, "Failed to update top apps")
        }
    }

    /**
     * Create view for single app stat (optimized format)
     */
    private fun createAppView(appStats: AppStats): TextView {
        return TextView(this).apply {
            // Format: AppName: CPU% / RAM MB
            text = String.format(
                "%s: %.0f%% / %dM",
                appStats.appName.take(12),  // Show more chars
                appStats.cpuPercent,
                appStats.ramMb
            )
            textSize = 10f  // Slightly larger for readability
            // Apply color based on CPU usage
            setTextColor(getColorForValue(appStats.cpuPercent))
            typeface = android.graphics.Typeface.MONOSPACE
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(2)
            }
        }
    }

    /**
     * Get color for load indicator
     * Green: 0-50%, Yellow: 50-80%, Red: 80-100%
     */
    private fun getColorForValue(percent: Float): Int {
        return when {
            percent < 50f -> getColor(R.color.metric_normal)  // Green
            percent < 80f -> getColor(R.color.metric_warning)  // Yellow/Orange
            else -> getColor(R.color.metric_error)  // Red
        }
    }

    companion object {
        private const val CHANNEL_ID = "sysmetrics_minimalist"
        private const val NOTIFICATION_ID = 2001
        private const val UPDATE_INTERVAL_MS = 500L  // Optimal for real-time monitoring
        private const val BASELINE_INIT_DELAY = 1000L  // Allow CPU baseline to stabilize
    }
}
