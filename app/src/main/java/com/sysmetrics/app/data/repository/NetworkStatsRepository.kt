package com.sysmetrics.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sysmetrics.app.data.model.network.NetworkAlert
import com.sysmetrics.app.data.model.network.NetworkAlertConfig
import com.sysmetrics.app.data.model.network.NetworkTrafficStats
import com.sysmetrics.app.data.model.network.NetworkTypeInfo
import com.sysmetrics.app.data.model.network.PerAppTrafficStats
import com.sysmetrics.app.data.source.network.NetworkStatsDataSource
import com.sysmetrics.app.data.source.network.NetworkTypeDetector
import com.sysmetrics.app.data.source.network.PerAppTrafficDataSource
import com.sysmetrics.app.domain.repository.INetworkStatsRepository
import com.sysmetrics.app.native_bridge.NativeNetworkMetrics
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import timber.log.Timber

private val Context.networkStatsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "network_stats_prefs"
)

/**
 * Repository implementation for network traffic statistics.
 * Aggregates data from multiple sources and manages state.
 *
 * ## Data Source Priority:
 * 1. Native C++ implementation (fastest, most accurate)
 * 2. Kotlin /proc/net/dev parser (fallback)
 * 3. TrafficStats API (last resort)
 *
 * @param context Application context
 * @param networkStatsDataSource Kotlin-based data source
 * @param networkTypeDetector Network type detector
 * @param perAppTrafficDataSource Per-app traffic data source
 * @param nativeNetworkMetrics Native JNI bridge
 */
