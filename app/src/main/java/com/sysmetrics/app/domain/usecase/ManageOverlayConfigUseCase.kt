package com.sysmetrics.app.domain.usecase

import com.sysmetrics.app.data.model.OverlayConfig
import com.sysmetrics.app.data.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for managing overlay configuration and state.
 */
class ManageOverlayConfigUseCase @Inject constructor(
    private val repository: PreferencesRepository
) {
    /**
     * Observes overlay configuration changes.
     */
    fun observeConfig(): Flow<OverlayConfig> {
        return repository.overlayConfig
    }

    /**
     * Observes overlay enabled state.
     */
    fun observeEnabled(): Flow<Boolean> {
        return repository.isOverlayEnabled
    }

    /**
     * Saves overlay configuration.
     */
    suspend fun saveConfig(config: OverlayConfig) {
        repository.saveConfig(config)
    }

    /**
     * Sets overlay enabled state.
     */
    suspend fun setEnabled(enabled: Boolean) {
        repository.setOverlayEnabled(enabled)
    }

    /**
     * Updates overlay position coordinates.
     */
    suspend fun updatePosition(x: Int, y: Int) {
        repository.updatePosition(x, y)
    }
}
