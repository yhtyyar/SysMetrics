package com.sysmetrics.app.domain.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.sysmetrics.app.data.model.advanced.*
import com.sysmetrics.app.domain.analytics.ChartBufferManager
import com.sysmetrics.app.domain.analytics.MetricsAverageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Orchestrates data export and sharing functionality.
 * Coordinates between data collection and export format generation.
 */
class ExportManager(
    private val context: Context,
    private val chartBufferManager: ChartBufferManager,
    private val metricsAverageManager: MetricsAverageManager
) {
    private val csvExporter = CsvMetricsExporter()
    private val txtExporter = TxtMetricsExporter()
    private val jsonExporter = JsonMetricsExporter()
    
    private val exportDir: File by lazy {
        File(context.cacheDir, "exports").apply { mkdirs() }
    }
    
    suspend fun exportData(config: ExportConfig): Result<File> = withContext(Dispatchers.IO) {
        try {
            val exportData = collectExportData(config)
            val exporter = getExporter(config.format)
            val outputFile = File(exportDir, config.generateFileName())
            
            exporter.export(exportData, outputFile)
        } catch (e: Exception) {
            Timber.e(e, "Export failed")
            Result.failure(e)
        }
    }
    
    suspend fun exportAndShare(config: ExportConfig): Result<Intent> = withContext(Dispatchers.IO) {
        exportData(config).map { file ->
            createShareIntent(file, config.format)
        }
    }
    
    private fun getExporter(format: ExportFormat): MetricsExporter = when (format) {
        is ExportFormat.CSV -> csvExporter
        is ExportFormat.TXT -> txtExporter
        is ExportFormat.JSON -> jsonExporter
    }
    
    private fun collectExportData(config: ExportConfig): ExportData {
        val now = System.currentTimeMillis()
        val startTime = if (config.timeRange == TimeRange.ALL) 0L else now - config.timeRange.durationMs
        
        // Collect data points from chart buffers
        val dataPoints = mutableListOf<ExportDataPoint>()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        
        // Get CPU chart data
        val cpuData = chartBufferManager.getChartData(MetricType.CPU).points
        val ramData = chartBufferManager.getChartData(MetricType.RAM).points
        val tempData = chartBufferManager.getChartData(MetricType.TEMPERATURE).points
        val netIngressData = chartBufferManager.getChartData(MetricType.NETWORK_INGRESS).points
        val netEgressData = chartBufferManager.getChartData(MetricType.NETWORK_EGRESS).points
        val fpsData = chartBufferManager.getChartData(MetricType.FPS).points
        
        // Merge data by timestamp (simplified - using CPU timestamps as base)
        cpuData.filter { it.timestamp >= startTime }.forEachIndexed { index, cpuPoint ->
            dataPoints.add(ExportDataPoint(
                timestamp = dateFormat.format(Date(cpuPoint.timestamp)),
                timestampMs = cpuPoint.timestamp,
                cpuPercent = cpuPoint.value,
                ramMb = ramData.getOrNull(index)?.value?.toLong() ?: 0L,
                ramPercent = 0f, // Calculate if needed
                tempCelsius = tempData.getOrNull(index)?.value ?: 0f,
                netIngressMbps = netIngressData.getOrNull(index)?.value ?: 0f,
                netEgressMbps = netEgressData.getOrNull(index)?.value ?: 0f,
                fps = fpsData.getOrNull(index)?.value?.toInt() ?: 0
            ))
        }
        
        // Calculate summary
        val cpuStats = metricsAverageManager.getStats(MetricType.CPU)
        val ramStats = metricsAverageManager.getStats(MetricType.RAM)
        val tempStats = metricsAverageManager.getStats(MetricType.TEMPERATURE)
        val netIngressStats = metricsAverageManager.getStats(MetricType.NETWORK_INGRESS)
        val netEgressStats = metricsAverageManager.getStats(MetricType.NETWORK_EGRESS)
        val fpsStats = metricsAverageManager.getStats(MetricType.FPS)
        
        val peakTimeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        
        val summary = ExportSummary(
            cpu = MetricSummary(cpuStats.avg1m, cpuStats.min, cpuStats.max, peakTimeFormat.format(Date()), "%"),
            ram = MetricSummary(ramStats.avg1m, ramStats.min, ramStats.max, peakTimeFormat.format(Date()), "MB"),
            temperature = MetricSummary(tempStats.avg1m, tempStats.min, tempStats.max, peakTimeFormat.format(Date()), "°C"),
            networkIngress = MetricSummary(netIngressStats.avg1m, netIngressStats.min, netIngressStats.max, peakTimeFormat.format(Date()), "Mbps"),
            networkEgress = MetricSummary(netEgressStats.avg1m, netEgressStats.min, netEgressStats.max, peakTimeFormat.format(Date()), "Mbps"),
            fps = if (fpsStats.current > 0) MetricSummary(fpsStats.avg1m, fpsStats.min, fpsStats.max, peakTimeFormat.format(Date()), "fps") else null
        )
        
        val metadata = ExportMetadata.create(
            durationMs = if (dataPoints.isNotEmpty()) dataPoints.last().timestampMs - dataPoints.first().timestampMs else 0L,
            dataPointsCount = dataPoints.size
        )
        
        return ExportData(metadata, summary, dataPoints)
    }
    
    private fun createShareIntent(file: File, format: ExportFormat): Intent {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        
        return Intent(Intent.ACTION_SEND).apply {
            type = format.mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "SysMetrics Report - ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())}")
            putExtra(Intent.EXTRA_TEXT, "SysMetrics Pro performance report attached.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
    
    fun cleanupOldExports(maxAgeMs: Long = 24 * 60 * 60 * 1000L) {
        val cutoff = System.currentTimeMillis() - maxAgeMs
        exportDir.listFiles()?.forEach { file ->
            if (file.lastModified() < cutoff) {
                file.delete()
                Timber.d("Deleted old export: ${file.name}")
            }
        }
    }
}
