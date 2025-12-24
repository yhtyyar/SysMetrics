package com.sysmetrics.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sysmetrics.app.data.model.SystemMetrics

/**
 * Room entity for storing metrics history.
 * Indexed by timestamp for efficient queries.
 */
@Entity(
    tableName = "metrics_history",
    indices = [Index(value = ["timestamp"])]
)
data class MetricsHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "timestamp")
    val timestamp: Long,
    
    @ColumnInfo(name = "cpu_usage")
    val cpuUsage: Float,
    
    @ColumnInfo(name = "cpu_cores")
    val cpuCores: Int,
    
    @ColumnInfo(name = "ram_used_mb")
    val ramUsedMb: Long,
    
    @ColumnInfo(name = "ram_total_mb")
    val ramTotalMb: Long,
    
    @ColumnInfo(name = "ram_usage_percent")
    val ramUsagePercent: Float,
    
    @ColumnInfo(name = "temperature_celsius")
    val temperatureCelsius: Float,
    
    @ColumnInfo(name = "gpu_usage")
    val gpuUsage: Float = 0f,
    
    @ColumnInfo(name = "gpu_temperature")
    val gpuTemperature: Float = 0f,
    
    @ColumnInfo(name = "network_download_speed")
    val networkDownloadSpeed: Float = 0f,
    
    @ColumnInfo(name = "network_upload_speed")
    val networkUploadSpeed: Float = 0f,
    
    @ColumnInfo(name = "battery_percent")
    val batteryPercent: Int = -1,
    
    @ColumnInfo(name = "battery_charging")
    val batteryCharging: Boolean = false
) {
    /**
     * Convert entity to domain model.
     */
    fun toSystemMetrics(): SystemMetrics {
        return SystemMetrics(
            cpuUsage = cpuUsage,
            cpuCores = cpuCores,
            ramUsedMb = ramUsedMb,
            ramTotalMb = ramTotalMb,
            ramUsagePercent = ramUsagePercent,
            temperatureCelsius = temperatureCelsius,
            gpuUsage = gpuUsage,
            gpuTemperature = gpuTemperature,
            downloadSpeedKbps = networkDownloadSpeed,
            uploadSpeedKbps = networkUploadSpeed,
            batteryPercent = batteryPercent,
            batteryCharging = batteryCharging,
            timestamp = timestamp
        )
    }

    companion object {
        /**
         * Create entity from domain model.
         */
        fun fromSystemMetrics(metrics: SystemMetrics): MetricsHistoryEntity {
            return MetricsHistoryEntity(
                timestamp = metrics.timestamp,
                cpuUsage = metrics.cpuUsage,
                cpuCores = metrics.cpuCores,
                ramUsedMb = metrics.ramUsedMb,
                ramTotalMb = metrics.ramTotalMb,
                ramUsagePercent = metrics.ramUsagePercent,
                temperatureCelsius = metrics.temperatureCelsius,
                gpuUsage = metrics.gpuUsage,
                gpuTemperature = metrics.gpuTemperature,
                networkDownloadSpeed = metrics.downloadSpeedKbps,
                networkUploadSpeed = metrics.uploadSpeedKbps,
                batteryPercent = metrics.batteryPercent,
                batteryCharging = metrics.batteryCharging
            )
        }
    }
}
