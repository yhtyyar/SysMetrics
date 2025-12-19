package com.sysmetrics.app.data.model.advanced

import android.os.Build
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Configuration for data export.
 */
data class ExportConfig(
    val format: ExportFormat = ExportFormat.CSV,
    val timeRange: TimeRange = TimeRange.LAST_5_MINUTES,
    val resources: Set<MetricType> = setOf(
        MetricType.CPU,
        MetricType.RAM,
        MetricType.TEMPERATURE,
        MetricType.NETWORK_INGRESS,
        MetricType.NETWORK_EGRESS
    ),
    val includeMetadata: Boolean = true,
    val includeSummary: Boolean = true,
    val includeDataPoints: Boolean = true
) {
    fun generateFileName(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)
        val timestamp = dateFormat.format(Date())
        return "sysmetrics_report_$timestamp.${format.extension}"
    }
}

/**
 * Metadata included in exports.
 */
data class ExportMetadata(
    val generatedAt: String,
    val generatedAtUtc: String,
    val durationSeconds: Long,
    val deviceModel: String,
    val androidVersion: String,
    val appVersion: String,
    val dataPointsCount: Int
) {
    companion object {
        fun create(
            durationMs: Long,
            dataPointsCount: Int,
            appVersion: String = "1.0.0"
        ): ExportMetadata {
            val utcFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val localFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.getDefault())
            val now = Date()
            
            return ExportMetadata(
                generatedAt = localFormat.format(now),
                generatedAtUtc = utcFormat.format(now),
                durationSeconds = durationMs / 1000,
                deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
                androidVersion = Build.VERSION.RELEASE,
                appVersion = appVersion,
                dataPointsCount = dataPointsCount
            )
        }
    }
}

/**
 * Summary statistics for export.
 */
data class ExportSummary(
    val cpu: MetricSummary,
    val ram: MetricSummary,
    val temperature: MetricSummary,
    val networkIngress: MetricSummary,
    val networkEgress: MetricSummary,
    val fps: MetricSummary? = null
)

/**
 * Summary for a single metric.
 */
data class MetricSummary(
    val average: Float,
    val min: Float,
    val max: Float,
    val peakAt: String,
    val unit: String
)

/**
 * Single data point for export.
 */
data class ExportDataPoint(
    val timestamp: String,
    val timestampMs: Long,
    val cpuPercent: Float,
    val ramMb: Long,
    val ramPercent: Float,
    val tempCelsius: Float,
    val netIngressMbps: Float,
    val netEgressMbps: Float,
    val fps: Int = 0,
    val batteryPercent: Int = -1
) {
    fun toCsvLine(): String = listOf(
        timestamp,
        String.format(Locale.US, "%.1f", cpuPercent),
        ramMb.toString(),
        String.format(Locale.US, "%.1f", ramPercent),
        String.format(Locale.US, "%.1f", tempCelsius),
        String.format(Locale.US, "%.2f", netIngressMbps),
        String.format(Locale.US, "%.2f", netEgressMbps),
        fps.toString(),
        if (batteryPercent >= 0) batteryPercent.toString() else ""
    ).joinToString(",")
    
    companion object {
        const val CSV_HEADER = "timestamp,cpu_percent,ram_mb,ram_percent,temp_celsius,net_ingress_mbps,net_egress_mbps,fps,battery_percent"
    }
}

/**
 * Complete export data container.
 */
data class ExportData(
    val metadata: ExportMetadata,
    val summary: ExportSummary,
    val dataPoints: List<ExportDataPoint>
)
