package com.sysmetrics.app.data.source

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sysmetrics.app.data.model.advanced.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import timber.log.Timber

private val Context.advancedDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "sysmetrics_advanced_prefs"
)

/**
 * DataStore-based storage for advanced monitoring settings.
 * Handles all configurable options for the 7 advanced features.
 */
class AdvancedPreferencesDataSource(
    private val context: Context
) {
    private object Keys {
        val UPDATE_INTERVAL_MS = longPreferencesKey("update_interval_ms")
        val SHOW_CPU_CHART = booleanPreferencesKey("show_cpu_chart")
        val SHOW_RAM_CHART = booleanPreferencesKey("show_ram_chart")
        val SHOW_TEMP_CHART = booleanPreferencesKey("show_temp_chart")
        val SHOW_NETWORK_CHART = booleanPreferencesKey("show_network_chart")
        val SHOW_FPS_CHART = booleanPreferencesKey("show_fps_chart")
        val CHART_HEIGHT = stringPreferencesKey("chart_height")
        val CHART_HISTORY_SIZE = intPreferencesKey("chart_history_size")
        val PEAK_NOTIFICATIONS_ENABLED = booleanPreferencesKey("peak_notifications_enabled")
        val PEAK_NOTIFICATION_INTERVAL_MS = longPreferencesKey("peak_notification_interval_ms")
        val SHOW_CPU_PEAK = booleanPreferencesKey("show_cpu_peak")
        val SHOW_RAM_PEAK = booleanPreferencesKey("show_ram_peak")
        val SHOW_TEMP_PEAK = booleanPreferencesKey("show_temp_peak")
        val SHOW_NET_PEAK = booleanPreferencesKey("show_net_peak")
        val SHOW_FPS_PEAK = booleanPreferencesKey("show_fps_peak")
        val TOAST_DURATION_MS = intPreferencesKey("toast_duration_ms")
        val SHOW_30S_AVERAGE = booleanPreferencesKey("show_30s_average")
        val SHOW_1M_AVERAGE = booleanPreferencesKey("show_1m_average")
        val SHOW_5M_AVERAGE = booleanPreferencesKey("show_5m_average")
        val SHOW_PERCENTILES = booleanPreferencesKey("show_percentiles")
        val FPS_MONITORING_ENABLED = booleanPreferencesKey("fps_monitoring_enabled")
        val JANK_DETECTION_ENABLED = booleanPreferencesKey("jank_detection_enabled")
        val FPS_THRESHOLD = intPreferencesKey("fps_threshold")
        val DEFAULT_EXPORT_FORMAT = stringPreferencesKey("default_export_format")
        val DEFAULT_EXPORT_RANGE = stringPreferencesKey("default_export_range")
        val DATA_RETENTION_DAYS = intPreferencesKey("data_retention_days")
        val AUTO_DELETE_OLD_DATA = booleanPreferencesKey("auto_delete_old_data")
    }
    
    val monitoringSettings: Flow<MonitoringSettings> = context.advancedDataStore.data.map { prefs ->
        MonitoringSettings(
            updateIntervalMs = prefs[Keys.UPDATE_INTERVAL_MS] ?: MonitoringSettings.DEFAULT.updateIntervalMs,
            showCpuChart = prefs[Keys.SHOW_CPU_CHART] ?: MonitoringSettings.DEFAULT.showCpuChart,
            showRamChart = prefs[Keys.SHOW_RAM_CHART] ?: MonitoringSettings.DEFAULT.showRamChart,
            showTempChart = prefs[Keys.SHOW_TEMP_CHART] ?: MonitoringSettings.DEFAULT.showTempChart,
            showNetworkChart = prefs[Keys.SHOW_NETWORK_CHART] ?: MonitoringSettings.DEFAULT.showNetworkChart,
            showFpsChart = prefs[Keys.SHOW_FPS_CHART] ?: MonitoringSettings.DEFAULT.showFpsChart,
            chartHeight = prefs[Keys.CHART_HEIGHT]?.let { ChartHeight.valueOf(it) } ?: MonitoringSettings.DEFAULT.chartHeight,
            chartHistorySize = prefs[Keys.CHART_HISTORY_SIZE] ?: MonitoringSettings.DEFAULT.chartHistorySize,
            peakNotificationsEnabled = prefs[Keys.PEAK_NOTIFICATIONS_ENABLED] ?: MonitoringSettings.DEFAULT.peakNotificationsEnabled,
            peakNotificationIntervalMs = prefs[Keys.PEAK_NOTIFICATION_INTERVAL_MS] ?: MonitoringSettings.DEFAULT.peakNotificationIntervalMs,
            showCpuPeak = prefs[Keys.SHOW_CPU_PEAK] ?: MonitoringSettings.DEFAULT.showCpuPeak,
            showRamPeak = prefs[Keys.SHOW_RAM_PEAK] ?: MonitoringSettings.DEFAULT.showRamPeak,
            showTempPeak = prefs[Keys.SHOW_TEMP_PEAK] ?: MonitoringSettings.DEFAULT.showTempPeak,
            showNetPeak = prefs[Keys.SHOW_NET_PEAK] ?: MonitoringSettings.DEFAULT.showNetPeak,
            showFpsPeak = prefs[Keys.SHOW_FPS_PEAK] ?: MonitoringSettings.DEFAULT.showFpsPeak,
            toastDurationMs = prefs[Keys.TOAST_DURATION_MS] ?: MonitoringSettings.DEFAULT.toastDurationMs,
            show30sAverage = prefs[Keys.SHOW_30S_AVERAGE] ?: MonitoringSettings.DEFAULT.show30sAverage,
            show1mAverage = prefs[Keys.SHOW_1M_AVERAGE] ?: MonitoringSettings.DEFAULT.show1mAverage,
            show5mAverage = prefs[Keys.SHOW_5M_AVERAGE] ?: MonitoringSettings.DEFAULT.show5mAverage,
            showPercentiles = prefs[Keys.SHOW_PERCENTILES] ?: MonitoringSettings.DEFAULT.showPercentiles,
            fpsMonitoringEnabled = prefs[Keys.FPS_MONITORING_ENABLED] ?: MonitoringSettings.DEFAULT.fpsMonitoringEnabled,
            jankDetectionEnabled = prefs[Keys.JANK_DETECTION_ENABLED] ?: MonitoringSettings.DEFAULT.jankDetectionEnabled,
            fpsThreshold = prefs[Keys.FPS_THRESHOLD] ?: MonitoringSettings.DEFAULT.fpsThreshold,
            defaultExportFormat = prefs[Keys.DEFAULT_EXPORT_FORMAT]?.let { ExportFormat.fromString(it) } ?: MonitoringSettings.DEFAULT.defaultExportFormat,
            defaultExportRange = prefs[Keys.DEFAULT_EXPORT_RANGE]?.let { TimeRange.valueOf(it) } ?: MonitoringSettings.DEFAULT.defaultExportRange,
            dataRetentionDays = prefs[Keys.DATA_RETENTION_DAYS] ?: MonitoringSettings.DEFAULT.dataRetentionDays,
            autoDeleteOldData = prefs[Keys.AUTO_DELETE_OLD_DATA] ?: MonitoringSettings.DEFAULT.autoDeleteOldData
        )
    }
    
    suspend fun getSettings(): MonitoringSettings = monitoringSettings.first()
    
    suspend fun saveSettings(settings: MonitoringSettings) {
        try {
            context.advancedDataStore.edit { prefs ->
                prefs[Keys.UPDATE_INTERVAL_MS] = settings.updateIntervalMs
                prefs[Keys.SHOW_CPU_CHART] = settings.showCpuChart
                prefs[Keys.SHOW_RAM_CHART] = settings.showRamChart
                prefs[Keys.SHOW_TEMP_CHART] = settings.showTempChart
                prefs[Keys.SHOW_NETWORK_CHART] = settings.showNetworkChart
                prefs[Keys.SHOW_FPS_CHART] = settings.showFpsChart
                prefs[Keys.CHART_HEIGHT] = settings.chartHeight.name
                prefs[Keys.CHART_HISTORY_SIZE] = settings.chartHistorySize
                prefs[Keys.PEAK_NOTIFICATIONS_ENABLED] = settings.peakNotificationsEnabled
                prefs[Keys.PEAK_NOTIFICATION_INTERVAL_MS] = settings.peakNotificationIntervalMs
                prefs[Keys.SHOW_CPU_PEAK] = settings.showCpuPeak
                prefs[Keys.SHOW_RAM_PEAK] = settings.showRamPeak
                prefs[Keys.SHOW_TEMP_PEAK] = settings.showTempPeak
                prefs[Keys.SHOW_NET_PEAK] = settings.showNetPeak
                prefs[Keys.SHOW_FPS_PEAK] = settings.showFpsPeak
                prefs[Keys.TOAST_DURATION_MS] = settings.toastDurationMs
                prefs[Keys.SHOW_30S_AVERAGE] = settings.show30sAverage
                prefs[Keys.SHOW_1M_AVERAGE] = settings.show1mAverage
                prefs[Keys.SHOW_5M_AVERAGE] = settings.show5mAverage
                prefs[Keys.SHOW_PERCENTILES] = settings.showPercentiles
                prefs[Keys.FPS_MONITORING_ENABLED] = settings.fpsMonitoringEnabled
                prefs[Keys.JANK_DETECTION_ENABLED] = settings.jankDetectionEnabled
                prefs[Keys.FPS_THRESHOLD] = settings.fpsThreshold
                prefs[Keys.DEFAULT_EXPORT_FORMAT] = when (settings.defaultExportFormat) {
                    is ExportFormat.CSV -> "CSV"
                    is ExportFormat.TXT -> "TXT"
                    is ExportFormat.JSON -> "JSON"
                }
                prefs[Keys.DEFAULT_EXPORT_RANGE] = settings.defaultExportRange.name
                prefs[Keys.DATA_RETENTION_DAYS] = settings.dataRetentionDays
                prefs[Keys.AUTO_DELETE_OLD_DATA] = settings.autoDeleteOldData
            }
            Timber.d("Advanced settings saved")
        } catch (e: Exception) {
            Timber.e(e, "Failed to save advanced settings")
        }
    }
    
    suspend fun updateUpdateInterval(intervalMs: Long) {
        context.advancedDataStore.edit { prefs ->
            prefs[Keys.UPDATE_INTERVAL_MS] = intervalMs.coerceIn(
                UpdateInterval.MIN_INTERVAL_MS, 
                UpdateInterval.MAX_INTERVAL_MS
            )
        }
    }
    
    suspend fun updateFpsMonitoring(enabled: Boolean) {
        context.advancedDataStore.edit { prefs ->
            prefs[Keys.FPS_MONITORING_ENABLED] = enabled
        }
    }
    
    suspend fun updatePeakNotifications(enabled: Boolean) {
        context.advancedDataStore.edit { prefs ->
            prefs[Keys.PEAK_NOTIFICATIONS_ENABLED] = enabled
        }
    }
}
