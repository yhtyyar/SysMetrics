package com.sysmetrics.app.core.common

/**
 * Application-wide constants.
 */
object Constants {
    
    /**
     * System file paths for metrics collection.
     */
    object SystemPaths {
        const val PROC_STAT = "/proc/stat"
        const val PROC_MEMINFO = "/proc/meminfo"
        const val SYS_THERMAL = "/sys/class/thermal"
        const val THERMAL_ZONE_PREFIX = "thermal_zone"
        const val TEMP_FILE = "temp"
    }
    
    /**
     * Overlay configuration defaults and limits.
     */
    object Overlay {
        const val DEFAULT_WIDTH_DP = 220
        const val DEFAULT_HEIGHT_DP = 280
        const val DEFAULT_POSITION_X = 20
        const val DEFAULT_POSITION_Y = 20
        const val DEFAULT_OPACITY = 0.85f
        const val MIN_OPACITY = 0.3f
        const val MAX_OPACITY = 1.0f
    }
    
    /**
     * Update interval options in milliseconds.
     */
    object UpdateInterval {
        const val FAST = 500L
        const val NORMAL = 1000L
        const val SLOW = 2000L
        const val DEFAULT = NORMAL
    }
    
    /**
     * Cache configuration.
     */
    object Cache {
        const val CPU_CACHE_DURATION_MS = 100L
        const val MEMORY_CACHE_DURATION_MS = 200L
        const val TEMPERATURE_CACHE_DURATION_MS = 500L
    }
    
    /**
     * Temperature thresholds in Celsius.
     */
    object Temperature {
        const val NORMAL_MAX = 50f
        const val WARNING_MAX = 70f
        const val CRITICAL_MAX = 85f
        const val MILLIDEGREES_DIVISOR = 1000f
    }
    
    /**
     * Memory conversion constants.
     */
    object Memory {
        const val KB_TO_MB = 1024L
    }
}