class NetworkStatsRepository(
    private val context: Context,
    private val networkStatsDataSource: NetworkStatsDataSource,
    private val networkTypeDetector: NetworkTypeDetector,
    private val perAppTrafficDataSource: PerAppTrafficDataSource,
    private val nativeNetworkMetrics: NativeNetworkMetrics
) : INetworkStatsRepository {

    companion object {
        private const val TAG = "NET_STATS_REPO"

        // DataStore keys for alert config
        private val KEY_ALERTS_ENABLED = booleanPreferencesKey("alerts_enabled")
        private val KEY_HIGH_SPEED_THRESHOLD = floatPreferencesKey("high_speed_threshold")
        private val KEY_DAILY_QUOTA_MB = longPreferencesKey("daily_quota_mb")
        private val KEY_QUOTA_WARNING_PERCENT = intPreferencesKey("quota_warning_percent")
        private val KEY_ANOMALY_DETECTION = booleanPreferencesKey("anomaly_detection")
    }

    // Alert flow for broadcasting alerts
    private val _alertFlow = MutableSharedFlow<NetworkAlert>(extraBufferCapacity = 10)

    // Cached alert config
    @Volatile
    private var cachedAlertConfig: NetworkAlertConfig = NetworkAlertConfig.DEFAULT

    // Use native by default if available
    private val useNative: Boolean
        get() = nativeNetworkMetrics.isNativeAvailable()

    override suspend fun getNetworkStats(): NetworkTrafficStats {
        return try {
            if (useNative) {
                nativeNetworkMetrics.getNetworkStats()
            } else {
                networkStatsDataSource.readNetworkStats()
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error getting network stats")
            NetworkTrafficStats.EMPTY
        }
    }

    override fun observeNetworkStats(intervalMs: Long): Flow<NetworkTrafficStats> = flow {
        while (currentCoroutineContext().isActive) {
            val stats = getNetworkStats()
            emit(stats)

            // Check alerts
            checkAlerts(stats)

            delay(intervalMs.coerceAtLeast(100L))
        }
    }.catch { e ->
        Timber.tag(TAG).e(e, "Error in network stats flow")
        emit(NetworkTrafficStats.EMPTY)
    }

    override suspend fun getNetworkType(): NetworkTypeInfo {
        return try {
            networkTypeDetector.getCurrentNetworkType()
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error getting network type")
            NetworkTypeInfo.DISCONNECTED
        }
    }

    override fun observeNetworkType(): Flow<NetworkTypeInfo> {
        return networkTypeDetector.observeNetworkType()
            .catch { e ->
                Timber.tag(TAG).e(e, "Error in network type flow")
                emit(NetworkTypeInfo.DISCONNECTED)
            }
    }

    override suspend fun getPerAppStats(topN: Int): List<PerAppTrafficStats> {
        return try {
            perAppTrafficDataSource.getPerAppStats(topN)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error getting per-app stats")
            emptyList()
        }
    }

    override fun observePerAppStats(intervalMs: Long, topN: Int): Flow<List<PerAppTrafficStats>> {
        return perAppTrafficDataSource.observePerAppStats(intervalMs, topN)
            .catch { e ->
                Timber.tag(TAG).e(e, "Error in per-app stats flow")
                emit(emptyList())
            }
    }

    override fun getPeakValues(): Pair<Float, Float> {
        return if (useNative) {
            nativeNetworkMetrics.getPeakValues()
        } else {
            networkStatsDataSource.getPeakValues()
        }
    }

    override fun getPeakTimestamps(): Pair<Long, Long> {
        return networkStatsDataSource.getPeakTimestamps()
    }

    override fun resetBaseline() {
        nativeNetworkMetrics.resetBaseline()
        networkStatsDataSource.resetBaseline()
        perAppTrafficDataSource.resetBaseline()
        Timber.tag(TAG).d("All baselines reset")
    }

    override suspend fun setAlertConfig(config: NetworkAlertConfig) {
        cachedAlertConfig = config
        context.networkStatsDataStore.edit { prefs ->
            prefs[KEY_ALERTS_ENABLED] = config.enabled
            prefs[KEY_HIGH_SPEED_THRESHOLD] = config.highSpeedThresholdMbps
            prefs[KEY_DAILY_QUOTA_MB] = config.dailyQuotaMb
            prefs[KEY_QUOTA_WARNING_PERCENT] = config.quotaWarningPercent
            prefs[KEY_ANOMALY_DETECTION] = config.anomalyDetectionEnabled
        }
        Timber.tag(TAG).d("Alert config saved: $config")
    }

    override suspend fun getAlertConfig(): NetworkAlertConfig {
        return try {
            context.networkStatsDataStore.data.map { prefs ->
                NetworkAlertConfig(
                    enabled = prefs[KEY_ALERTS_ENABLED] ?: false,
                    highSpeedThresholdMbps = prefs[KEY_HIGH_SPEED_THRESHOLD] ?: 100f,
                    dailyQuotaMb = prefs[KEY_DAILY_QUOTA_MB] ?: 0L,
                    quotaWarningPercent = prefs[KEY_QUOTA_WARNING_PERCENT] ?: 80,
                    anomalyDetectionEnabled = prefs[KEY_ANOMALY_DETECTION] ?: false
                )
            }.catch { e ->
                Timber.tag(TAG).e(e, "Error reading alert config")
                emit(NetworkAlertConfig.DEFAULT)
            }.first().also { config ->
                cachedAlertConfig = config
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error getting alert config")
            NetworkAlertConfig.DEFAULT
        }
    }

    override fun observeAlerts(): Flow<NetworkAlert> = _alertFlow.asSharedFlow()

    override fun isNativeAvailable(): Boolean = nativeNetworkMetrics.isNativeAvailable()

    override fun isMonitoringAvailable(): Boolean {
        return nativeNetworkMetrics.isNativeAvailable() || networkStatsDataSource.isAvailable()
    }

    /**
     * Checks current stats against alert thresholds.
     */
    private suspend fun checkAlerts(stats: NetworkTrafficStats) {
        if (!cachedAlertConfig.enabled) return

        val currentTime = System.currentTimeMillis()

        // Check high speed threshold
        val maxSpeedMbps = maxOf(stats.ingressMbps, stats.egressMbps)
        if (maxSpeedMbps > cachedAlertConfig.highSpeedThresholdMbps) {
            _alertFlow.emit(
                NetworkAlert(
                    type = NetworkAlert.AlertType.HIGH_SPEED,
                    message = "Network speed exceeds threshold: %.1f Mbps".format(maxSpeedMbps),
                    currentValue = maxSpeedMbps,
                    thresholdValue = cachedAlertConfig.highSpeedThresholdMbps,
                    timestamp = currentTime
                )
            )
        }

        // Check daily quota (if configured)
        if (cachedAlertConfig.dailyQuotaMb > 0) {
            val totalUsedMb = (stats.sessionIngressBytes + stats.sessionEgressBytes) / (1024f * 1024f)
            val quotaPercent = (totalUsedMb / cachedAlertConfig.dailyQuotaMb) * 100

            if (quotaPercent >= 100) {
                _alertFlow.emit(
                    NetworkAlert(
                        type = NetworkAlert.AlertType.QUOTA_EXCEEDED,
                        message = "Daily quota exceeded: %.1f MB used".format(totalUsedMb),
                        currentValue = quotaPercent,
                        thresholdValue = 100f,
                        timestamp = currentTime
                    )
                )
            } else if (quotaPercent >= cachedAlertConfig.quotaWarningPercent) {
                _alertFlow.emit(
                    NetworkAlert(
                        type = NetworkAlert.AlertType.QUOTA_WARNING,
                        message = "%.0f%% of daily quota used".format(quotaPercent),
                        currentValue = quotaPercent,
                        thresholdValue = cachedAlertConfig.quotaWarningPercent.toFloat(),
                        timestamp = currentTime
                    )
                )
            }
        }
    }
}
