package com.sysmetrics.app.domain.repository

import com.sysmetrics.app.data.model.SystemMetrics
import kotlinx.coroutines.flow.Flow

/**
 * Interface for metrics history repository.
 * Defines operations for storing and retrieving metrics history.
 */
interface IMetricsHistoryRepository {

    /**
     * Save metrics to history.
     */
    suspend fun saveMetrics(metrics: SystemMetrics)

    /**
     * Get metrics history for the last N hours as Flow.
     */
    fun getMetricsHistory(hours: Int = 24): Flow<List<SystemMetrics>>

    /**
     * Get metrics between two timestamps.
     */
    suspend fun getMetricsBetween(fromTimestamp: Long, toTimestamp: Long): List<SystemMetrics>

    /**
     * Get the latest N metrics entries.
     */
    suspend fun getLatestMetrics(count: Int): List<SystemMetrics>

    /**
     * Get statistics for a time period.
     */
    suspend fun getStatistics(hours: Int = 24): MetricsStatistics

    /**
     * Clean up old entries (older than 24 hours).
     */
    suspend fun cleanupOldEntries(): Int

    /**
     * Delete all history.
     */
    suspend fun deleteAll()

    /**
     * Get total count of stored metrics.
     */
    suspend fun getCount(): Int
}

/**
 * Statistics for metrics over a time period.
 */
data class MetricsStatistics(
    val avgCpuUsage: Float,
    val maxCpuUsage: Float,
    val avgRamUsage: Float,
    val maxTemperature: Float,
    val totalEntries: Int,
    val periodHours: Int
)
