package com.sysmetrics.app.utils

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.PowerManager
import com.sysmetrics.app.core.common.Constants
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility class for device-specific operations and optimizations.
 * Provides TV detection, device capabilities, and optimization settings.
 * 
 * IMPROVEMENTS:
 * - Uses centralized constants
 * - Better documentation
 * - Optimized for testability
 */
@Singleton
class DeviceUtils @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "DEVICE_UTILS"
    }
    
    private val powerManager by lazy { 
        context.getSystemService(Context.POWER_SERVICE) as? PowerManager 
    }
    
    private val uiModeManager by lazy {
        context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
    }
    
    /**
     * Check if device is Android TV.
     */
    fun isTvDevice(): Boolean {
        val uiMode = uiModeManager?.currentModeType ?: Configuration.UI_MODE_TYPE_NORMAL
        val isTv = uiMode == Configuration.UI_MODE_TYPE_TELEVISION
        
        if (isTv) {
            Timber.tag(TAG).d("Device is Android TV")
        }
        
        return isTv
    }
    
    /**
     * Check if device has touchscreen (mobile vs TV/box).
     */
    fun hasTouchScreen(): Boolean {
        return context.packageManager.hasSystemFeature("android.hardware.touchscreen")
    }
    
    /**
     * Check if device is in power saving mode.
     */
    fun isPowerSaveMode(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            powerManager?.isPowerSaveMode == true
        } else {
            false
        }
    }
    
    /**
     * Get optimal margin for overlay based on device type.
     * TV devices need larger margins for safe zones.
     */
    fun getOverlayMargin(): Int {
        return if (isTvDevice()) {
            Constants.OverlayService.TV_MARGIN_DP
        } else {
            Constants.OverlayService.MOBILE_MARGIN_DP
        }
    }
    
    /**
     * Get optimal update interval based on device type.
     */
    fun getOptimalUpdateInterval(): Long {
        return when {
            isTvDevice() -> 1000L        // TV: 1 second
            isPowerSaveMode() -> 2000L   // Power save: 2 seconds
            else -> 500L                  // Mobile: 500ms
        }
    }
    
    /**
     * Check if dragging should be enabled (mobile only).
     */
    fun shouldEnableDragging(): Boolean {
        return !isTvDevice() && hasTouchScreen()
    }
    
    /**
     * Check if adaptive performance should be used.
     */
    fun shouldUseAdaptivePerformance(): Boolean {
        // Use adaptive on TV or when power saving is enabled
        return isTvDevice() || isPowerSaveMode()
    }
    
    /**
     * Get device info for logging.
     */
    fun getDeviceInfo(): String {
        return buildString {
            append("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            append(", Android: ${Build.VERSION.RELEASE}")
            append(", TV: ${isTvDevice()}")
            append(", Touch: ${hasTouchScreen()}")
            append(", PowerSave: ${isPowerSaveMode()}")
        }
    }
    
    /**
     * Log device capabilities.
     */
    fun logDeviceCapabilities() {
        Timber.tag(TAG).i(getDeviceInfo())
        Timber.tag(TAG).d("Optimal update interval: ${getOptimalUpdateInterval()}ms")
        Timber.tag(TAG).d("Overlay margin: ${getOverlayMargin()}dp")
        Timber.tag(TAG).d("Dragging enabled: ${shouldEnableDragging()}")
        Timber.tag(TAG).d("Adaptive performance: ${shouldUseAdaptivePerformance()}")
    }
}
