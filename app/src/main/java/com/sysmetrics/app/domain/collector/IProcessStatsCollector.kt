package com.sysmetrics.app.domain.collector

import com.sysmetrics.app.utils.AppStats

/**
 * Interface for process statistics collection.
 * Enables easy testing and mocking.
 */
interface IProcessStatsCollector {
    /**
     * Get statistics for current app (SysMetrics).
     */
    suspend fun getSelfStats(): AppStats
    
    /**
     * Get top N apps by resource usage.
     * @param count Number of top apps to return
     * @param sortBy Sorting criteria: "cpu", "ram", or "combined"
     * @return List of AppStats sorted by specified criteria
     */
    suspend fun getTopApps(count: Int, sortBy: String = "combined"): List<AppStats>
    
    /**
     * Get top apps by CPU usage specifically.
     */
    suspend fun getTopAppsByCpu(count: Int): List<AppStats>
    
    /**
     * Get top apps by RAM usage specifically.
     */
    suspend fun getTopAppsByRam(count: Int): List<AppStats>
    
    /**
     * Initialize baseline for accurate measurement.
     */
    suspend fun initializeBaseline()
    
    /**
     * Warm up cache with initial readings.
     */
    suspend fun warmUpCache()
    
    /**
     * Clear cached stats.
     */
    fun clearCache()
}
