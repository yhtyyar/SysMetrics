package com.sysmetrics.app.data.model.advanced

/**
 * Single data point for chart rendering.
 * Contains value, timestamp, and severity for color coding.
 */
data class ChartDataPoint(
    val timestamp: Long,
    val value: Float,
    val severity: Severity = Severity.fromValue(value)
) {
    companion object {
        val EMPTY = ChartDataPoint(0L, 0f, Severity.LOW)
    }
}

/**
 * Severity levels for color-coded display.
 * Used to determine chart line colors and status indicators.
 */
enum class Severity(
    val threshold: Float,
    val colorName: String
) {
    LOW(0f, "green"),      // 0-50%: Green - normal
    MEDIUM(50f, "yellow"), // 50-80%: Yellow - warning  
    HIGH(80f, "red");      // 80-100%: Red - critical
    
    companion object {
        fun fromValue(value: Float): Severity = when {
            value < 50f -> LOW
            value < 80f -> MEDIUM
            else -> HIGH
        }
        
        fun fromFps(fps: Int, threshold: Int = 30): Severity = when {
            fps >= 55 -> LOW      // 55-60+ fps: Smooth
            fps >= 45 -> MEDIUM   // 45-54 fps: Acceptable
            fps >= threshold -> MEDIUM  // Above threshold but below 45
            else -> HIGH          // Below threshold: Lag
        }
    }
}

/**
 * Collection of chart data points for a specific metric.
 */
data class ChartData(
    val metricType: MetricType,
    val points: List<ChartDataPoint>,
    val maxHistorySize: Int = 60
) {
    val isEmpty: Boolean get() = points.isEmpty()
    val latestValue: Float get() = points.lastOrNull()?.value ?: 0f
    val minValue: Float get() = points.minOfOrNull { it.value } ?: 0f
    val maxValue: Float get() = points.maxOfOrNull { it.value } ?: 0f
    val avgValue: Float get() = if (points.isEmpty()) 0f else points.map { it.value }.average().toFloat()
    
    companion object {
        fun empty(metricType: MetricType) = ChartData(metricType, emptyList())
    }
}
