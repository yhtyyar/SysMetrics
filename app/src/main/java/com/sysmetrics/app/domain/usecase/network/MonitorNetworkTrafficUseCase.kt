package com.sysmetrics.app.domain.usecase.network

import com.sysmetrics.app.data.model.network.NetworkAlert
import com.sysmetrics.app.data.model.network.NetworkAlertConfig
import com.sysmetrics.app.data.model.network.NetworkTrafficStats
import com.sysmetrics.app.data.model.network.NetworkTypeInfo
import com.sysmetrics.app.data.model.network.PerAppTrafficStats
import com.sysmetrics.app.domain.repository.INetworkStatsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import timber.log.Timber

/**
 * Use case for continuous network traffic monitoring.
 * Provides reactive Flows for real-time updates.
 *
 * ## Responsibilities:
 * - Continuous traffic monitoring Flow
 * - Alert generation based on thresholds
 * - Anomaly detection
 * - Combined state flow for UI
 *
 * @param repository Network stats repository
 */
class MonitorNetworkTrafficUseCase(
    private val repository: INetworkStatsRepository
) {
    companion object {
        private const val TAG = "MONITOR_NET_UC"
        private const val DEFAULT_INTERVAL_MS = 1000L
        private const val FAST_INTERVAL_MS = 500L
        private const val SLOW_INTERVAL_MS = 2000L
    }

    /**
     * Combined monitoring state.
     */
    data class MonitoringState(
        val trafficStats: NetworkTrafficStats,
        val networkType: NetworkTypeInfo,
        val perAppStats: List<PerAppTrafficStats>,
        val alertConfig: NetworkAlertConfig,
        val timestamp: Long = System.currentTimeMillis()
    ) {
        val isConnected: Boolean
            get() = networkType.type != com.sysmetrics.app.data.model.network.NetworkTypeEnum.NONE

        val hasActiveTraffic: Boolean
            get() = trafficStats.ingressBytesPerSec > 0 || trafficStats.egressBytesPerSec > 0

        companion object {
            val EMPTY = MonitoringState(
                trafficStats = NetworkTrafficStats.EMPTY,
                networkType = NetworkTypeInfo.DISCONNECTED,
                perAppStats = emptyList(),
                alertConfig = NetworkAlertConfig.DEFAULT
            )
        }
    }

    /**
     * Starts continuous traffic monitoring.
     *
     * @param intervalMs Update interval in milliseconds
     * @return Flow of [NetworkTrafficStats]
     */
    fun observeTraffic(intervalMs: Long = DEFAULT_INTERVAL_MS): Flow<NetworkTrafficStats> {
        return repository.observeNetworkStats(intervalMs)
            .onEach { stats ->
                Timber.tag(TAG).v("Traffic: ↓${stats.formatIngressSpeed()} ↑${stats.formatEgressSpeed()}")
            }
            .catch { e ->
                Timber.tag(TAG).e(e, "Error monitoring traffic")
                emit(NetworkTrafficStats.EMPTY)
            }
    }

    /**
     * Starts combined monitoring of all network metrics.
     *
     * @param intervalMs Update interval in milliseconds
     * @param topApps Number of top apps to track
     * @return Flow of [MonitoringState]
     */
    fun observeAll(
        intervalMs: Long = DEFAULT_INTERVAL_MS,
        topApps: Int = 5
    ): Flow<MonitoringState> {
        return combine(
            repository.observeNetworkStats(intervalMs),
            repository.observeNetworkType(),
            repository.observePerAppStats(intervalMs, topApps)
        ) { traffic, networkType, perAppStats ->
            MonitoringState(
                trafficStats = traffic,
                networkType = networkType,
                perAppStats = perAppStats,
                alertConfig = repository.getAlertConfig()
            )
        }.catch { e ->
            Timber.tag(TAG).e(e, "Error in combined monitoring")
            emit(MonitoringState.EMPTY)
        }
    }

    /**
     * Observes network type changes only.
     *
     * @return Flow of [NetworkTypeInfo]
     */
    fun observeNetworkType(): Flow<NetworkTypeInfo> {
        return repository.observeNetworkType()
            .distinctUntilChanged()
            .onEach { type ->
                Timber.tag(TAG).d("Network type changed: ${type.formatDisplay()}")
            }
    }

    /**
     * Observes per-app traffic.
     *
     * @param intervalMs Update interval
     * @param topN Number of top apps
     * @return Flow of per-app stats lists
     */
    fun observePerAppTraffic(
        intervalMs: Long = DEFAULT_INTERVAL_MS,
        topN: Int = 5
    ): Flow<List<PerAppTrafficStats>> {
        return repository.observePerAppStats(intervalMs, topN)
            .catch { e ->
                Timber.tag(TAG).e(e, "Error monitoring per-app traffic")
                emit(emptyList())
            }
    }

    /**
     * Observes network alerts.
     *
     * @return Flow of [NetworkAlert] events
     */
    fun observeAlerts(): Flow<NetworkAlert> {
        return repository.observeAlerts()
            .onEach { alert ->
                Timber.tag(TAG).w("Network alert: ${alert.type} - ${alert.message}")
            }
    }

    /**
     * Observes traffic with speed threshold filtering.
     * Only emits when traffic exceeds threshold.
     *
     * @param thresholdBytesPerSec Minimum speed to emit
     * @param intervalMs Update interval
     * @return Filtered flow of [NetworkTrafficStats]
     */
    fun observeHighTraffic(
        thresholdBytesPerSec: Long = 1024 * 100, // 100 KB/s default
        intervalMs: Long = DEFAULT_INTERVAL_MS
    ): Flow<NetworkTrafficStats> {
        return repository.observeNetworkStats(intervalMs)
            .map { stats ->
                if (stats.ingressBytesPerSec >= thresholdBytesPerSec ||
                    stats.egressBytesPerSec >= thresholdBytesPerSec) {
                    stats
                } else {
                    null
                }
            }
            .catch { e ->
                Timber.tag(TAG).e(e, "Error in high traffic monitoring")
            }
            .map { it ?: NetworkTrafficStats.EMPTY }
    }

    /**
     * Sets alert configuration.
     *
     * @param config New alert configuration
     */
    suspend fun setAlertConfig(config: NetworkAlertConfig) {
        repository.setAlertConfig(config)
        Timber.tag(TAG).d("Alert config updated: $config")
    }

    /**
     * Gets current alert configuration.
     *
     * @return Current [NetworkAlertConfig]
     */
    suspend fun getAlertConfig(): NetworkAlertConfig {
        return repository.getAlertConfig()
    }

    /**
     * Resets monitoring baseline.
     * Call when starting a new session.
     */
    fun resetBaseline() {
        repository.resetBaseline()
        Timber.tag(TAG).d("Monitoring baseline reset")
    }

    /**
     * Gets recommended update interval based on device capabilities.
     *
     * @return Interval in milliseconds
     */
    fun getRecommendedInterval(): Long {
        return if (repository.isNativeAvailable()) {
            FAST_INTERVAL_MS // Native can handle faster updates
        } else {
            DEFAULT_INTERVAL_MS
        }
    }

    /**
     * Checks if fast monitoring mode is available.
     */
    fun isFastMonitoringAvailable(): Boolean = repository.isNativeAvailable()
}
