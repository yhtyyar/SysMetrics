package com.sysmetrics.app.domain.usecase

import com.sysmetrics.app.data.model.SystemMetrics
import com.sysmetrics.app.data.repository.SystemMetricsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for observing system metrics with configurable update interval.
 */
class GetSystemMetricsUseCase @Inject constructor(
    private val repository: SystemMetricsRepository
) {
    /**
     * Returns a Flow of system metrics updated at the specified interval.
     * @param intervalMs Update interval in milliseconds
     */
    operator fun invoke(intervalMs: Long = 1000L): Flow<SystemMetrics> {
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
