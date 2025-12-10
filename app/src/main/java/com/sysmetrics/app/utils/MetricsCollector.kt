package com.sysmetrics.app.utils

import android.content.Context
import com.sysmetrics.app.data.model.CpuStats
import com.sysmetrics.app.data.source.SystemDataSource
import kotlinx.coroutines.runBlocking
import timber.log.Timber

/**
 * Simplified metrics collector utility
 * Wraps SystemDataSource for easier synchronous access
 * Following TvOverlay_cpu pattern
 */
class MetricsCollector(
    private val context: Context,
    private val systemDataSource: SystemDataSource
) {

    private var previousCpuStats: CpuStats = CpuStats.EMPTY

    /**
     * Get current CPU usage percentage
     * Calculates delta between previous and current stats
     * @return CPU usage 0.0-100.0
     */
    fun getCpuUsage(): Float {
        return try {
            val currentStats = runBlocking { systemDataSource.readCpuStats() }
            val usage = calculateCpuUsage(previousCpuStats, currentStats)
            previousCpuStats = currentStats
            usage.coerceIn(0f, 100f)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get CPU usage")
            0f
        }
    }

    /**
     * Get RAM usage information
     * @return Triple<UsedMB, TotalMB, PercentUsed>
     * Values are guaranteed to be non-negative and percentage is 0-100
     */
    fun getRamUsage(): Triple<Long, Long, Float> {
        return try {
            val memInfo = runBlocking { systemDataSource.readMemoryInfo() }
            
            // Convert to MB and ensure non-negative
            val totalMb = (memInfo.totalKb / 1024).coerceAtLeast(0)
            val usedMb = (memInfo.usedKb / 1024).coerceAtLeast(0)
            
            // Calculate percentage, ensure 0-100 range
            val percentUsed = if (totalMb > 0) {
                ((usedMb.toFloat() / totalMb.toFloat()) * 100f).coerceIn(0f, 100f)
            } else 0f

            // Ensure used never exceeds total
            val validUsedMb = usedMb.coerceAtMost(totalMb)

            Triple(validUsedMb, totalMb, percentUsed)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get RAM usage")
            Triple(0L, 0L, 0f)
        }
    }

    /**
     * Get CPU temperature in Celsius
     * @return Temperature or -1 if unavailable
     */
    fun getTemperature(): Float {
        return try {
            val tempInfo = runBlocking { systemDataSource.readTemperature() }
            tempInfo.cpuTempCelsius.coerceIn(0f, 200f)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get temperature")
            -1f
        }
    }

    /**
     * Get CPU core count
     */
    fun getCoreCount(): Int {
        return try {
            systemDataSource.getCpuCoreCount()
        } catch (e: Exception) {
            Timber.e(e, "Failed to get core count")
            Runtime.getRuntime().availableProcessors()
        }
    }

    /**
     * Calculate CPU usage from stats delta
     */
    private fun calculateCpuUsage(previous: CpuStats, current: CpuStats): Float {
        val totalDelta = (current.total() - previous.total()).toFloat()
        if (totalDelta <= 0f) return 0f

        val idleDelta = (current.idle - previous.idle).toFloat()
        val activeDelta = totalDelta - idleDelta

        return (activeDelta / totalDelta) * 100f
    }

    /**
     * Reset CPU stats baseline
     * Call this when starting new monitoring session
     */
    fun resetBaseline() {
        previousCpuStats = CpuStats.EMPTY
    }
}
