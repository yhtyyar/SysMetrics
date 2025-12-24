package com.sysmetrics.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sysmetrics.app.data.local.entity.MetricsHistoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for metrics history operations.
 */
@Dao
interface MetricsHistoryDao {

    /**
     * Insert a new metrics entry.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: MetricsHistoryEntity)

    /**
     * Insert multiple metrics entries.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<MetricsHistoryEntity>)

    /**
     * Get all metrics within the last N hours.
     */
    @Query("SELECT * FROM metrics_history WHERE timestamp >= :fromTimestamp ORDER BY timestamp DESC")
    fun getMetricsSince(fromTimestamp: Long): Flow<List<MetricsHistoryEntity>>

    /**
     * Get metrics between two timestamps.
     */
    @Query("SELECT * FROM metrics_history WHERE timestamp BETWEEN :fromTimestamp AND :toTimestamp ORDER BY timestamp ASC")
    suspend fun getMetricsBetween(fromTimestamp: Long, toTimestamp: Long): List<MetricsHistoryEntity>

    /**
     * Get the latest N metrics entries.
     */
    @Query("SELECT * FROM metrics_history ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getLatestMetrics(limit: Int): List<MetricsHistoryEntity>

    /**
     * Get the latest metrics entry.
     */
    @Query("SELECT * FROM metrics_history ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestMetric(): MetricsHistoryEntity?

    /**
     * Get total count of stored metrics.
     */
    @Query("SELECT COUNT(*) FROM metrics_history")
    suspend fun getCount(): Int

    /**
     * Delete metrics older than the specified timestamp.
     * Used for cleanup to maintain 24-hour history.
     */
    @Query("DELETE FROM metrics_history WHERE timestamp < :beforeTimestamp")
    suspend fun deleteOlderThan(beforeTimestamp: Long): Int

    /**
     * Delete all metrics history.
     */
    @Query("DELETE FROM metrics_history")
    suspend fun deleteAll()

    /**
     * Get average CPU usage for a time period.
     */
    @Query("SELECT AVG(cpu_usage) FROM metrics_history WHERE timestamp >= :fromTimestamp")
    suspend fun getAverageCpuUsage(fromTimestamp: Long): Float?

    /**
     * Get average RAM usage for a time period.
     */
    @Query("SELECT AVG(ram_usage_percent) FROM metrics_history WHERE timestamp >= :fromTimestamp")
    suspend fun getAverageRamUsage(fromTimestamp: Long): Float?

    /**
     * Get max CPU usage for a time period.
     */
    @Query("SELECT MAX(cpu_usage) FROM metrics_history WHERE timestamp >= :fromTimestamp")
    suspend fun getMaxCpuUsage(fromTimestamp: Long): Float?

    /**
     * Get max temperature for a time period.
     */
    @Query("SELECT MAX(temperature_celsius) FROM metrics_history WHERE timestamp >= :fromTimestamp")
    suspend fun getMaxTemperature(fromTimestamp: Long): Float?
}
