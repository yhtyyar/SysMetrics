package com.sysmetrics.app.domain.repository

import com.sysmetrics.app.data.model.network.NetworkAlert
import com.sysmetrics.app.data.model.network.NetworkAlertConfig
import com.sysmetrics.app.data.model.network.NetworkTrafficStats
import com.sysmetrics.app.data.model.network.NetworkTypeInfo
import com.sysmetrics.app.data.model.network.PerAppTrafficStats
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for network traffic statistics.
 * Provides abstraction over data sources for clean architecture.
 *
 * ## Responsibilities:
 * - Aggregate data from multiple sources (Kotlin, Native, TrafficStats)
 * - Manage peak tracking across sessions
 * - Provide reactive Flows for UI consumption
 * - Handle graceful degradation when data sources unavailable
 */
interface INetworkStatsRepository {

    /**
     * Gets current network traffic statistics snapshot.
     * Prefers native implementation for performance.
     *
     * @return [NetworkTrafficStats] with current speeds and totals
     */
    suspend fun getNetworkStats(): NetworkTrafficStats

    /**
     * Observes network traffic statistics as a Flow.
     * Emits updates at the specified interval.
     *
     * @param intervalMs Update interval in milliseconds (default: 1000ms)
     * @return Flow emitting [NetworkTrafficStats]
     */
    fun observeNetworkStats(intervalMs: Long = 1000L): Flow<NetworkTrafficStats>

    /**
     * Gets current network connection type and quality.
     *
     * @return [NetworkTypeInfo] with connection details
     */
    suspend fun getNetworkType(): NetworkTypeInfo

    /**
     * Observes network type changes as a Flow.
     *
     * @return Flow emitting [NetworkTypeInfo] on changes
     */
    fun observeNetworkType(): Flow<NetworkTypeInfo>

    /**
     * Gets per-application traffic statistics.
     *
     * @param topN Number of top apps to return (default: 5)
     * @return List of [PerAppTrafficStats] sorted by traffic
     */
    suspend fun getPerAppStats(topN: Int = 5): List<PerAppTrafficStats>

    /**
     * Observes per-app traffic as a Flow.
     *
     * @param intervalMs Update interval in milliseconds
     * @param topN Number of top apps to include
     * @return Flow of per-app stats lists
     */
    fun observePerAppStats(intervalMs: Long = 1000L, topN: Int = 5): Flow<List<PerAppTrafficStats>>

    /**
     * Gets peak traffic values for the session.
     *
     * @return Pair of (peakIngressMbps, peakEgressMbps)
     */
    fun getPeakValues(): Pair<Float, Float>

    /**
     * Gets peak timestamps.
     *
     * @return Pair of (peakIngressTimestamp, peakEgressTimestamp)
     */
    fun getPeakTimestamps(): Pair<Long, Long>

    /**
     * Resets all baselines and caches.
     * Call when starting a new monitoring session.
     */
    fun resetBaseline()

    /**
     * Sets alert configuration.
     *
     * @param config Alert configuration
     */
    suspend fun setAlertConfig(config: NetworkAlertConfig)

    /**
     * Gets current alert configuration.
     *
     * @return Current [NetworkAlertConfig]
     */
    suspend fun getAlertConfig(): NetworkAlertConfig

    /**
     * Observes network alerts.
     *
     * @return Flow of [NetworkAlert] events
     */
    fun observeAlerts(): Flow<NetworkAlert>

    /**
     * Checks if native implementation is available.
     *
     * @return true if native library is loaded
     */
    fun isNativeAvailable(): Boolean

    /**
     * Checks if network monitoring is available.
     *
     * @return true if at least one data source is available
     */
    fun isMonitoringAvailable(): Boolean
}
