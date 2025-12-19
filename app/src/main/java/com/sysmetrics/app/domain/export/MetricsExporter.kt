package com.sysmetrics.app.domain.export

import com.sysmetrics.app.data.model.advanced.ExportConfig
import com.sysmetrics.app.data.model.advanced.ExportData
import java.io.File

/**
 * Interface for metrics exporters.
 * Implementations handle specific export formats (CSV, TXT, JSON).
 */
interface MetricsExporter {
    val format: String
    val mimeType: String
    val fileExtension: String
    
    suspend fun export(data: ExportData, outputFile: File): Result<File>
    
    fun generateContent(data: ExportData): String
}
