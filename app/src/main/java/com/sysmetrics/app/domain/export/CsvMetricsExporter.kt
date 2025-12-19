package com.sysmetrics.app.domain.export

import com.sysmetrics.app.data.model.advanced.ExportData
import com.sysmetrics.app.data.model.advanced.ExportDataPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

/**
 * CSV format exporter for metrics data.
 * Generates spreadsheet-compatible CSV files.
 */
class CsvMetricsExporter : MetricsExporter {
    
    override val format = "CSV"
    override val mimeType = "text/csv"
    override val fileExtension = "csv"
    
    override suspend fun export(data: ExportData, outputFile: File): Result<File> {
        return withContext(Dispatchers.IO) {
            try {
                val content = generateContent(data)
                outputFile.writeText(content)
                Timber.d("CSV export successful: ${outputFile.absolutePath}")
                Result.success(outputFile)
            } catch (e: Exception) {
                Timber.e(e, "CSV export failed")
                Result.failure(e)
            }
        }
    }
    
    override fun generateContent(data: ExportData): String = buildString {
        // Header row
        appendLine(ExportDataPoint.CSV_HEADER)
        
        // Data rows
        data.dataPoints.forEach { point ->
            appendLine(point.toCsvLine())
        }
    }
}
