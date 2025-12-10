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
    
    /**
     * Overlay service configuration.
     */
    object OverlayService {
        const val CHANNEL_ID = "sysmetrics_minimalist"
        const val NOTIFICATION_ID = 2001
        const val UPDATE_INTERVAL_MS = 500L
        const val BASELINE_INIT_DELAY_MS = 1000L
        const val ADAPTIVE_CHECK_CYCLES = 10
        const val SLOW_UPDATE_THRESHOLD_MS = 100L
        
        // Default app settings
        const val DEFAULT_TOP_APPS_COUNT = 3
        const val DEFAULT_SORT_BY = "combined"
        const val DEFAULT_OPACITY_PERCENT = 95
        
        // TV margins
        const val TV_MARGIN_DP = 48
        const val MOBILE_MARGIN_DP = 16
        
        // App name display length
        const val APP_NAME_MAX_LENGTH = 12
        const val APP_TEXT_SIZE = 10f
        const val APP_BOTTOM_MARGIN_DP = 2
    }
    
    /**
     * Performance thresholds for color indicators.
     */
    object PerformanceThresholds {
        const val CPU_NORMAL_MAX = 50f
        const val CPU_WARNING_MAX = 80f
        const val RAM_NORMAL_MAX = 50f
        const val RAM_WARNING_MAX = 80f
        
        // Adaptive performance thresholds
        const val HIGH_CPU_THRESHOLD = 80f
        const val HIGH_RAM_THRESHOLD = 85f
        const val CRITICAL_CPU_THRESHOLD = 90f
        const val CRITICAL_RAM_THRESHOLD = 95f
        const val LOW_MEMORY_THRESHOLD_MB = 200L
        const val CRITICAL_MEMORY_THRESHOLD_MB = 100L
    }
    
    /**
     * Process monitoring configuration.
     */
    object ProcessMonitoring {
        const val MIN_CPU_THRESHOLD = 0.01f
        const val MIN_RAM_THRESHOLD_MB = 10L
        const val CPU_SCORE_WEIGHT = 10f
        const val RAM_SCORE_WEIGHT_DIVISOR = 100f
    }
    
    /**
     * Adaptive performance intervals.
     */
    object AdaptiveIntervals {
        const val FAST = 500L
        const val NORMAL = 1000L
        const val SLOW = 2000L
        const val VERY_SLOW = 5000L
        const val CHECK_INTERVAL_MS = 10_000L
    }
}
