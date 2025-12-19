package com.sysmetrics.app.data.model.advanced

/**
 * Comprehensive monitoring settings for SysMetrics Pro advanced features.
 * Includes all configurable options for monitoring, notifications, charts, and analytics.
 */
data class MonitoringSettings(
    // Update Intervals
    val updateIntervalMs: Long = UpdateInterval.FAST.intervalMs,
    
    // Chart Settings
    val showCpuChart: Boolean = true,
    val showRamChart: Boolean = true,
    val showTempChart: Boolean = true,
    val showNetworkChart: Boolean = true,
    val showFpsChart: Boolean = false,
    val chartHeight: ChartHeight = ChartHeight.NORMAL,
    val chartHistorySize: Int = 60,
    
    // Peak Notification Settings
    val peakNotificationsEnabled: Boolean = true,
    val peakNotificationIntervalMs: Long = PeakNotificationInterval.ONE_MINUTE.intervalMs,
    val showCpuPeak: Boolean = true,
    val showRamPeak: Boolean = true,
    val showTempPeak: Boolean = true,
    val showNetPeak: Boolean = true,
    val showFpsPeak: Boolean = false,
    val toastDurationMs: Int = 5000,
    
    // Time Window Analytics
    val show30sAverage: Boolean = true,
    val show1mAverage: Boolean = true,
    val show5mAverage: Boolean = true,
    val showPercentiles: Boolean = false,
    
    // FPS Monitoring
    val fpsMonitoringEnabled: Boolean = false,
    val jankDetectionEnabled: Boolean = false,
    val fpsThreshold: Int = 30,
    
    // Export Settings
    val defaultExportFormat: ExportFormat = ExportFormat.CSV,
    val defaultExportRange: TimeRange = TimeRange.LAST_5_MINUTES,
    
    // Data Retention
    val dataRetentionDays: Int = 7,
    val autoDeleteOldData: Boolean = true
) {
    companion object {
        val DEFAULT = MonitoringSettings()
    }
}

/**
 * Predefined update intervals with user-friendly names.
 */
enum class UpdateInterval(
    val intervalMs: Long,
    val displayName: String,
    val icon: String
) {
    ULTRA_FAST(500L, "Ultra-Fast (500ms)", "⚡"),
    FAST(1000L, "Fast (1s)", "🚀"),
    BALANCED(2000L, "Balanced (2s)", "⚖️"),
    POWER_SAVE(3000L, "Power Save (3s)", "🔋"),
    LIGHT(5000L, "Light (5s)", "💤");
    
    companion object {
        fun fromMs(ms: Long): UpdateInterval = entries.find { it.intervalMs == ms } ?: FAST
        
        val MIN_INTERVAL_MS = 500L
        val MAX_INTERVAL_MS = 5000L
    }
}

/**
 * Peak notification intervals.
 */
enum class PeakNotificationInterval(
    val intervalMs: Long,
    val displayName: String
) {
    THIRTY_SECONDS(30_000L, "Every 30 seconds"),
    ONE_MINUTE(60_000L, "Every minute"),
    FIVE_MINUTES(300_000L, "Every 5 minutes");
    
    companion object {
        fun fromMs(ms: Long): PeakNotificationInterval = 
            entries.find { it.intervalMs == ms } ?: ONE_MINUTE
    }
}

/**
 * Chart height options.
 */
enum class ChartHeight(
    val heightDp: Int,
    val displayName: String
) {
    SMALL(20, "Small"),
    NORMAL(40, "Normal"),
    LARGE(60, "Large");
    
    companion object {
        fun fromDp(dp: Int): ChartHeight = entries.find { it.heightDp == dp } ?: NORMAL
    }
}

/**
 * Export format options.
 */
sealed class ExportFormat(val extension: String, val mimeType: String) {
    data object CSV : ExportFormat("csv", "text/csv")
    data object TXT : ExportFormat("txt", "text/plain")
    data object JSON : ExportFormat("json", "application/json")
    
    companion object {
        fun fromString(value: String): ExportFormat = when (value.uppercase()) {
            "CSV" -> CSV
            "TXT" -> TXT
            "JSON" -> JSON
            else -> CSV
        }
        
        val entries = listOf(CSV, TXT, JSON)
    }
}

/**
 * Time range for data export and analytics.
 */
enum class TimeRange(
    val durationMs: Long,
    val displayName: String
) {
    LAST_1_MINUTE(60_000L, "Last 1 minute"),
    LAST_5_MINUTES(300_000L, "Last 5 minutes"),
    LAST_30_MINUTES(1_800_000L, "Last 30 minutes"),
    LAST_1_HOUR(3_600_000L, "Last 1 hour"),
    ALL(Long.MAX_VALUE, "All data");
    
    companion object {
        fun fromMs(ms: Long): TimeRange = entries.find { it.durationMs == ms } ?: LAST_5_MINUTES
    }
}
