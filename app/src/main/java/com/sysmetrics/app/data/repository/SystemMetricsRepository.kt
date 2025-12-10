package com.sysmetrics.app.data.repository

import com.sysmetrics.app.data.model.CpuStats
import com.sysmetrics.app.data.model.SystemMetrics
import com.sysmetrics.app.data.source.MetricsParser
import com.sysmetrics.app.data.source.SystemDataSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for system metrics collection.
 * Provides a Flow-based API for continuous metrics streaming.
 */
@Singleton
class SystemMetricsRepository @Inject constructor(
    private val systemDataSource: SystemDataSource
) {
    private var previousCpuStats: CpuStats = CpuStats.EMPTY

    /**
     * Returns a Flow that emits system metrics at the specified interval.
     * @param intervalMs Update interval in milliseconds (default: 1000ms)
     */
    fun getMetricsFlow(intervalMs: Long = 1000L): Flow<SystemMetrics> = flow {
        while (true) {
            val metrics = collectMetrics()
            emit(metrics)
            delay(intervalMs)
        }
    }.catch { e ->
        Timber.e(e, "Error in metrics flow")
        emit(SystemMetrics.EMPTY)
    }

    /**
     * Collects a single snapshot of system metrics.
     */
    suspend fun collectMetrics(): SystemMetrics {
        return try {
            // Read CPU stats and calculate usage
            val currentCpuStats = systemDataSource.readCpuStats()
            val cpuUsage = MetricsParser.calculateCpuUsage(previousCpuStats, currentCpuStats)
            previousCpuStats = currentCpuStats

            // Read memory info
            val memoryInfo = systemDataSource.readMemoryInfo()

            // Read temperature
            val temperatureInfo = systemDataSource.readTemperature()

            SystemMetrics(
                cpuUsage = cpuUsage,
                cpuCores = systemDataSource.getCpuCoreCount(),
                ramUsedMb = memoryInfo.usedKb / 1024,
                ramTotalMb = memoryInfo.totalKb / 1024,
                ramUsagePercent = memoryInfo.usagePercent,
                temperatureCelsius = temperatureInfo.cpuTempCelsius,
                timestamp = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to collect metrics")
            SystemMetrics.EMPTY
        }
    }

    /**
     * Resets the CPU statistics baseline.
     * Call this when starting a new monitoring session.
     */
    fun resetBaseline() {
        previousCpuStats = CpuStats.EMPTY
    }
}
