package com.sysmetrics.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sysmetrics.app.data.model.OverlayConfig
import com.sysmetrics.app.data.model.OverlayPosition
import com.sysmetrics.app.domain.usecase.ManageOverlayConfigUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for SettingsActivity.
 * Manages overlay configuration options.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val manageOverlayConfigUseCase: ManageOverlayConfigUseCase
) : ViewModel() {

    val overlayConfig: StateFlow<OverlayConfig> = manageOverlayConfigUseCase
        .observeConfig()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = OverlayConfig.DEFAULT
        )

    private val _configModified = MutableStateFlow(false)
    val configModified: StateFlow<Boolean> = _configModified.asStateFlow()

    private var pendingConfig: OverlayConfig? = null

    /**
     * Updates a single configuration property.
     */
    fun updateConfig(
        position: OverlayPosition? = null,
        updateIntervalMs: Long? = null,
        opacity: Float? = null,
        showCpu: Boolean? = null,
        showRam: Boolean? = null,
        showTemperature: Boolean? = null
    ) {
        val current = pendingConfig ?: overlayConfig.value
        pendingConfig = current.copy(
            position = position ?: current.position,
            updateIntervalMs = updateIntervalMs ?: current.updateIntervalMs,
            opacity = opacity ?: current.opacity,
            showCpu = showCpu ?: current.showCpu,
            showRam = showRam ?: current.showRam,
            showTemperature = showTemperature ?: current.showTemperature
        )
        _configModified.value = true
    }

    /**
     * Saves pending configuration changes.
     */
    fun saveConfig() {
        pendingConfig?.let { config ->
            viewModelScope.launch {
                manageOverlayConfigUseCase.saveConfig(config)
                pendingConfig = null
                _configModified.value = false
            }
        }
    }

    /**
     * Discards pending configuration changes.
     */
    fun discardChanges() {
        pendingConfig = null
        _configModified.value = false
    }
}
