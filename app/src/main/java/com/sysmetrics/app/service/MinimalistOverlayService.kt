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
import com.sysmetrics.app.core.SysMetricsApplication
import com.sysmetrics.app.core.common.Constants
import com.sysmetrics.app.data.source.PreferencesDataSource
import com.sysmetrics.app.data.source.SystemDataSource
import com.sysmetrics.app.domain.collector.IMetricsCollector
import com.sysmetrics.app.domain.collector.IProcessStatsCollector
import com.sysmetrics.app.domain.formatter.IStringFormatter
import com.sysmetrics.app.utils.AdaptivePerformanceMonitor
import com.sysmetrics.app.utils.DeviceUtils
import com.sysmetrics.app.utils.DraggableOverlayTouchListener
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
// @AndroidEntryPoint
class MinimalistOverlayService : LifecycleService() {
    
    companion object {
        // Logging tags for easy filtering
        private const val TAG_SERVICE = "OVERLAY_SERVICE"
        private const val TAG_UPDATE = "OVERLAY_UPDATE"
        private const val TAG_DISPLAY = "OVERLAY_DISPLAY"
        private const val TAG_SETTINGS = "OVERLAY_SETTINGS"
    }

    private lateinit var systemDataSource: SystemDataSource
    private lateinit var deviceUtils: DeviceUtils
    private lateinit var metricsCollector: IMetricsCollector
    private lateinit var processStatsCollector: IProcessStatsCollector
    private lateinit var adaptiveMonitor: AdaptivePerformanceMonitor
    private lateinit var preferencesDataSource: PreferencesDataSource
    private lateinit var stringFormatter: IStringFormatter

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
    private lateinit var timeText: TextView

    private var currentConfig: com.sysmetrics.app.data.model.OverlayConfig = com.sysmetrics.app.data.model.OverlayConfig.DEFAULT
    private var isBaselineInitialized = false
    // Time formatters for better performance
    private val timeFormat24h = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val timeFormat12h = SimpleDateFormat("hh:mm a", Locale.getDefault())

    override fun onCreate() {
        super.onCreate()
        Timber.tag(TAG_SERVICE).i("✅ MinimalistOverlayService created")
        
        // Initialize dependencies from AppContainer
        val appContainer = (application as SysMetricsApplication).appContainer
        deviceUtils = appContainer.deviceUtils
        metricsCollector = appContainer.metricsCollector
        processStatsCollector = appContainer.processStatsCollector
        adaptiveMonitor = appContainer.adaptivePerformanceMonitor
        stringFormatter = appContainer.stringFormatter
        
        // Create data sources directly (they are private in AppContainer)
        systemDataSource = SystemDataSource(com.sysmetrics.app.core.di.DefaultDispatcherProvider())
        preferencesDataSource = PreferencesDataSource(this)
        
        // Setup exception handler for TV-specific crashes
        setupExceptionHandler()

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        Timber.tag(TAG_SERVICE).d("📦 Dependencies initialized from AppContainer")
        
        // Log device capabilities
        deviceUtils.logDeviceCapabilities()
        
        // Set optimal initial interval
        currentUpdateInterval = deviceUtils.getOptimalUpdateInterval()
        Timber.tag(TAG_SERVICE).d("Initial update interval: ${currentUpdateInterval}ms")

        loadSettings()
        createNotificationChannel()
        startForeground(Constants.OverlayService.NOTIFICATION_ID, createNotification())
        createOverlayView()
        
        // Observe config changes and apply them in real-time
        lifecycleScope.launch {
            preferencesDataSource.overlayConfig.collect { config ->
                currentConfig = config
                
                // Update time visibility
                if (::timeText.isInitialized) {
                    timeText.visibility = if (config.showTime) android.view.View.VISIBLE else android.view.View.GONE
                }
                
                // Update overlay position in real-time
                if (::overlayView.isInitialized && ::layoutParams.isInitialized) {
                    updateOverlayPosition(config)
                }
            }
        }
        
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
     */
    private fun loadSettings() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        
        // Apply overlay opacity if overlayView is already created
        val opacity = prefs.getInt("overlay_opacity", Constants.OverlayService.DEFAULT_OPACITY_PERCENT)
        if (::overlayView.isInitialized) {
            overlayView.alpha = opacity / 100f
        }
        
        Timber.tag(TAG_SETTINGS).i("⚙️ Settings loaded: opacity=%d", opacity)
    }
    
