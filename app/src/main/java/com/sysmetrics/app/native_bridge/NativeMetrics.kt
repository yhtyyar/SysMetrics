package com.sysmetrics.app.native_bridge

import timber.log.Timber

/**
 * Kotlin bridge for native C++ metrics collection.
 * Provides high-performance system metrics using JNI.
 * 
 * Native implementation is ~5-10x faster than Kotlin file parsing
 * for high-frequency updates (>2 updates/second).
 */
object NativeMetrics {

    private var isLoaded = false

    init {
        try {
            System.loadLibrary("sysmetrics_native")
            isLoaded = true
            Timber.i("Native library loaded successfully")
        } catch (e: UnsatisfiedLinkError) {
            Timber.e(e, "Failed to load native library")
            isLoaded = false
        }
    }

    /**
     * Check if native library is available.
     */
    fun isNativeAvailable(): Boolean {
        return isLoaded && runCatching { isAvailable() }.getOrDefault(false)
    }

    /**
     * Get current CPU usage percentage using native code.
     * @return CPU usage (0-100), or -1 if unavailable
     */
    fun getCpuUsageNative(): Float {
        return if (isLoaded) {
            runCatching { getCpuUsage() }.getOrDefault(-1f)
        } else {
            -1f
        }
    }

    /**
     * Reset CPU baseline for accurate measurements.
     */
    fun resetCpuBaselineNative() {
        if (isLoaded) {
            runCatching { resetCpuBaseline() }
        }
    }

    /**
     * Get memory statistics using native code.
     * @return MemoryData with totalMb, usedMb, availableMb, usagePercent
     */
    fun getMemoryStatsNative(): MemoryData? {
        if (!isLoaded) return null

        return runCatching {
            val stats = getMemoryStats()
            if (stats != null && stats.size >= 4) {
                MemoryData(
                    totalMb = stats[0],
                    usedMb = stats[1],
                    availableMb = stats[2],
                    usagePercent = stats[3]
                )
            } else {
                null
            }
        }.getOrNull()
    }

    /**
     * Get CPU temperature using native code.
     * @return Temperature in Celsius, or -1 if unavailable
     */
    fun getTemperatureNative(): Float {
        return if (isLoaded) {
            runCatching { getTemperature() }.getOrDefault(-1f)
        } else {
            -1f
        }
    }

    /**
     * Get number of CPU cores using native code.
     */
    fun getCpuCoreCountNative(): Int {
        return if (isLoaded) {
            runCatching { getCpuCoreCount() }.getOrDefault(-1)
        } else {
            -1
        }
    }

    // Native method declarations
    private external fun getCpuUsage(): Float
    private external fun resetCpuBaseline()
    private external fun getMemoryStats(): FloatArray?
    private external fun getTemperature(): Float
    private external fun isAvailable(): Boolean
    private external fun getCpuCoreCount(): Int

    /**
     * Data class for memory statistics.
     */
    data class MemoryData(
        val totalMb: Float,
        val usedMb: Float,
        val availableMb: Float,
        val usagePercent: Float
    )
}
