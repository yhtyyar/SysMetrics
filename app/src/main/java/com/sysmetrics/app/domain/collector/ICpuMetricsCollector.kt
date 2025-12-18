package com.sysmetrics.app.domain.collector

/**
 * Interface for CPU metrics collection.
 * Supports both native and fallback implementations.
 */
interface ICpuMetricsCollector {

    /**
     * Get current system CPU usage percentage.
     * @return CPU usage (0-100), or -1 if unavailable
     */
    fun getCpuUsage(): Float

    /**
     * Reset CPU baseline for accurate delta measurements.
     */
    fun resetBaseline()

    /**
     * Get CPU core count.
     * @return number of cores, or -1 if unavailable
     */
    fun getCoreCount(): Int

    /**
     * Get process CPU stats for specific PID.
     * @param pid Process ID
     * @return ProcessCpuData or null if unavailable
     */
    fun getProcessCpuStatsNative(pid: Int): ProcessCpuData?

    /**
     * Check if native implementation is available.
     */
    fun isNativeAvailable(): Boolean

    /**
     * Data class for process CPU statistics
     */
    data class ProcessCpuData(
        val utime: Long,
        val stime: Long,
        val totalTime: Long
    )
}
