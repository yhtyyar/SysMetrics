package com.sysmetrics.app.ui.components

import android.graphics.Color

/**
 * Helper object for determining metric colors based on usage levels.
 * Implements smart color-coding: Green → Yellow → Orange → Red
 * 
 * Usage:
 * - 🟢 Green: Healthy, low usage
 * - 🟡 Yellow: Normal, moderate usage
 * - 🟠 Orange: Warning, high usage
 * - 🔴 Red: Critical, very high usage
 */
object MetricColorHelper {
    
    // Color definitions
    val COLOR_GREEN = Color.parseColor("#4CAF50")    // 🟢 Healthy
    val COLOR_YELLOW = Color.parseColor("#FFC107")   // 🟡 Normal
    val COLOR_ORANGE = Color.parseColor("#FF9800")   // 🟠 Warning
    val COLOR_RED = Color.parseColor("#F44336")      // 🔴 Critical
    val COLOR_GRAY = Color.parseColor("#9E9E9E")     // ⚪ N/A
    
    /**
     * Get color for CPU usage percentage.
     * 
     * @param usage CPU usage (0-100%)
     * @return Color int
     */
    fun getCpuColor(usage: Float): Int = when {
        usage < 0f -> COLOR_GRAY
        usage < 20f -> COLOR_GREEN        // 🟢 0-20%
        usage < 40f -> COLOR_YELLOW       // 🟡 20-40%
        usage < 70f -> COLOR_ORANGE       // 🟠 40-70%
        else -> COLOR_RED                 // 🔴 70-100%
    }
    
    /**
     * Get color for RAM usage percentage.
     * 
     * @param usagePercent RAM usage (0-100%)
     * @return Color int
     */
    fun getRamColor(usagePercent: Float): Int = when {
        usagePercent < 0f -> COLOR_GRAY
        usagePercent < 50f -> COLOR_GREEN        // 🟢 0-50%
        usagePercent < 70f -> COLOR_YELLOW       // 🟡 50-70%
        usagePercent < 85f -> COLOR_ORANGE       // 🟠 70-85%
        else -> COLOR_RED                        // 🔴 85-100%
    }
    
    /**
     * Get color for GPU usage percentage.
     * 
     * @param usage GPU usage (0-100%)
     * @return Color int
     */
    fun getGpuColor(usage: Float): Int = when {
        usage < 0f -> COLOR_GRAY
        usage < 30f -> COLOR_GREEN        // 🟢 0-30%
        usage < 50f -> COLOR_YELLOW       // 🟡 30-50%
        usage < 75f -> COLOR_ORANGE       // 🟠 50-75%
        else -> COLOR_RED                 // 🔴 75-100%
    }
    
    /**
     * Get color for temperature in Celsius.
     * 
     * @param celsius Temperature in degrees Celsius
     * @return Color int
     */
    fun getTemperatureColor(celsius: Float): Int = when {
        celsius <= 0f -> COLOR_GRAY
        celsius < 45f -> COLOR_GREEN        // 🟢 0-45°C Cool
        celsius < 60f -> COLOR_YELLOW       // 🟡 45-60°C Warm
        celsius < 75f -> COLOR_ORANGE       // 🟠 60-75°C Hot
        else -> COLOR_RED                   // 🔴 75+°C Critical
    }
    
    /**
     * Get color for process RAM usage in MB.
     * 
     * @param ramMb RAM usage in megabytes
     * @return Color int
     */
    fun getProcessRamColor(ramMb: Long): Int = when {
        ramMb < 0 -> COLOR_GRAY
        ramMb < 100 -> COLOR_GREEN          // 🟢 0-100 MB
        ramMb < 200 -> COLOR_YELLOW         // 🟡 100-200 MB
        ramMb < 500 -> COLOR_ORANGE         // 🟠 200-500 MB
        else -> COLOR_RED                   // 🔴 500+ MB
    }
    
    /**
     * Get color for network speed in KB/s.
     * 
     * @param speedKbps Speed in kilobytes per second
     * @return Color int
     */
    fun getNetworkSpeedColor(speedKbps: Float): Int = when {
        speedKbps < 0f -> COLOR_GRAY
        speedKbps < 100f -> COLOR_GREEN     // 🟢 0-100 KB/s
        speedKbps < 1024f -> COLOR_YELLOW   // 🟡 100-1024 KB/s (1 MB/s)
        speedKbps < 5120f -> COLOR_ORANGE   // 🟠 1-5 MB/s
        else -> COLOR_RED                   // 🔴 5+ MB/s (High traffic)
    }
    
    /**
     * Get color for battery percentage.
     * 
     * @param percent Battery percentage (0-100%)
     * @param isCharging Whether battery is charging
     * @return Color int
     */
    fun getBatteryColor(percent: Int, isCharging: Boolean): Int = when {
        percent < 0 -> COLOR_GRAY
        isCharging -> COLOR_GREEN           // 🟢 Charging
        percent > 60 -> COLOR_GREEN         // 🟢 60-100%
        percent > 30 -> COLOR_YELLOW        // 🟡 30-60%
        percent > 15 -> COLOR_ORANGE        // 🟠 15-30%
        else -> COLOR_RED                   // 🔴 0-15%
    }
    
    /**
     * Get emoji indicator for CPU usage.
     */
    fun getCpuEmoji(usage: Float): String = when {
        usage < 20f -> "🟢"
        usage < 40f -> "🟡"
        usage < 70f -> "🟠"
        else -> "🔴"
    }
    
    /**
     * Get emoji indicator for RAM usage.
     */
    fun getRamEmoji(usagePercent: Float): String = when {
        usagePercent < 50f -> "🟢"
        usagePercent < 70f -> "🟡"
        usagePercent < 85f -> "🟠"
        else -> "🔴"
    }
    
    /**
     * Get emoji indicator for temperature.
     */
    fun getTemperatureEmoji(celsius: Float): String = when {
        celsius <= 0f -> "❄️"
        celsius < 45f -> "🟢"
        celsius < 60f -> "🟡"
        celsius < 75f -> "🟠"
        else -> "🔴"
    }
    
    /**
     * Get overall system health emoji based on multiple metrics.
     */
    fun getSystemHealthEmoji(
        cpuUsage: Float,
        ramUsagePercent: Float,
        temperature: Float
    ): String {
        val cpuScore = when {
            cpuUsage < 20f -> 0
            cpuUsage < 40f -> 1
            cpuUsage < 70f -> 2
            else -> 3
        }
        
        val ramScore = when {
            ramUsagePercent < 50f -> 0
            ramUsagePercent < 70f -> 1
            ramUsagePercent < 85f -> 2
            else -> 3
        }
        
        val tempScore = if (temperature > 0f) {
            when {
                temperature < 45f -> 0
                temperature < 60f -> 1
                temperature < 75f -> 2
                else -> 3
            }
        } else 0
        
        val totalScore = cpuScore + ramScore + tempScore
        
        return when {
            totalScore == 0 -> "🟢 EXCELLENT"
            totalScore <= 2 -> "🟢 HEALTHY"
            totalScore <= 4 -> "🟡 NORMAL"
            totalScore <= 6 -> "🟠 WARNING"
            else -> "🔴 CRITICAL"
        }
    }
    
    /**
     * Format color to hex string for debugging.
     */
    fun colorToHex(color: Int): String {
        return String.format("#%06X", 0xFFFFFF and color)
    }
}
