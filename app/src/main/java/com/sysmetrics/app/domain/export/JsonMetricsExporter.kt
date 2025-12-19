package com.sysmetrics.app.domain.export

import com.sysmetrics.app.data.model.advanced.ExportData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.io.File

/**
 * JSON format exporter for metrics data.
 * Generates structured JSON for programmatic analysis.
 */
class JsonMetricsExporter : MetricsExporter {
    
    override val format = "JSON"
    override val mimeType = "application/json"
    override val fileExtension = "json"
    
    override suspend fun export(data: ExportData, outputFile: File): Result<File> {
        return withContext(Dispatchers.IO) {
            try {
                val content = generateContent(data)
                outputFile.writeText(content)
                Timber.d("JSON export successful: ${outputFile.absolutePath}")
                Result.success(outputFile)
            } catch (e: Exception) {
                Timber.e(e, "JSON export failed")
                Result.failure(e)
            }
        }
    }
    
    override fun generateContent(data: ExportData): String {
        val json = JSONObject()
        
        // Metadata
        json.put("metadata", JSONObject().apply {
            put("generated_at", data.metadata.generatedAtUtc)
            put("generated_at_local", data.metadata.generatedAt)
            put("duration_seconds", data.metadata.durationSeconds)
            put("device_model", data.metadata.deviceModel)
            put("android_version", data.metadata.androidVersion)
            put("app_version", data.metadata.appVersion)
            put("data_points_count", data.metadata.dataPointsCount)
        })
        
        // Summary
        json.put("summary", JSONObject().apply {
            put("cpu", createMetricSummaryJson(data.summary.cpu))
            put("ram", createMetricSummaryJson(data.summary.ram))
            put("temperature", createMetricSummaryJson(data.summary.temperature))
            put("network_ingress", createMetricSummaryJson(data.summary.networkIngress))
            put("network_egress", createMetricSummaryJson(data.summary.networkEgress))
            data.summary.fps?.let { put("fps", createMetricSummaryJson(it)) }
        })
        
        // Data Points
        val dataPointsArray = JSONArray()
        data.dataPoints.forEach { point ->
            dataPointsArray.put(JSONObject().apply {
                put("timestamp", point.timestamp)
                put("timestamp_ms", point.timestampMs)
                put("cpu_percent", point.cpuPercent)
                put("ram_mb", point.ramMb)
                put("ram_percent", point.ramPercent)
                put("temp_celsius", point.tempCelsius)
                put("net_ingress_mbps", point.netIngressMbps)
                put("net_egress_mbps", point.netEgressMbps)
                put("fps", point.fps)
                if (point.batteryPercent >= 0) {
                    put("battery_percent", point.batteryPercent)
                }
            })
        }
        json.put("data_points", dataPointsArray)
        
        return json.toString(2) // Pretty print with indent
    }
    
    private fun createMetricSummaryJson(summary: com.sysmetrics.app.data.model.advanced.MetricSummary): JSONObject {
        return JSONObject().apply {
            put("average", summary.average)
            put("min", summary.min)
            put("max", summary.max)
            put("peak_at", summary.peakAt)
            put("unit", summary.unit)
        }
    }
}
