package com.sysmetrics.app.data.repository

import com.sysmetrics.app.data.model.advanced.*
import com.sysmetrics.app.data.source.AdvancedPreferencesDataSource
import kotlinx.coroutines.flow.Flow
import timber.log.Timber

/**
 * Repository for managing advanced monitoring settings.
 * Provides a clean API for settings access and validation.
 */
class SettingsRepository(
    private val preferencesDataSource: AdvancedPreferencesDataSource
) {
    val settingsFlow: Flow<MonitoringSettings> = preferencesDataSource.monitoringSettings
    
    suspend fun getSettings(): MonitoringSettings = preferencesDataSource.getSettings()
    
    suspend fun saveSettings(settings: MonitoringSettings): Result<Unit> {
        return try {
            val validatedSettings = validateSettings(settings)
            preferencesDataSource.saveSettings(validatedSettings)
            Timber.d("Settings saved successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to save settings")
            Result.failure(e)
        }
    }
    
    suspend fun updateUpdateInterval(interval: UpdateInterval): Result<Unit> {
        return try {
            preferencesDataSource.updateUpdateInterval(interval.intervalMs)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateFpsMonitoring(enabled: Boolean): Result<Unit> {
        return try {
            preferencesDataSource.updateFpsMonitoring(enabled)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updatePeakNotifications(enabled: Boolean): Result<Unit> {
        return try {
            preferencesDataSource.updatePeakNotifications(enabled)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun validateSettings(settings: MonitoringSettings): MonitoringSettings {
        return settings.copy(
            updateIntervalMs = settings.updateIntervalMs.coerceIn(
                UpdateInterval.MIN_INTERVAL_MS,
                UpdateInterval.MAX_INTERVAL_MS
            ),
            toastDurationMs = settings.toastDurationMs.coerceIn(3000, 10000),
            fpsThreshold = settings.fpsThreshold.coerceIn(15, 60),
            chartHistorySize = settings.chartHistorySize.coerceIn(30, 120),
            dataRetentionDays = settings.dataRetentionDays.coerceIn(1, 90)
        )
    }
}
