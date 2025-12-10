package com.sysmetrics.app.data.repository

import com.sysmetrics.app.data.model.OverlayConfig
import com.sysmetrics.app.data.source.PreferencesDataSource
import com.sysmetrics.app.domain.repository.IPreferencesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for overlay preferences and configuration.
 * Implements [IPreferencesRepository] for proper abstraction.
 */
@Singleton
class PreferencesRepository @Inject constructor(
    private val preferencesDataSource: PreferencesDataSource
) : IPreferencesRepository {
    
    /**
     * Observes overlay configuration changes.
     */
    override val overlayConfig: Flow<OverlayConfig> = preferencesDataSource.overlayConfig

    /**
     * Observes overlay enabled state.
     */
    override val isOverlayEnabled: Flow<Boolean> = preferencesDataSource.isOverlayEnabled

    /**
     * Saves overlay configuration.
     */
    override suspend fun saveConfig(config: OverlayConfig) {
        preferencesDataSource.saveOverlayConfig(config)
    }

    /**
     * Sets overlay enabled state.
     */
    override suspend fun setOverlayEnabled(enabled: Boolean) {
        preferencesDataSource.setOverlayEnabled(enabled)
    }

    /**
     * Updates overlay position.
     */
    override suspend fun updatePosition(x: Int, y: Int) {
        preferencesDataSource.updatePosition(x, y)
    }
}
