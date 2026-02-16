package com.sysmetrics.app.ui

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sysmetrics.app.R
import com.sysmetrics.app.core.common.Constants
import com.sysmetrics.app.data.model.OverlayConfig
import com.sysmetrics.app.databinding.ActivityMainOverlayBinding
import com.sysmetrics.app.core.SysMetricsApplication
import com.sysmetrics.app.service.MinimalistOverlayService
import com.sysmetrics.app.utils.MetricsCollector
import com.sysmetrics.app.data.source.network.NetworkStatsDataSource
import com.sysmetrics.app.data.source.SystemDataSource
import com.sysmetrics.app.core.di.DefaultDispatcherProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import timber.log.Timber
import com.sysmetrics.app.utils.DeviceUtils
import android.app.UiModeManager
import android.content.res.Configuration

/**
 * Main Activity - Optimized UX
 * - Large toggle button with color indication (Red=OFF, Green=ON)
 * - Real-time CPU and RAM metrics preview
 * - Clear status messages
 * Temperature monitoring removed for better performance
 */
// @AndroidEntryPoint
class MainActivityOverlay : AppCompatActivity() {

    private lateinit var binding: ActivityMainOverlayBinding
    private lateinit var metricsCollector: MetricsCollector
    private lateinit var networkStatsDataSource: NetworkStatsDataSource
    private lateinit var systemDataSource: SystemDataSource
    private lateinit var deviceUtils: DeviceUtils
    private var isOverlayActive = false
    
    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            if (isOverlayActive) {
                lifecycleScope.launch {
                    updateMetricsPreview()
                }
                handler.postDelayed(this, 1000)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainOverlayBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Initialize dependencies from AppContainer
        val appContainer = (application as SysMetricsApplication).appContainer
        metricsCollector = appContainer.metricsCollector
        deviceUtils = appContainer.deviceUtils
        
        // Initialize data sources
        val dispatcherProvider = DefaultDispatcherProvider()
        networkStatsDataSource = NetworkStatsDataSource(dispatcherProvider)
        systemDataSource = SystemDataSource(dispatcherProvider)
        
        // Log device info for debugging
        Timber.d("Device info: ${deviceUtils.getDeviceInfo()}")
        
        setupUI()
        checkServiceStatus()
    }

    override fun onResume() {
        super.onResume()
        checkServiceStatus()
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(updateRunnable)
    }

    private fun setupUI() {
        binding.apply {
            // Toggle Button Click
            btnToggleOverlay.setOnClickListener {
                handleToggleClick()
            }
            
            // Settings Button
            btnSettings.setOnClickListener {
                startActivity(Intent(this@MainActivityOverlay, SettingsActivity::class.java))
            }
            
            // Permission Info Click
            tvPermissionInfo.setOnClickListener {
                showPermissionInfo()
            }
        }
    }

    /**
     * Handle toggle button click
     */
    private fun handleToggleClick() {
        if (isOverlayActive) {
            // Stop overlay
            stopOverlay()
        } else {
            // Check permission first
            if (hasOverlayPermission()) {
                startOverlay()
            } else {
                requestOverlayPermission()
            }
        }
    }

