package com.sysmetrics.app.data.repository

import com.sysmetrics.app.data.local.dao.MetricsHistoryDao
import com.sysmetrics.app.data.local.entity.MetricsHistoryEntity
import com.sysmetrics.app.data.model.SystemMetrics
import com.sysmetrics.app.domain.repository.IMetricsHistoryRepository
import com.sysmetrics.app.domain.repository.MetricsStatistics
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject

/**
 * Repository implementation for metrics history.
 * Stores metrics in Room database with 24-hour retention.
 */
class MetricsHistoryRepository @Inject constructor(
    private val metricsHistoryDao: MetricsHistoryDao
) : IMetricsHistoryRepository {

    companion object {
        private const val TAG = "MetricsHistory"
        private const val HOURS_24_MS = 24 * 60 * 60 * 1000L
    }

    override suspend fun saveMetrics(metrics: SystemMetrics) {
        try {
            val entity = MetricsHistoryEntity.fromSystemMetrics(metrics)
            metricsHistoryDao.insert(entity)
            Timber.tag(TAG).v("Saved metrics at ${metrics.timestamp}")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to save metrics")
        }
    }

    override fun getMetricsHistory(hours: Int): Flow<List<SystemMetrics>> {
        val fromTimestamp = System.currentTimeMillis() - (hours * 60 * 60 * 1000L)
        return metricsHistoryDao.getMetricsSince(fromTimestamp).map { entities ->
            entities.map { it.toSystemMetrics() }
        }
    }

    override suspend fun getMetricsBetween(
        fromTimestamp: Long,
        toTimestamp: Long
    ): List<SystemMetrics> {
        return metricsHistoryDao.getMetricsBetween(fromTimestamp, toTimestamp)
            .map { it.toSystemMetrics() }
    }

    override suspend fun getLatestMetrics(count: Int): List<SystemMetrics> {
        return metricsHistoryDao.getLatestMetrics(count)
            .map { it.toSystemMetrics() }
    }

    override suspend fun getStatistics(hours: Int): MetricsStatistics {
        val fromTimestamp = System.currentTimeMillis() - (hours * 60 * 60 * 1000L)
        
        return MetricsStatistics(
            avgCpuUsage = metricsHistoryDao.getAverageCpuUsage(fromTimestamp) ?: 0f,
            maxCpuUsage = metricsHistoryDao.getMaxCpuUsage(fromTimestamp) ?: 0f,
            avgRamUsage = metricsHistoryDao.getAverageRamUsage(fromTimestamp) ?: 0f,
            maxTemperature = metricsHistoryDao.getMaxTemperature(fromTimestamp) ?: 0f,
            totalEntries = metricsHistoryDao.getCount(),
            periodHours = hours
        )
    }

    override suspend fun cleanupOldEntries(): Int {
        val cutoffTimestamp = System.currentTimeMillis() - HOURS_24_MS
        val deleted = metricsHistoryDao.deleteOlderThan(cutoffTimestamp)
        if (deleted > 0) {
            Timber.tag(TAG).i("Cleaned up $deleted old metrics entries")
        }
        return deleted
    }

    override suspend fun deleteAll() {
        metricsHistoryDao.deleteAll()
        Timber.tag(TAG).i("Deleted all metrics history")
    }

    override suspend fun getCount(): Int {
        return metricsHistoryDao.getCount()
    }
}
