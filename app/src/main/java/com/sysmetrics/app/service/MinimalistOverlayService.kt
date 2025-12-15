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
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.sysmetrics.app.core.common.Constants
import com.sysmetrics.app.data.source.SystemDataSource
import com.sysmetrics.app.domain.collector.IMetricsCollector
import com.sysmetrics.app.domain.collector.IProcessStatsCollector
import com.sysmetrics.app.ui.overlay.DraggableOverlayTouchListener
import com.sysmetrics.app.utils.AdaptivePerformanceMonitor
import com.sysmetrics.app.utils.AppStats
import com.sysmetrics.app.utils.DeviceUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Optimized overlay service - Production ready
 * Shows system metrics + top-N apps by CPU/RAM usage
 * Efficient under load with accurate measurements
 * 
 * LOGGING TAGS:
 * - OVERLAY_SERVICE: Service lifecycle events
 * - OVERLAY_UPDATE: Metrics update cycle
 * - OVERLAY_DISPLAY: What's shown on screen
 * - OVERLAY_SETTINGS: Settings changes
 * 
 * IMPROVEMENTS:
 * - Uses LifecycleService for proper lifecycle and coroutine support
 * - Injects interfaces for testability
 * - Uses coroutines for async operations
 * - Better separation of concerns
 */
@AndroidEntryPoint
class MinimalistOverlayService : LifecycleService() {
    
    companion object {
        // Logging tags for easy filtering
        private const val TAG_SERVICE = "OVERLAY_SERVICE"
        private const val TAG_UPDATE = "OVERLAY_UPDATE"
        private const val TAG_DISPLAY = "OVERLAY_DISPLAY"
        private const val TAG_SETTINGS = "OVERLAY_SETTINGS"
    }

    @Inject
    lateinit var systemDataSource: SystemDataSource
    
    @Inject
    lateinit var deviceUtils: DeviceUtils
    
    @Inject
    lateinit var metricsCollector: IMetricsCollector
    
    @Inject
    lateinit var processStatsCollector: IProcessStatsCollector
    
    @Inject
    lateinit var adaptiveMonitor: AdaptivePerformanceMonitor
    
    @Inject
    lateinit var preferencesDataSource: com.sysmetrics.app.data.source.PreferencesDataSource

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: LinearLayout
    private lateinit var layoutParams: WindowManager.LayoutParams
    
    // Adaptive performance monitoring
    private var currentUpdateInterval = Constants.OverlayService.UPDATE_INTERVAL_MS
    private var adaptiveCheckCounter = 0

    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            updateMetrics()
            
            // Adjust interval adaptively
            if (adaptiveCheckCounter++ >= Constants.OverlayService.ADAPTIVE_CHECK_CYCLES) {
                adjustUpdateIntervalIfNeeded()
                adaptiveCheckCounter = 0
            }
            
