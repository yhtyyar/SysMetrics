package com.sysmetrics.app.utils

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import timber.log.Timber

/**
 * Battery-aware performance monitor.
 * Automatically adjusts update intervals based on battery level and charging state.
 * 
 * Optimization strategy:
 * - Battery < 20%: Reduce update frequency to 3000ms
 * - Battery < 50%: Moderate frequency 2000ms
 * - Battery > 50% or Charging: Normal frequency 1000ms
 */
class BatteryAwareMonitor(private val context: Context) {
    
    companion object {
        private const val TAG = "BATTERY_MONITOR"
        
        // Update intervals based on battery state
        const val INTERVAL_CRITICAL = 3000L  // Low battery
        const val INTERVAL_MODERATE = 2000L  // Medium battery
        const val INTERVAL_NORMAL = 1000L    // Good battery or charging
        
        const val BATTERY_LOW_THRESHOLD = 20
        const val BATTERY_MEDIUM_THRESHOLD = 50
    }
    
    /**
     * Get optimal update interval based on current battery state.
     */
    fun getOptimalInterval(): Long {
        val batteryStatus = getBatteryStatus()
        val batteryLevel = batteryStatus.level
        val isCharging = batteryStatus.isCharging
        
        return when {
            isCharging -> {
                Timber.tag(TAG).v("Device charging - using normal interval")
                INTERVAL_NORMAL
            }
            batteryLevel < BATTERY_LOW_THRESHOLD -> {
                Timber.tag(TAG).i("⚠️ Low battery ($batteryLevel%) - using critical interval")
                INTERVAL_CRITICAL
            }
            batteryLevel < BATTERY_MEDIUM_THRESHOLD -> {
                Timber.tag(TAG).v("Medium battery ($batteryLevel%) - using moderate interval")
                INTERVAL_MODERATE
            }
            else -> {
                Timber.tag(TAG).v("Good battery ($batteryLevel%) - using normal interval")
                INTERVAL_NORMAL
            }
        }
    }
    
    /**
     * Check if device should reduce performance for battery saving.
     */
    fun shouldReducePerformance(): Boolean {
        val batteryStatus = getBatteryStatus()
        return batteryStatus.level < BATTERY_LOW_THRESHOLD && !batteryStatus.isCharging
    }
    
    /**
     * Get current battery status.
     */
    fun getBatteryStatus(): BatteryStatus {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        
        val level = batteryIntent?.let {
            val rawLevel = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (rawLevel >= 0 && scale > 0) {
                (rawLevel * 100 / scale)
            } else {
                100 // Default to full if unable to read
            }
        } ?: 100
        
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL
        
        val plugged = batteryIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        val chargingSource = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> ChargingSource.AC
            BatteryManager.BATTERY_PLUGGED_USB -> ChargingSource.USB
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> ChargingSource.WIRELESS
            else -> ChargingSource.NONE
        }
        
        return BatteryStatus(level, isCharging, chargingSource)
    }
    
    /**
     * Log current battery status.
     */
    fun logBatteryStatus() {
        val status = getBatteryStatus()
        val interval = getOptimalInterval()
        
        Timber.tag(TAG).i("""
            Battery Status:
            - Level: ${status.level}%
            - Charging: ${status.isCharging} (${status.chargingSource})
            - Optimal Interval: ${interval}ms
        """.trimIndent())
    }
    
    /**
     * Battery status data class.
     */
    data class BatteryStatus(
        val level: Int,
        val isCharging: Boolean,
        val chargingSource: ChargingSource
    )
    
    /**
     * Charging source enum.
     */
    enum class ChargingSource {
        NONE, AC, USB, WIRELESS
    }
}
