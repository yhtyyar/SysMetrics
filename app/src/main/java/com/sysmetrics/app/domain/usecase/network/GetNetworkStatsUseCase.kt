package com.sysmetrics.app.domain.usecase.network

import com.sysmetrics.app.data.model.network.NetworkDisplayMode
import com.sysmetrics.app.data.model.network.NetworkTrafficStats
import com.sysmetrics.app.data.model.network.NetworkTypeInfo
import com.sysmetrics.app.data.model.network.PerAppTrafficStats
import com.sysmetrics.app.domain.repository.INetworkStatsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import timber.log.Timber

/**
 * Use case for retrieving network statistics.
 * Combines data from multiple sources and transforms for UI consumption.
 *
 * ## Responsibilities:
 * - Get current network stats snapshot
 * - Combine traffic stats with network type info
 * - Format data for different display modes
 * - Validate data integrity
 *
 * @param repository Network stats repository
 */
class GetNetworkStatsUseCase(
    private val repository: INetworkStatsRepository
) {
    companion object {
        private const val TAG = "GET_NET_STATS_UC"
    }

    /**
     * Combined network state for UI.
     */
    data class NetworkState(
        val trafficStats: NetworkTrafficStats,
        val networkType: NetworkTypeInfo,
        val perAppStats: List<PerAppTrafficStats>,
        val isMonitoringAvailable: Boolean,
        val isNativeAvailable: Boolean,
        val displayMode: NetworkDisplayMode = NetworkDisplayMode.COMPACT
    ) {
        /**
         * Formats state for compact overlay display.
         */
        fun formatCompact(): String = trafficStats.formatCompact()

        /**
         * Formats state for extended display.
         */
        fun formatExtended(): String = buildString {
            appendLine(trafficStats.formatExtended())
            append("Network: ${networkType.formatCompact()}")
        }

        /**
         * Formats per-app traffic for overlay.
         */
        fun formatPerApp(): String = perAppStats.take(3).joinToString("\n") { it.formatDisplay() }

        /**
         * Formats combined view with all metrics.
         */
        fun formatCombined(cpuPercent: Float, ramPercent: Float, tempCelsius: Float): String = buildString {
            append("CPU: %.0f%% | RAM: %.0f%% | Temp: %.0f°C\n".format(cpuPercent, ramPercent, tempCelsius))
            append(trafficStats.formatCompact())
            append(" | ${networkType.formatCompact()}")
        }

        companion object {
            val EMPTY = NetworkState(
                trafficStats = NetworkTrafficStats.EMPTY,
                networkType = NetworkTypeInfo.DISCONNECTED,
                perAppStats = emptyList(),
                isMonitoringAvailable = false,
                isNativeAvailable = false
            )
        }
    }

    /**
     * Gets current network state snapshot.
     *
     * @return [NetworkState] with all network information
     */
    suspend operator fun invoke(): NetworkState {
        return try {
            val trafficStats = repository.getNetworkStats()
            val networkType = repository.getNetworkType()
            val perAppStats = repository.getPerAppStats()

            NetworkState(
                trafficStats = trafficStats,
                networkType = networkType,
                perAppStats = perAppStats,
                isMonitoringAvailable = repository.isMonitoringAvailable(),
                isNativeAvailable = repository.isNativeAvailable()
            )
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error getting network state")
            NetworkState.EMPTY
        }
    }

    /**
     * Gets traffic stats only (lightweight).
     *
     * @return [NetworkTrafficStats]
     */
    suspend fun getTrafficStats(): NetworkTrafficStats {
        return try {
            repository.getNetworkStats()
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error getting traffic stats")
            NetworkTrafficStats.EMPTY
        }
    }

    /**
     * Gets network type only.
     *
     * @return [NetworkTypeInfo]
     */
    suspend fun getNetworkType(): NetworkTypeInfo {
        return try {
            repository.getNetworkType()
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error getting network type")
            NetworkTypeInfo.DISCONNECTED
        }
    }

    /**
     * Gets per-app stats.
     *
     * @param topN Number of top apps
     * @return List of [PerAppTrafficStats]
     */
    suspend fun getPerAppStats(topN: Int = 5): List<PerAppTrafficStats> {
        return try {
            repository.getPerAppStats(topN)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error getting per-app stats")
            emptyList()
        }
    }

    /**
     * Gets peak values.
     *
     * @return Pair of (peakIngressMbps, peakEgressMbps)
     */
    fun getPeakValues(): Pair<Float, Float> = repository.getPeakValues()

    /**
     * Validates traffic stats data.
     *
     * @param stats Stats to validate
     * @return true if data appears valid
     */
    fun validateStats(stats: NetworkTrafficStats): Boolean {
        // Check for reasonable values
        if (stats.ingressBytesPerSec < 0 || stats.egressBytesPerSec < 0) return false
        if (stats.ingressMbps < 0 || stats.egressMbps < 0) return false
        if (stats.totalIngressBytes < 0 || stats.totalEgressBytes < 0) return false

        // Check for unreasonably high values (> 100 Gbps)
        if (stats.ingressMbps > 100_000 || stats.egressMbps > 100_000) {
            Timber.tag(TAG).w("Unusually high speed detected: ${stats.ingressMbps} / ${stats.egressMbps} Mbps")
            return false
        }

        return true
    }

    /**
     * Checks if monitoring is available.
     */
    fun isMonitoringAvailable(): Boolean = repository.isMonitoringAvailable()

    /**
     * Checks if native implementation is available.
     */
    fun isNativeAvailable(): Boolean = repository.isNativeAvailable()
}
