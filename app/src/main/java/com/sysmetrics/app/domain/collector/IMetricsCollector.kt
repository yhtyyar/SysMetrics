package com.sysmetrics.app.domain.collector

/**
 * Interface for system metrics collection.
 * Enables easy testing and mocking.
 */
interface IMetricsCollector {
    /**
     * Initialize baseline for CPU measurement.
     * Must be called before first getCpuUsage() call.
     */
    suspend fun initializeBaseline()
    
    /**
     * Get current CPU usage percentage.
     * @return CPU usage 0.0-100.0
     */
    suspend fun getCpuUsage(): Float
    
    /**
     * Get RAM usage information.
     * @return Triple<UsedMB, TotalMB, PercentUsed>
     */
    suspend fun getRamUsage(): Triple<Long, Long, Float>
    
    /**
     * Get CPU core count.
     */
    fun getCoreCount(): Int
    
    /**
     * Reset CPU stats baseline.
     */
    fun resetBaseline()
}