    /**
     * Load config and apply visibility settings
     */
    private fun loadConfigAndApplySettings() {
        lifecycleScope.launch {
            preferencesDataSource.overlayConfig.collect { config ->
                currentConfig = config
                if (::timeText.isInitialized) {
                    timeText.visibility = if (config.showTime) android.view.View.VISIBLE else android.view.View.GONE
                }
            }
        }
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
            .setSmallIcon(R.drawable.ic_cpu)
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
            timeText = overlayView.findViewById(R.id.time_text)

            // Verify all views are found and log status
            Timber.tag(TAG_SERVICE).d("📋 View references: CPU=%b, RAM=%b, Self=%b, Time=%b",
                ::cpuText.isInitialized, ::ramText.isInitialized,
                ::selfStatsText.isInitialized, ::timeText.isInitialized)
            
            // Load config and apply visibility
            loadConfigAndApplySettings()

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
     * 
     * Initial position will be set by updateOverlayPosition() after view is added
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
            
            // Initial position - will be updated by updateOverlayPosition()
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
    /**
     * Enable dragging for mobile devices.
     */
    private fun enableDragging() {
        if (!::overlayView.isInitialized) return
        
        if (deviceUtils.shouldEnableDragging()) {
            val dragListener = DraggableOverlayTouchListener(
                params = layoutParams,
                windowManager = windowManager,
                onPositionChanged = { x, y ->
                    Timber.tag(TAG_SERVICE).d("Overlay dragged to: ($x, $y)")
                    lifecycleScope.launch {
                        try {
                            preferencesDataSource.updatePosition(x, y)
                            Timber.tag(TAG_SERVICE).i("✅ Position saved: ($x, $y)")
                        } catch (e: Exception) {
                            Timber.tag(TAG_SERVICE).e(e, "Failed to save position")
                        }
                    }
                }
            )
            
            overlayView.setOnTouchListener(dragListener)
            Timber.tag(TAG_SERVICE).i("✅ Dragging enabled for mobile device")
        } else {
            Timber.tag(TAG_SERVICE).i("ℹ️ Dragging disabled (TV or no touchscreen)")
        }
    }
    
    /**
     * Update overlay position based on configuration.
     * Applies changes in real-time when settings are changed.
     * 
     * Uses proper Gravity-based positioning for WindowManager:
     * - x, y are offsets from the gravity point (not absolute coordinates)
     * - Positive values move away from edges, negative values move toward edges
     */
    private fun updateOverlayPosition(config: com.sysmetrics.app.data.model.OverlayConfig) {
        try {
            val margin = dpToPx(deviceUtils.getOverlayMargin())
            
            // Check if custom position (from dragging)
            val isCustomPosition = config.positionX != 20 || config.positionY != 20
            
            if (isCustomPosition) {
                // Custom dragged position - use TOP|START gravity with absolute coordinates
                layoutParams.gravity = Gravity.TOP or Gravity.START
                layoutParams.x = config.positionX
                layoutParams.y = config.positionY
            } else {
                // Predefined position - use appropriate gravity for each corner
                when (config.position) {
                    com.sysmetrics.app.data.model.OverlayPosition.TOP_LEFT -> {
                        layoutParams.gravity = Gravity.TOP or Gravity.START
                        layoutParams.x = margin
                        layoutParams.y = margin
                    }
                    com.sysmetrics.app.data.model.OverlayPosition.TOP_RIGHT -> {
                        layoutParams.gravity = Gravity.TOP or Gravity.END
                        layoutParams.x = margin  // Offset from RIGHT edge
                        layoutParams.y = margin
                    }
                    com.sysmetrics.app.data.model.OverlayPosition.BOTTOM_LEFT -> {
                        layoutParams.gravity = Gravity.BOTTOM or Gravity.START
                        layoutParams.x = margin
                        layoutParams.y = margin  // Offset from BOTTOM edge
                    }
                    com.sysmetrics.app.data.model.OverlayPosition.BOTTOM_RIGHT -> {
                        layoutParams.gravity = Gravity.BOTTOM or Gravity.END
                        layoutParams.x = margin  // Offset from RIGHT edge
                        layoutParams.y = margin  // Offset from BOTTOM edge
                    }
                }
            }
            
            // Apply changes
            windowManager.updateViewLayout(overlayView, layoutParams)
            
            Timber.tag(TAG_SERVICE).d("📍 Overlay position updated: gravity=${layoutParams.gravity}, x=${layoutParams.x}, y=${layoutParams.y}")
        } catch (e: Exception) {
            Timber.tag(TAG_SERVICE).e(e, "Failed to update overlay position")
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
    
    private suspend fun updateUI(cpuPercent: Float, usedMb: Long, totalMb: Long, ramPercent: Float) {
        // Update CPU with color indicator - use optimized string formatting
        val cpuDisplay = stringFormatter.formatCpu(cpuPercent)
        cpuText.text = cpuDisplay
        cpuText.setTextColor(getColorForValue(cpuPercent))

        val cpuColor = when {
            cpuPercent < 50 -> "GREEN"
            cpuPercent < 80 -> "YELLOW"
            else -> "RED"
        }
        Timber.tag(TAG_DISPLAY).d("📺 CPU on SCREEN: '%s' color=%s", cpuDisplay, cpuColor)

        // Update RAM with color indicator - use optimized string formatting
        val ramDisplay = stringFormatter.formatRam(usedMb, totalMb)
        ramText.text = ramDisplay
        ramText.setTextColor(getColorForValue(ramPercent))

        Timber.tag(TAG_DISPLAY).d("📺 RAM on SCREEN: '%s' (%.1f%%)", ramDisplay, ramPercent)

        // SysMetrics self stats with color (compact format) - use optimized string formatting
        val selfStats = processStatsCollector.getSelfStats()
        val selfDisplay = stringFormatter.formatSelfStats(selfStats.cpuPercent, selfStats.ramMb)
        selfStatsText.text = selfDisplay
        selfStatsText.setTextColor(getColorForValue(selfStats.cpuPercent))

        Timber.tag(TAG_DISPLAY).d("📺 SELF on SCREEN: '%s'", selfDisplay)

        // Update time display - always show time
        val calendar = java.util.Calendar.getInstance()
        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val minute = calendar.get(java.util.Calendar.MINUTE)

        val currentTime = timeFormat24h.format(Date())
        timeText.text = currentTime
        Timber.tag(TAG_DISPLAY).v("🕒 TIME on SCREEN: '%s'", currentTime)
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
