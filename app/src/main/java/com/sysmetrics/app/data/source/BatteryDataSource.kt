package com.sysmetrics.app.data.source

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.sysmetrics.app.core.di.DispatcherProvider
import com.sysmetrics.app.data.model.BatteryInfo
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Data source for battery information.
 * Useful for Android TV boxes with backup batteries.
 */

class BatteryDataSource constructor(
    private val context: Context,
    private val dispatcherProvider: DispatcherProvider
) {
    companion object {
        private const val TAG = "BATTERY_DATA"
        private const val CACHE_DURATION_MS = 2000L // Battery updates are infrequent
    }

    private var cachedBatteryInfo: BatteryInfo? = null
    private var cacheTimestamp: Long = 0L

    /**
     * Reads current battery information.
     */
    suspend fun readBatteryInfo(): BatteryInfo = withContext(dispatcherProvider.io) {
        val now = System.currentTimeMillis()
        
        // Return cached value if still valid
        cachedBatteryInfo?.let { cached ->
            if (now - cacheTimestamp < CACHE_DURATION_MS) {
                return@withContext cached
            }
        }
        
        try {
            val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = context.registerReceiver(null, intentFilter)
            
            if (batteryStatus == null) {
                Timber.tag(TAG).d("Battery info not available")
                return@withContext BatteryInfo.UNAVAILABLE
            }

            // Get battery level
            val level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val percent = if (level >= 0 && scale > 0) {
                (level.toFloat() / scale.toFloat() * 100).toInt().coerceIn(0, 100)
            } else -1

            // Check if device has a battery
            val hasBattery = batteryStatus.getIntExtra(BatteryManager.EXTRA_PRESENT, 0) == 1
            
            if (!hasBattery || percent < 0) {
                Timber.tag(TAG).v("No battery present or invalid level")
                return@withContext BatteryInfo.UNAVAILABLE
            }

            // Get charging status
            val status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                           status == BatteryManager.BATTERY_STATUS_FULL

            // Get temperature (in tenths of degrees Celsius)
            val temperature = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
                .let { it / 10f }

            // Get voltage (in millivolts)
            val voltage = batteryStatus.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)

            val info = BatteryInfo(
                percent = percent,
                isCharging = isCharging,
                temperatureCelsius = temperature,
                voltage = voltage,
                isAvailable = true
            )

            // Update cache
            cachedBatteryInfo = info
            cacheTimestamp = now

            Timber.tag(TAG).v("Battery: %d%%, Charging: %b, Temp: %.1f°C",
                percent, isCharging, temperature)

            info
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error reading battery info")
            BatteryInfo.UNAVAILABLE
        }
    }

    /**
     * Clears cached battery info.
     */
    fun clearCache() {
        cachedBatteryInfo = null
        cacheTimestamp = 0L
    }
}
