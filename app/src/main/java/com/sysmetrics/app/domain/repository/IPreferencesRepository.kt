package com.sysmetrics.app.domain.repository

import com.sysmetrics.app.data.model.OverlayConfig
import kotlinx.coroutines.flow.Flow

/**
 * Interface for preferences repository.
 * Defines the contract for overlay configuration management.
 */
interface IPreferencesRepository {
    
    /**
     * Observes overlay configuration changes.
     */
    val overlayConfig: Flow<OverlayConfig>
    
    /**
     * Observes overlay enabled state.
     */
    val isOverlayEnabled: Flow<Boolean>
    
    /**
     * Saves overlay configuration.
     */
    suspend fun saveConfig(config: OverlayConfig)
    
    /**
     * Sets overlay enabled state.
     */
    suspend fun setOverlayEnabled(enabled: Boolean)
    
    /**
     * Updates overlay position.
     */
    suspend fun updatePosition(x: Int, y: Int)
}
