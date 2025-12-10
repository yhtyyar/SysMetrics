package com.sysmetrics.app.domain.repository

import com.sysmetrics.app.data.model.SystemMetrics
import kotlinx.coroutines.flow.Flow

/**
 * Interface for system metrics repository.
 * Defines the contract for metrics collection and streaming.
 */
interface ISystemMetricsRepository {
    
    /**
     * Returns a Flow that emits system metrics at the specified interval.
     * @param intervalMs Update interval in milliseconds
     */
    fun getMetricsFlow(intervalMs: Long): Flow<SystemMetrics>
    
    /**
     * Collects a single snapshot of system metrics.
     */
    suspend fun collectMetrics(): SystemMetrics
    
    /**
     * Resets the CPU statistics baseline.
     */
    fun resetBaseline()
}
