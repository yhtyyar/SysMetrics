package com.sysmetrics.app.data.repository

import com.sysmetrics.app.core.common.Constants.Memory
import com.sysmetrics.app.core.common.Constants.UpdateInterval
import com.sysmetrics.app.data.model.CpuStats
import com.sysmetrics.app.data.model.SystemMetrics
import com.sysmetrics.app.data.source.BatteryDataSource
import com.sysmetrics.app.data.source.GpuDataSource
import com.sysmetrics.app.data.source.MetricsParser
import com.sysmetrics.app.data.source.NetworkDataSource
import com.sysmetrics.app.data.source.SystemDataSource
import com.sysmetrics.app.domain.repository.ISystemMetricsRepository
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import timber.log.Timber

/**
 * Repository for system metrics collection.
 * Enhanced with GPU, network, and battery monitoring.
 * Implements [ISystemMetricsRepository] for proper abstraction.
 * Provides a Flow-based API for continuous metrics streaming.
 */

class SystemMetricsRepository constructor(
    private val systemDataSource: SystemDataSource,
    private val gpuDataSource: GpuDataSource,
    private val networkDataSource: NetworkDataSource,
    private val batteryDataSource: BatteryDataSource
) : ISystemMetricsRepository {
    
    @Volatile
    private var previousCpuStats: CpuStats = CpuStats.EMPTY

    /**
     * Returns a Flow that emits system metrics at the specified interval.
     * Uses currentCoroutineContext for proper cancellation handling.
     * @param intervalMs Update interval in milliseconds (default: 1000ms)
     */
    override fun getMetricsFlow(intervalMs: Long): Flow<SystemMetrics> = flow {
        while (currentCoroutineContext().isActive) {
            val metrics = collectMetrics()
            emit(metrics)
            delay(intervalMs.coerceAtLeast(UpdateInterval.FAST))
        }
    }.catch { e ->
        Timber.e(e, "Error in metrics flow")
        emit(SystemMetrics.EMPTY)
    }

    /**
     * Collects a single snapshot of system metrics.
     * Reads CPU, memory, temperature, GPU, network, and battery data.
     */
    override suspend fun collectMetrics(): SystemMetrics {
        return try {
            // Read CPU stats and calculate usage
            val currentCpuStats = systemDataSource.readCpuStats()
            val cpuUsage = MetricsParser.calculateCpuUsage(previousCpuStats, currentCpuStats)
            previousCpuStats = currentCpuStats

            // Read memory info
            val memoryInfo = systemDataSource.readMemoryInfo()

            // Read temperature
            val temperatureInfo = systemDataSource.readTemperature()

            // Read GPU info
            val gpuInfo = gpuDataSource.readGpuInfo()

            // Read network stats
            val networkStats = networkDataSource.readNetworkStats()

            // Read battery info
            val batteryInfo = batteryDataSource.readBatteryInfo()

            SystemMetrics(
                // CPU & RAM
                cpuUsage = cpuUsage,
                cpuCores = systemDataSource.getCpuCoreCount(),
                ramUsedMb = memoryInfo.usedKb / Memory.KB_TO_MB,
                ramTotalMb = memoryInfo.totalKb / Memory.KB_TO_MB,
                ramUsagePercent = memoryInfo.usagePercent,
                temperatureCelsius = temperatureInfo.cpuTempCelsius,
                
                // GPU
                gpuUsage = gpuInfo.usagePercent,
                gpuFrequencyMhz = gpuInfo.frequencyMhz,
                gpuTemperature = gpuInfo.temperatureCelsius,
                gpuVendor = gpuInfo.vendor.displayName,
                hasGpu = gpuInfo.isAvailable,
                
                // Network
                downloadSpeedKbps = networkStats.downloadSpeedKbps,
                uploadSpeedKbps = networkStats.uploadSpeedKbps,
                totalDownloadMb = networkStats.totalDownloadMb,
                totalUploadMb = networkStats.totalUploadMb,
                hasNetwork = networkStats.isAvailable,
                
                // Battery
                batteryPercent = batteryInfo.percent,
                batteryCharging = batteryInfo.isCharging,
                batteryTemperature = batteryInfo.temperatureCelsius,
                hasBattery = batteryInfo.isAvailable,
                
                timestamp = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to collect metrics")
            SystemMetrics.EMPTY
        }
    }

    /**
     * Resets all metrics baselines.
     * Thread-safe due to @Volatile annotation.
     * Call this when starting a new monitoring session.
     */
    override fun resetBaseline() {
        previousCpuStats = CpuStats.EMPTY
        systemDataSource.clearCache()
        gpuDataSource.clearCache()
        networkDataSource.resetBaseline()
        batteryDataSource.clearCache()
        Timber.d("All metrics baselines reset")
    }
}
