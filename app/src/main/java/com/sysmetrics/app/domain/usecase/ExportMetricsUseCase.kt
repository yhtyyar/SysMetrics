package com.sysmetrics.app.domain.usecase

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.sysmetrics.app.data.model.SystemMetrics
import com.sysmetrics.app.domain.repository.IMetricsHistoryRepository
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * Use case for exporting metrics history to CSV or JSON.
 */
class ExportMetricsUseCase @Inject constructor(
    private val context: Context,
    private val historyRepository: IMetricsHistoryRepository
) {
    companion object {
        private const val TAG = "ExportMetrics"
        private const val EXPORT_DIR = "exports"
    }

    /**
     * Export format options.
     */
    enum class ExportFormat {
        CSV, JSON
    }

    /**
     * Export result containing file URI and share intent.
     */
    data class ExportResult(
        val success: Boolean,
        val fileUri: Uri? = null,
        val fileName: String? = null,
        val shareIntent: Intent? = null,
        val errorMessage: String? = null
    )

    /**
     * Export metrics history for the specified hours.
     */
    suspend fun export(
        hours: Int = 24,
        format: ExportFormat = ExportFormat.CSV
    ): ExportResult {
        return try {
            Timber.tag(TAG).i("Starting export: hours=$hours, format=$format")
            
            val fromTimestamp = System.currentTimeMillis() - (hours * 60 * 60 * 1000L)
            val toTimestamp = System.currentTimeMillis()
            val metrics = historyRepository.getMetricsBetween(fromTimestamp, toTimestamp)
            
            if (metrics.isEmpty()) {
                return ExportResult(
                    success = false,
                    errorMessage = "No metrics data available for the selected period"
                )
            }

            val fileName = generateFileName(hours, format)
            val file = createExportFile(fileName)
            
            when (format) {
                ExportFormat.CSV -> writeCsv(file, metrics)
                ExportFormat.JSON -> writeJson(file, metrics)
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = createShareIntent(uri, format)
            
            Timber.tag(TAG).i("Export completed: ${metrics.size} entries to $fileName")
            
            ExportResult(
                success = true,
                fileUri = uri,
                fileName = fileName,
                shareIntent = shareIntent
            )
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Export failed")
            ExportResult(
                success = false,
                errorMessage = e.message ?: "Export failed"
            )
        }
    }

    private fun generateFileName(hours: Int, format: ExportFormat): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val extension = when (format) {
            ExportFormat.CSV -> "csv"
            ExportFormat.JSON -> "json"
        }
        return "sysmetrics_${hours}h_$timestamp.$extension"
    }

    private fun createExportFile(fileName: String): File {
        val exportDir = File(context.cacheDir, EXPORT_DIR)
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }
        return File(exportDir, fileName)
    }

    private fun writeCsv(file: File, metrics: List<SystemMetrics>) {
        file.bufferedWriter().use { writer ->
            // Header
            writer.write("timestamp,datetime,cpu_usage,cpu_cores,ram_used_mb,ram_total_mb,ram_percent,")
            writer.write("temperature,gpu_usage,gpu_temp,download_speed,upload_speed,")
            writer.write("battery_percent,battery_charging\n")

            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            
            // Data rows
            metrics.forEach { m ->
                writer.write("${m.timestamp},")
                writer.write("${dateFormat.format(Date(m.timestamp))},")
                writer.write("${String.format(Locale.US, "%.2f", m.cpuUsage)},")
                writer.write("${m.cpuCores},")
                writer.write("${m.ramUsedMb},")
                writer.write("${m.ramTotalMb},")
                writer.write("${String.format(Locale.US, "%.2f", m.ramUsagePercent)},")
                writer.write("${String.format(Locale.US, "%.1f", m.temperatureCelsius)},")
                writer.write("${String.format(Locale.US, "%.2f", m.gpuUsage)},")
                writer.write("${String.format(Locale.US, "%.1f", m.gpuTemperature)},")
                writer.write("${String.format(Locale.US, "%.2f", m.downloadSpeedKbps)},")
                writer.write("${String.format(Locale.US, "%.2f", m.uploadSpeedKbps)},")
                writer.write("${m.batteryPercent},")
                writer.write("${m.batteryCharging}\n")
            }
        }
    }

    private fun writeJson(file: File, metrics: List<SystemMetrics>) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        
        val jsonArray = JSONArray()
        metrics.forEach { m ->
            val obj = JSONObject().apply {
                put("timestamp", m.timestamp)
                put("datetime", dateFormat.format(Date(m.timestamp)))
                put("cpu", JSONObject().apply {
                    put("usage", m.cpuUsage)
                    put("cores", m.cpuCores)
                })
                put("ram", JSONObject().apply {
                    put("usedMb", m.ramUsedMb)
                    put("totalMb", m.ramTotalMb)
                    put("percent", m.ramUsagePercent)
                })
                put("temperature", m.temperatureCelsius)
                put("gpu", JSONObject().apply {
                    put("usage", m.gpuUsage)
                    put("temperature", m.gpuTemperature)
                })
                put("network", JSONObject().apply {
                    put("downloadSpeedKbps", m.downloadSpeedKbps)
                    put("uploadSpeedKbps", m.uploadSpeedKbps)
                })
                put("battery", JSONObject().apply {
                    put("percent", m.batteryPercent)
                    put("charging", m.batteryCharging)
                })
            }
            jsonArray.put(obj)
        }

        val root = JSONObject().apply {
            put("exportTime", dateFormat.format(Date()))
            put("totalEntries", metrics.size)
            put("metrics", jsonArray)
        }

        file.writeText(root.toString(2))
    }

    private fun createShareIntent(uri: Uri, format: ExportFormat): Intent {
        val mimeType = when (format) {
            ExportFormat.CSV -> "text/csv"
            ExportFormat.JSON -> "application/json"
        }
        
        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "SysMetrics Export")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
