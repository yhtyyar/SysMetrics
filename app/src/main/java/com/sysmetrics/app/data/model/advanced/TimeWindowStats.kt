package com.sysmetrics.app.data.model.advanced

/**
 * Statistics calculated over various time windows.
 * Used for displaying averages, min/max, and percentiles.
 */
data class TimeWindowStats(
    val metricType: MetricType,
    val current: Float,
    val avg30s: Float,
    val avg1m: Float,
    val avg5m: Float,
    val min: Float,
    val max: Float,
    val p95: Float,
    val p99: Float,
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        fun empty(metricType: MetricType) = TimeWindowStats(
            metricType = metricType,
            current = 0f,
            avg30s = 0f,
            avg1m = 0f,
            avg5m = 0f,
            min = 0f,
            max = 0f,
            p95 = 0f,
            p99 = 0f
        )
    }
}

/**
 * Types of metrics that can be tracked.
 */
enum class MetricType(val displayName: String, val unit: String) {
    CPU("CPU", "%"),
    RAM("RAM", "MB"),
    TEMPERATURE("Temperature", "°C"),
    NETWORK_INGRESS("Network ↓", "MB/s"),
    NETWORK_EGRESS("Network ↑", "MB/s"),
    FPS("FPS", "fps"),
    BATTERY("Battery", "%")
}

/**
 * Aggregated statistics for all metrics.
 */
data class AllMetricsStats(
    val cpu: TimeWindowStats,
    val ram: TimeWindowStats,
    val temperature: TimeWindowStats,
    val networkIngress: TimeWindowStats,
    val networkEgress: TimeWindowStats,
    val fps: TimeWindowStats,
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        val EMPTY = AllMetricsStats(
            cpu = TimeWindowStats.empty(MetricType.CPU),
            ram = TimeWindowStats.empty(MetricType.RAM),
            temperature = TimeWindowStats.empty(MetricType.TEMPERATURE),
            networkIngress = TimeWindowStats.empty(MetricType.NETWORK_INGRESS),
            networkEgress = TimeWindowStats.empty(MetricType.NETWORK_EGRESS),
            fps = TimeWindowStats.empty(MetricType.FPS)
        )
    }
}
