package com.sysmetrics.app.data.source

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sysmetrics.app.data.model.OverlayConfig
import com.sysmetrics.app.data.model.OverlayPosition
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "sysmetrics_prefs")

/**
 * DataStore-based preferences storage for overlay configuration.
 */

class PreferencesDataSource constructor(
    private val context: Context
) {
    private object Keys {
        val POSITION_X = intPreferencesKey("position_x")
        val POSITION_Y = intPreferencesKey("position_y")
        val POSITION = stringPreferencesKey("position")
        val UPDATE_INTERVAL = longPreferencesKey("update_interval")
        val OPACITY = floatPreferencesKey("opacity")
        val SHOW_CPU = booleanPreferencesKey("show_cpu")
        val SHOW_RAM = booleanPreferencesKey("show_ram")
        val SHOW_TEMPERATURE = booleanPreferencesKey("show_temperature")
        val SHOW_TIME = booleanPreferencesKey("show_time")
        val OVERLAY_ENABLED = booleanPreferencesKey("overlay_enabled")
    }

    /**
     * Observes overlay configuration changes.
     */
    val overlayConfig: Flow<OverlayConfig> = context.dataStore.data.map { prefs ->
        OverlayConfig(
            positionX = prefs[Keys.POSITION_X] ?: OverlayConfig.DEFAULT.positionX,
            positionY = prefs[Keys.POSITION_Y] ?: OverlayConfig.DEFAULT.positionY,
            position = prefs[Keys.POSITION]?.let { 
                runCatching { OverlayPosition.valueOf(it) }.getOrNull() 
            } ?: OverlayConfig.DEFAULT.position,
            updateIntervalMs = prefs[Keys.UPDATE_INTERVAL] ?: OverlayConfig.DEFAULT.updateIntervalMs,
            opacity = prefs[Keys.OPACITY] ?: OverlayConfig.DEFAULT.opacity,
            showCpu = prefs[Keys.SHOW_CPU] ?: OverlayConfig.DEFAULT.showCpu,
            showRam = prefs[Keys.SHOW_RAM] ?: OverlayConfig.DEFAULT.showRam,
            showTemperature = prefs[Keys.SHOW_TEMPERATURE] ?: OverlayConfig.DEFAULT.showTemperature,
            showTime = prefs[Keys.SHOW_TIME] ?: OverlayConfig.DEFAULT.showTime
        )
    }

    /**
     * Observes overlay enabled state.
     */
    val isOverlayEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.OVERLAY_ENABLED] ?: false
    }

    /**
     * Saves overlay configuration.
     */
    suspend fun saveOverlayConfig(config: OverlayConfig) {
        try {
            context.dataStore.edit { prefs ->
                prefs[Keys.POSITION_X] = config.positionX
                prefs[Keys.POSITION_Y] = config.positionY
                prefs[Keys.POSITION] = config.position.name
                prefs[Keys.UPDATE_INTERVAL] = config.updateIntervalMs
                prefs[Keys.OPACITY] = config.opacity
                prefs[Keys.SHOW_CPU] = config.showCpu
                prefs[Keys.SHOW_RAM] = config.showRam
                prefs[Keys.SHOW_TEMPERATURE] = config.showTemperature
                prefs[Keys.SHOW_TIME] = config.showTime
            }
            Timber.d("Overlay config saved")
        } catch (e: Exception) {
            Timber.e(e, "Failed to save overlay config")
        }
    }

    /**
     * Sets overlay enabled state.
     */
    suspend fun setOverlayEnabled(enabled: Boolean) {
        try {
            context.dataStore.edit { prefs ->
                prefs[Keys.OVERLAY_ENABLED] = enabled
            }
            Timber.d("Overlay enabled: $enabled")
        } catch (e: Exception) {
            Timber.e(e, "Failed to set overlay enabled state")
        }
    }

    /**
     * Updates overlay position.
     */
    suspend fun updatePosition(x: Int, y: Int) {
        try {
            context.dataStore.edit { prefs ->
                prefs[Keys.POSITION_X] = x
                prefs[Keys.POSITION_Y] = y
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to update position")
        }
    }
}