    /**
     * Start overlay service
     */
    private fun startOverlay() {
        try {
            val serviceIntent = Intent(this, MinimalistOverlayService::class.java)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            
            isOverlayActive = true
            updateButtonState(true)
            showToast(R.string.overlay_started)
            
            // Start metrics preview updates
            handler.post(updateRunnable)
            
            Timber.d("Minimalist overlay service started")
        } catch (e: Exception) {
            Timber.e(e, "Failed to start overlay")
            android.widget.Toast.makeText(this, "Failed to start overlay: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Stop overlay service
     */
    private fun stopOverlay() {
        try {
            val serviceIntent = Intent(this, MinimalistOverlayService::class.java)
            stopService(serviceIntent)
            
            isOverlayActive = false
            updateButtonState(false)
            showToast(R.string.overlay_stopped)
            
            // Stop metrics preview updates
            handler.removeCallbacks(updateRunnable)
            
            Timber.d("Minimalist overlay service stopped")
        } catch (e: Exception) {
            Timber.e(e, "Failed to stop overlay")
            android.widget.Toast.makeText(this, "Failed to stop overlay: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Update button appearance based on state
     */
    private fun updateButtonState(isActive: Boolean) {
        binding.apply {
            if (isActive) {
                // Active state - Green
                btnToggleOverlay.apply {
                    text = getString(R.string.stop_overlay)
                    setIconResource(R.drawable.ic_stop)
                    backgroundTintList = ContextCompat.getColorStateList(context, R.color.button_toggle_on)
                    strokeColor = ContextCompat.getColorStateList(context, R.color.metric_success)
                }
                tvStatus.text = getString(R.string.overlay_status_on)
                tvStatus.setTextColor(getColor(R.color.metric_success))
                layoutMetricsPreview.visibility = View.VISIBLE
            } else {
                // Inactive state - Red
                btnToggleOverlay.apply {
                    text = getString(R.string.start_overlay)
                    setIconResource(R.drawable.ic_play)
                    backgroundTintList = ContextCompat.getColorStateList(context, R.color.button_toggle_off)
                    strokeColor = ContextCompat.getColorStateList(context, R.color.metric_error)
                }
                tvStatus.text = getString(R.string.overlay_status_off)
                tvStatus.setTextColor(getColor(R.color.text_secondary))
                layoutMetricsPreview.visibility = View.GONE
            }
            
            // Animate button
            btnToggleOverlay.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction {
                    btnToggleOverlay.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .start()
                }
                .start()
        }
    }

    /**
     * Update metrics preview display
     */
    private suspend fun updateMetricsPreview() {
        try {
            binding.apply {
                // CPU
                val cpuUsage = metricsCollector.getCpuUsage()
                tvCpuPreview.text = String.format("%.1f%%", cpuUsage)
                tvCpuPreview.setTextColor(getColorForValue(cpuUsage))
                
                // RAM
                val (usedMb, totalMb, ramPercent) = metricsCollector.getRamUsage()
                tvRamPreview.text = String.format("%d / %d MB", usedMb, totalMb)
                tvRamPreview.setTextColor(getColorForValue(ramPercent))
                
                // Temperature
                val tempInfo = systemDataSource.readTemperature()
                if (tempInfo.cpuTempCelsius > 0) {
                    tvTempPreview.text = String.format("%.0f°C", tempInfo.cpuTempCelsius)
                    tvTempPreview.setTextColor(getColorForTemperature(tempInfo.cpuTempCelsius))
                } else {
                    tvTempPreview.text = "N/A"
                    tvTempPreview.setTextColor(getColor(R.color.text_tertiary))
                }
                
                // Network
                val networkStats = networkStatsDataSource.readNetworkStats()
                val networkDisplay = formatNetworkSpeed(networkStats.ingressBytesPerSec, networkStats.egressBytesPerSec)
                tvNetworkPreview.text = networkDisplay
                tvNetworkPreview.setTextColor(getColor(R.color.metric_success))
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to update metrics preview")
        }
    }
    
    /**
     * Format network speed for display
     */
    private fun formatNetworkSpeed(ingressBytesPerSec: Long, egressBytesPerSec: Long): String {
        val ingressStr = formatBytesPerSec(ingressBytesPerSec)
        val egressStr = formatBytesPerSec(egressBytesPerSec)
        return "↓$ingressStr ↑$egressStr"
    }
    
    private fun formatBytesPerSec(bytesPerSec: Long): String {
        return when {
            bytesPerSec < 1024 -> "${bytesPerSec}B"
            bytesPerSec < 1024 * 1024 -> String.format("%.1fK", bytesPerSec / 1024f)
            else -> String.format("%.1fM", bytesPerSec / 1024f / 1024f)
        }
    }
    
    /**
     * Get color based on temperature value
     */
    private fun getColorForTemperature(tempCelsius: Float): Int {
        return when {
            tempCelsius < 45 -> getColor(R.color.metric_success)   // Green - cool
            tempCelsius < 60 -> getColor(R.color.metric_warning)   // Yellow - warm
            else -> getColor(R.color.metric_error)                  // Red - hot
        }
    }

    /**
     * Get color based on percentage value (0-100)
     */
    private fun getColorForValue(percent: Float): Int {
        return when {
            percent < 50 -> getColor(R.color.metric_success)   // Green
            percent < 80 -> getColor(R.color.metric_warning)   // Yellow
            else -> getColor(R.color.metric_error)             // Red
        }
    }


    /**
     * Request overlay permission from user
     * On Android TV, directly start overlay as permission is not required
     */
    private fun requestOverlayPermission() {
        // On TV devices, permission is automatically granted - just start overlay
        if (isTvDevice()) {
            Timber.i("TV device - starting overlay without permission request")
            startOverlay()
            return
        }
        
        // For mobile devices, show permission dialog
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.overlay_permission_required)
            .setMessage(R.string.overlay_permission_message)
            .setPositiveButton(R.string.grant_permission) { _, _ ->
                openOverlaySettings()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    /**
     * Open system overlay permission settings
     */
    private fun openOverlaySettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            } catch (e: android.content.ActivityNotFoundException) {
                Timber.w(e, "Overlay settings not available, opening app settings")
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } catch (e2: Exception) {
                    Timber.e(e2, "Failed to open app settings")
                    MaterialAlertDialogBuilder(this)
                        .setTitle("Settings Unavailable")
                        .setMessage("Unable to open settings. Please enable overlay permission manually in system settings.")
                        .setPositiveButton(android.R.string.ok, null)
                        .show()
                }
            }
        }
    }

    /**
     * Show permission information dialog
     */
    private fun showPermissionInfo() {
        val message = """
            SysMetrics requires overlay permission to display real-time metrics over other applications.
            
            This permission allows the app to show a floating window with:
            • System CPU usage
            • System RAM usage  
            • Top-3 apps by CPU and RAM
            
            The overlay is non-intrusive and can be stopped anytime.
        """.trimIndent()
        
        MaterialAlertDialogBuilder(this)
            .setTitle("About Overlay Permission")
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    /**
     * Check if service is already running.
     * Uses ActivityManager to query running services.
     */
    private fun checkServiceStatus() {
        val isRunning = isServiceRunning(MinimalistOverlayService::class.java)
        isOverlayActive = isRunning
        updateButtonState(isRunning)
        
        if (isRunning) {
            handler.post(updateRunnable)
        }
    }
    
    /**
     * Check if a service is currently running.
     */
    @Suppress("DEPRECATION")
    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager
        return manager.getRunningServices(Int.MAX_VALUE)
            .any { it.service.className == serviceClass.name }
    }

    /**
     * Check if overlay permission is granted
     * On Android TV, overlay permission is automatically granted and doesn't require user action
     */
    private fun hasOverlayPermission(): Boolean {
        // Android TV devices don't need overlay permission request
        if (isTvDevice()) {
            Timber.d("TV device detected - overlay permission automatically granted")
            return true
        }
        
        // For mobile devices, check standard permission
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }
    
    /**
     * Check if device is Android TV
     */
    private fun isTvDevice(): Boolean {
        val uiModeManager = getSystemService(UI_MODE_SERVICE) as? UiModeManager
        val uiMode = uiModeManager?.currentModeType ?: Configuration.UI_MODE_TYPE_NORMAL
        return uiMode == Configuration.UI_MODE_TYPE_TELEVISION
    }

    /**
     * Show toast message
     */
    private fun showToast(messageResId: Int) {
        android.widget.Toast.makeText(this, messageResId, android.widget.Toast.LENGTH_SHORT).show()
    }
}
