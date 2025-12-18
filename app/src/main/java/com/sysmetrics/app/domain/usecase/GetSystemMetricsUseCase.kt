package com.sysmetrics.app.domain.usecase

import com.sysmetrics.app.core.common.Constants.UpdateInterval
import com.sysmetrics.app.data.model.SystemMetrics
import com.sysmetrics.app.domain.repository.ISystemMetricsRepository
import kotlinx.coroutines.flow.Flow

/**
 * Use case for observing system metrics with configurable update interval.
 * Encapsulates the business logic for metrics collection.
 */
class GetSystemMetricsUseCase(
    private val repository: ISystemMetricsRepository
) {
    /**
     * Returns a Flow of system metrics updated at the specified interval.
     * @param intervalMs Update interval in milliseconds (default: 1000ms)
     */
    operator fun invoke(intervalMs: Long = UpdateInterval.DEFAULT): Flow<SystemMetrics> {
        return repository.getMetricsFlow(intervalMs)
    }

    /**
     * Collects a single snapshot of metrics.
     */
    suspend fun collectOnce(): SystemMetrics {
        return repository.collectMetrics()
    }

    /**
     * Resets the metrics baseline (useful for accurate CPU readings).
     */
    fun resetBaseline() {
        repository.resetBaseline()
    }
}
