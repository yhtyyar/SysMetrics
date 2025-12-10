package com.sysmetrics.app.data.repository

import com.sysmetrics.app.data.model.OverlayConfig
import com.sysmetrics.app.data.source.PreferencesDataSource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for overlay preferences and configuration.
 */
@Singleton
class PreferencesRepository @Inject constructor(
    private val preferencesDataSource: PreferencesDataSource
) {
    /**
     * Observes overlay configuration changes.
     */
    val overlayConfig: Flow<OverlayConfig> = preferencesDataSource.overlayConfig

    /**
     * Observes overlay enabled state.
     */
    val isOverlayEnabled: Flow<Boolean> = preferencesDataSource.isOverlayEnabled

    /**
     * Saves overlay configuration.
     */
    suspend fun saveConfig(config: OverlayConfig) {
        preferencesDataSource.saveOverlayConfig(config)
    }

    /**
     * Sets overlay enabled state.
     */
    suspend fun setOverlayEnabled(enabled: Boolean) {
        preferencesDataSource.setOverlayEnabled(enabled)
    }

    /**
     * Updates overlay position.
     */
    suspend fun updatePosition(x: Int, y: Int) {
        preferencesDataSource.updatePosition(x, y)
    }
}
