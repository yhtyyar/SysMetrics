package com.sysmetrics.app.data.model

/**
 * Battery information.
 * Useful for Android TV boxes with backup batteries.
 */
data class BatteryInfo(
    val percent: Int = -1,
    val isCharging: Boolean = false,
    val temperatureCelsius: Float = 0f,
    val voltage: Int = 0,
    val isAvailable: Boolean = false
) {
    /**
     * Returns display-friendly battery status.
     */
    fun getStatusText(): String {
        return when {
            !isAvailable -> "N/A"
            isCharging -> "⚡ $percent%"
            percent >= 0 -> "$percent%"
            else -> "N/A"
        }
    }

    companion object {
        val EMPTY = BatteryInfo()
        val UNAVAILABLE = BatteryInfo(isAvailable = false)
    }
}
