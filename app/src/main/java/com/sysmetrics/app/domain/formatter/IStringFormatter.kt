package com.sysmetrics.app.domain.formatter

/**
 * Interface for string formatting operations.
 * Provides optimized formatting for UI display.
 */
interface IStringFormatter {

    /**
     * Format time display string in 24-hour format.
     * @param hour hour (0-23)
     * @param minute minute (0-59)
     * @return formatted time string (HH:mm)
     */
    fun formatTime(hour: Int, minute: Int): String

    /**
     * Format CPU usage display string.
     * @param cpuPercent CPU usage percentage (0-100)
     * @return formatted CPU string
     */
    fun formatCpu(cpuPercent: Float): String

    /**
     * Format RAM usage display string.
     * @param usedMb used RAM in MB
     * @param totalMb total RAM in MB
     * @return formatted RAM string
     */
    fun formatRam(usedMb: Long, totalMb: Long): String

    /**
     * Format self stats display string.
     * @param cpuPercent CPU usage percentage
     * @param ramMb RAM usage in MB
     * @return formatted self stats string
     */
    fun formatSelfStats(cpuPercent: Float, ramMb: Long): String

    /**
     * Check if native formatting is available.
     */
    fun isNativeAvailable(): Boolean
}