            handler.postDelayed(this, currentUpdateInterval)
        }
    }

    // View references
    private lateinit var cpuText: TextView
    private lateinit var ramText: TextView
    private lateinit var selfStatsText: TextView
    private lateinit var topAppsContainer: LinearLayout

    private var topAppsCount = Constants.OverlayService.DEFAULT_TOP_APPS_COUNT
    private var topAppsSortBy = Constants.OverlayService.DEFAULT_SORT_BY
    private var isBaselineInitialized = false

    override fun onCreate() {
        super.onCreate()
        Timber.tag(TAG_SERVICE).i("✅ MinimalistOverlayService created")
        
        // Setup exception handler for TV-specific crashes
        setupExceptionHandler()

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        Timber.tag(TAG_SERVICE).d("📦 Collectors injected via Hilt")
        
        // Log device capabilities
        deviceUtils.logDeviceCapabilities()
        
        // Set optimal initial interval
        currentUpdateInterval = deviceUtils.getOptimalUpdateInterval()
        Timber.tag(TAG_SERVICE).d("Initial update interval: ${currentUpdateInterval}ms")

        loadSettings()
        createNotificationChannel()
        startForeground(Constants.OverlayService.NOTIFICATION_ID, createNotification())
        createOverlayView()
        
        // Initialize baseline with proper timing
        Timber.tag(TAG_SERVICE).d("🔧 Starting baseline initialization...")
        initializeBaseline()
    }
    
    /**
     * Setup exception handler for TV-specific crashes (ACTION_HOVER_EXIT).
     * Prevents crash when hover events occur on Android TV.
     */
    private fun setupExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Timber.tag(TAG_SERVICE).e(throwable, "Uncaught exception in ${thread.name}")
            
            // Handle Compose/View hover event crash on TV
            if (throwable is IllegalStateException && 
                throwable.message?.contains("ACTION_HOVER") == true) {
                Timber.tag(TAG_SERVICE).w("Caught TV hover event crash - recovering gracefully")
                return@setDefaultUncaughtExceptionHandler
            }
            
            // Use default handler for other exceptions
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    /**
     * Initialize CPU baseline with proper 2-step measurement
     * Required for accurate delta calculation
     */
    private fun initializeBaseline() {
        lifecycleScope.launch {
            try {
                Timber.tag(TAG_SERVICE).d("🎯 Step 1: First baseline measurement")
                
                // Initialize baseline in MetricsCollector (CRITICAL!)
                metricsCollector.initializeBaseline()
                
                // Initialize baseline in ProcessStatsCollector
                processStatsCollector.initializeBaseline()
                
                Timber.tag(TAG_SERVICE).i("✅ Baseline initialized - waiting for delta...")
                
                // Wait for baseline to settle
                kotlinx.coroutines.delay(Constants.OverlayService.BASELINE_INIT_DELAY_MS)
                
                Timber.tag(TAG_SERVICE).d("🎯 Step 2: Second measurement for delta")
                
                // Trigger second measurement to establish delta
                val initialCpu = metricsCollector.getCpuUsage()
                processStatsCollector.warmUpCache()
                
                isBaselineInitialized = true
                
                Timber.tag(TAG_SERVICE).i("✅ Baseline ready - Initial CPU: %.2f%% - starting metrics updates", initialCpu)
                
                // Start regular updates on main thread
                handler.post(updateRunnable)
            } catch (e: Exception) {
                Timber.tag(TAG_SERVICE).e(e, "Failed to initialize baseline")
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateRunnable)
        
        try {
            windowManager.removeView(overlayView)
        } catch (e: Exception) {
            Timber.e(e, "Failed to remove overlay view")
        }
        
        Timber.tag(TAG_SERVICE).i("✅ MinimalistOverlayService destroyed")
    }

    /**
     * Load settings from preferences
     * Supports dynamic configuration of top apps count and sorting
     */
    private fun loadSettings() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        
        // Load top apps configuration
        topAppsCount = prefs.getString("top_apps_count", Constants.OverlayService.DEFAULT_TOP_APPS_COUNT.toString())
            ?.toIntOrNull() ?: Constants.OverlayService.DEFAULT_TOP_APPS_COUNT
        topAppsSortBy = prefs.getString("top_apps_sort", Constants.OverlayService.DEFAULT_SORT_BY) 
            ?: Constants.OverlayService.DEFAULT_SORT_BY
        
        // Apply overlay opacity if overlayView is already created
        val opacity = prefs.getInt("overlay_opacity", Constants.OverlayService.DEFAULT_OPACITY_PERCENT)
        if (::overlayView.isInitialized) {
            overlayView.alpha = opacity / 100f
        }
        
        Timber.tag(TAG_SETTINGS).i("⚙️ Settings loaded: topAppsCount=%d, sortBy=%s, opacity=%d", 
            topAppsCount, topAppsSortBy, opacity)
    }

    /**
     * Create notification channel for foreground service
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Constants.OverlayService.CHANNEL_ID,
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
        return NotificationCompat.Builder(this, Constants.OverlayService.CHANNEL_ID)
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
            Timber.tag(TAG_SERVICE).d("🎨 Creating overlay view...")
            
            // Inflate layout
            overlayView = LayoutInflater.from(this)
                .inflate(R.layout.overlay_minimalist, null) as LinearLayout

            // Get view references
            cpuText = overlayView.findViewById(R.id.cpu_text)
            ramText = overlayView.findViewById(R.id.ram_text)
            selfStatsText = overlayView.findViewById(R.id.self_stats_text)
            topAppsContainer = overlayView.findViewById(R.id.top_apps_container)

            // Verify all views are found and log status
            Timber.tag(TAG_SERVICE).d("📋 View references: CPU=%b, RAM=%b, Self=%b, Apps=%b",
                ::cpuText.isInitialized, ::ramText.isInitialized,
                ::selfStatsText.isInitialized, ::topAppsContainer.isInitialized)

            // Create window params
            val params = createLayoutParams()
            
            Timber.tag(TAG_SERVICE).d("📐 Window params: x=%d, y=%d, gravity=%d",
                params.x, params.y, params.gravity)

            // Add view to window
            windowManager.addView(overlayView, params)

            Timber.tag(TAG_SERVICE).i("✅ Overlay view created and added to window")
        } catch (e: Exception) {
            Timber.tag(TAG_SERVICE).e(e, "❌ Failed to create overlay view")
        }
    }

    /**
     * Create WindowManager.LayoutParams for overlay
     * TV-specific flags to prevent hover event crashes
     */
    private fun createLayoutParams(): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        }
        
        // Determine margin based on device type
        val marginDp = deviceUtils.getOverlayMargin()
        val margin = dpToPx(marginDp)

        return WindowManager.LayoutParams().apply {
            this.type = type
            format = PixelFormat.TRANSLUCENT
            
            // Flags: Different for TV vs Mobile
            flags = if (deviceUtils.isTvDevice()) {
                // TV: Disable touch to prevent hover crashes
                (WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED)
            } else {
                // Mobile: Enable touch for dragging
                (WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED)
            }
            
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            gravity = Gravity.TOP or Gravity.START
            x = margin
            y = margin
        }.also {
            layoutParams = it
            
            // Enable dragging for mobile devices
            if (deviceUtils.shouldEnableDragging()) {
                enableDragging()
            }
        }
    }
    
    /**
     * Enable dragging for mobile devices.
     */
    private fun enableDragging() {
        if (!::overlayView.isInitialized) return
        
        val dragListener = DraggableOverlayTouchListener(
            params = layoutParams,
            windowManager = windowManager,
            onPositionChanged = { x, y ->
                Timber.tag(TAG_SERVICE).d("Overlay position changed: ($x, $y)")
                lifecycleScope.launch {
                    preferencesDataSource.updatePosition(x, y)
                    Timber.tag(TAG_SERVICE).i("✅ Position saved to preferences")
                }
            }
        )
        
        overlayView.setOnTouchListener(dragListener)
        Timber.tag(TAG_SERVICE).i("✅ Dragging enabled for mobile device")
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
        lifecycleScope.launch {
            try {
                val startTime = System.currentTimeMillis()
                Timber.tag(TAG_UPDATE).v("🔄 Update cycle #%d started", System.currentTimeMillis() / 1000)
                
                // System metrics
                val cpuPercent = metricsCollector.getCpuUsage()
                val (usedMb, totalMb, ramPercent) = metricsCollector.getRamUsage()

                Timber.tag(TAG_UPDATE).d("📊 Metrics collected: CPU=%.2f%%, RAM=%d/%dMB (%.1f%%)",
                    cpuPercent, usedMb, totalMb, ramPercent)

                // Update UI on main thread
                updateUI(cpuPercent, usedMb, totalMb, ramPercent)
                
                val duration = System.currentTimeMillis() - startTime
                Timber.tag(TAG_UPDATE).v("✅ Update cycle completed in %dms", duration)
                
                // Warn if update is too slow
                if (duration > Constants.OverlayService.SLOW_UPDATE_THRESHOLD_MS) {
                    Timber.tag(TAG_UPDATE).w("⚠️ Slow update cycle: %dms (should be <%dms)", 
                        duration, Constants.OverlayService.SLOW_UPDATE_THRESHOLD_MS)
                }

            } catch (e: Exception) {
                Timber.tag("OVERLAY_ERROR").e(e, "❌ Failed to update metrics")
            }
        }
    }
    
    /**
     * Update UI with metrics (must be called from main thread)
     */
    private suspend fun updateUI(cpuPercent: Float, usedMb: Long, totalMb: Long, ramPercent: Float) {
        // Update CPU with color indicator
        val cpuDisplay = String.format("CPU: %.0f%%", cpuPercent)
        cpuText.text = cpuDisplay
        cpuText.setTextColor(getColorForValue(cpuPercent))
        
        val cpuColor = when {
            cpuPercent < 50 -> "GREEN"
            cpuPercent < 80 -> "YELLOW"
            else -> "RED"
        }
        Timber.tag(TAG_DISPLAY).d("📺 CPU on SCREEN: '%s' color=%s", cpuDisplay, cpuColor)

        // Update RAM with color indicator
        val ramDisplay = String.format("RAM: %d/%d MB", usedMb, totalMb)
        ramText.text = ramDisplay
        ramText.setTextColor(getColorForValue(ramPercent))
        
        Timber.tag(TAG_DISPLAY).d("📺 RAM on SCREEN: '%s' (%.1f%%)", ramDisplay, ramPercent)

        // SysMetrics self stats with color (compact format)
        val selfStats = processStatsCollector.getSelfStats()
        val selfDisplay = String.format(
            "Self: %.1f%% / %dM",
            selfStats.cpuPercent,
            selfStats.ramMb
        )
        selfStatsText.text = selfDisplay
        selfStatsText.setTextColor(getColorForValue(selfStats.cpuPercent))
        
        Timber.tag(TAG_DISPLAY).d("📺 SELF on SCREEN: '%s'", selfDisplay)

        // Top apps
        updateTopApps()
    }

    /**
     * Update top-N apps list (configurable count and sorting)
     * Supports dynamic configuration from settings
     */
    private suspend fun updateTopApps() {
        try {
            if (topAppsCount <= 0) {
                // Clear all apps if count is 0
                val childCount = topAppsContainer.childCount
                if (childCount > 1) {
                    topAppsContainer.removeViews(1, childCount - 1)
                }
                Timber.tag(TAG_DISPLAY).d("📺 SCREEN: Top Apps section hidden (count=0)")
                return
            }
            
            // Get top-N apps by configured sorting method
            val topApps = processStatsCollector.getTopApps(topAppsCount, topAppsSortBy)
            
            // Clear previous views (keep title)
            val childCount = topAppsContainer.childCount
            if (childCount > 1) {
                topAppsContainer.removeViews(1, childCount - 1)
            }

            // Add top-N apps and log what's displayed
            Timber.tag(TAG_DISPLAY).d("📺 SCREEN: Showing %d top apps:", topApps.size)
            for ((index, app) in topApps.withIndex()) {
                val appView = createAppView(app)
                topAppsContainer.addView(appView)
                Timber.tag(TAG_DISPLAY).d("📺   #%d: %s: %.0f%% / %dMB", 
                    index + 1, app.appName, app.cpuPercent, app.ramMb)
            }

        } catch (e: Exception) {
            Timber.tag("OVERLAY_ERROR").e(e, "❌ Failed to update top apps")
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
                appStats.appName.take(Constants.OverlayService.APP_NAME_MAX_LENGTH),
                appStats.cpuPercent,
                appStats.ramMb
            )
            textSize = Constants.OverlayService.APP_TEXT_SIZE
            // Apply color based on CPU usage
            setTextColor(getColorForValue(appStats.cpuPercent))
            typeface = android.graphics.Typeface.MONOSPACE
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(Constants.OverlayService.APP_BOTTOM_MARGIN_DP)
            }
        }
    }

    /**
     * Get color for load indicator
     * Green: 0-50%, Yellow: 50-80%, Red: 80-100%
     */
    private fun getColorForValue(percent: Float): Int {
        return when {
            percent < Constants.PerformanceThresholds.CPU_NORMAL_MAX -> getColor(R.color.metric_normal)  // Green
            percent < Constants.PerformanceThresholds.CPU_WARNING_MAX -> getColor(R.color.metric_warning)  // Yellow/Orange
            else -> getColor(R.color.metric_error)  // Red
        }
    }
    
    /**
     * Adjust update interval based on system load (adaptive performance).
     * Only runs if adaptive monitoring is enabled.
     */
    private fun adjustUpdateIntervalIfNeeded() {
        if (!deviceUtils.shouldUseAdaptivePerformance()) {
            return
        }
        
        lifecycleScope.launch {
            try {
                // Get current metrics
                val cpuUsage = metricsCollector.getCpuUsage()
                val (usedMb, totalMb, ramPercent) = metricsCollector.getRamUsage()
            
            // Create SystemMetrics for adaptive monitor
            val metrics = com.sysmetrics.app.data.model.SystemMetrics(
                cpuUsage = cpuUsage,
                ramUsedMb = usedMb,
                ramTotalMb = totalMb,
                ramUsagePercent = ramPercent
            )
            
            // Calculate optimal interval
            val optimalInterval = adaptiveMonitor.calculateOptimalInterval(
                metrics = metrics,
                isTvDevice = deviceUtils.isTvDevice(),
                preferredInterval = Constants.OverlayService.UPDATE_INTERVAL_MS
            )
                
                // Apply new interval if changed significantly
                if (optimalInterval != currentUpdateInterval) {
                    val oldInterval = currentUpdateInterval
                    currentUpdateInterval = optimalInterval
                    
                    Timber.tag(TAG_SERVICE).i(
                        "🔄 Adaptive: Changed interval %dms → %dms (CPU: %.1f%%, RAM: %.1f%%)",
                        oldInterval, optimalInterval, cpuUsage, ramPercent
                    )
                }
            } catch (e: Exception) {
                Timber.tag(TAG_SERVICE).w(e, "Error in adaptive monitoring")
            }
        }
    }

}
