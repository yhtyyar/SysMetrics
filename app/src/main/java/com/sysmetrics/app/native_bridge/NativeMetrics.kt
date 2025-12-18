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
     * Get process CPU stats using native code (optimized).
     * @return ProcessCpuData with utime, stime, total_time or null if failed
     */
    fun getProcessCpuStatsNative(pid: Int): ProcessCpuData? {
        if (!isLoaded) return null

        return runCatching {
            val stats = getProcessCpuStats(pid)
            if (stats != null && stats.size >= 3) {
                ProcessCpuData(
                    utime = stats[0],
                    stime = stats[1],
                    totalTime = stats[2]
                )
            } else {
                null
            }
        }.getOrNull()
    }

    /**
     * Format time string using native code (optimized).
     */
    fun formatTimeNative(hour: Int, minute: Int, use24h: Boolean): String {
        return if (isLoaded) {
            runCatching { formatTimeString(hour, minute, use24h) }.getOrDefault("$hour:$minute")
        } else {
            if (use24h) String.format("%02d:%02d", hour, minute)
            else String.format("%d:%02d %s", if (hour % 12 == 0) 12 else hour % 12, minute, if (hour >= 12) "PM" else "AM")
        }
    }

    /**
     * Format CPU string using native code.
     */
    fun formatCpuNative(cpuPercent: Float): String {
        return if (isLoaded) {
            runCatching { formatCpuString(cpuPercent) }.getOrDefault(String.format("CPU: %.1f%%", cpuPercent))
        } else {
            String.format("CPU: %.1f%%", cpuPercent)
        }
    }

    /**
     * Format RAM string using native code.
     */
    fun formatRamNative(usedMb: Long, totalMb: Long): String {
        return if (isLoaded) {
            runCatching { formatRamString(usedMb, totalMb) }.getOrDefault(String.format("RAM: %d/%d MB", usedMb, totalMb))
        } else {
            String.format("RAM: %d/%d MB", usedMb, totalMb)
        }
    }

    /**
     * Format self stats string using native code.
     */
    fun formatSelfStatsNative(cpuPercent: Float, ramMb: Long): String {
        return if (isLoaded) {
            runCatching { formatSelfStatsString(cpuPercent, ramMb) }.getOrDefault(String.format("Self: %.1f%% / %dM", cpuPercent, ramMb))
        } else {
            String.format("Self: %.1f%% / %dM", cpuPercent, ramMb)
        }
    }

    // Native method declarations
    private external fun getCpuUsage(): Float
    private external fun resetCpuBaseline()
    private external fun getMemoryStats(): FloatArray?
    private external fun getTemperature(): Float
    private external fun isAvailable(): Boolean
    private external fun getCpuCoreCount(): Int
    private external fun getProcessCpuStats(pid: Int): LongArray?
    private external fun formatTimeString(hour: Int, minute: Int, use24h: Boolean): String
    private external fun formatCpuString(cpuPercent: Float): String
    private external fun formatRamString(usedMb: Long, totalMb: Long): String
    private external fun formatSelfStatsString(cpuPercent: Float, ramMb: Long): String

    /**
     * Data class for memory statistics.
     */
    data class MemoryData(
        val totalMb: Float,
        val usedMb: Float,
        val availableMb: Float,
        val usagePercent: Float
    )

    /**
     * Data class for process CPU statistics.
     */
    data class ProcessCpuData(
        val utime: Long,
        val stime: Long,
        val totalTime: Long
    )
}
