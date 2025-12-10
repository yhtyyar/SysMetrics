package com.sysmetrics.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sysmetrics.app.data.model.OverlayConfig
import com.sysmetrics.app.data.model.SystemMetrics
import com.sysmetrics.app.domain.usecase.GetSystemMetricsUseCase
import com.sysmetrics.app.domain.usecase.ManageOverlayConfigUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for MainActivity.
 * Manages overlay state and provides system metrics preview.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val getSystemMetricsUseCase: GetSystemMetricsUseCase,
    private val manageOverlayConfigUseCase: ManageOverlayConfigUseCase
) : ViewModel() {

    private val _overlayEnabled = MutableStateFlow(false)
    val overlayEnabled: StateFlow<Boolean> = _overlayEnabled.asStateFlow()

    val overlayConfig: StateFlow<OverlayConfig> = manageOverlayConfigUseCase
        .observeConfig()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = OverlayConfig.DEFAULT
        )

    val systemMetrics: StateFlow<SystemMetrics> = overlayConfig
        .flatMapLatest { config ->
            getSystemMetricsUseCase(config.updateIntervalMs)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SystemMetrics.EMPTY
        )

    init {
        viewModelScope.launch {
            manageOverlayConfigUseCase.observeEnabled().collect { enabled ->
                _overlayEnabled.value = enabled
            }
        }
    }

    /**
     * Toggles overlay on/off state.
     */
    fun toggleOverlay() {
        viewModelScope.launch {
            val newState = !_overlayEnabled.value
            manageOverlayConfigUseCase.setEnabled(newState)
        }
    }

    /**
     * Sets overlay enabled state.
     */
    fun setOverlayEnabled(enabled: Boolean) {
        viewModelScope.launch {
            manageOverlayConfigUseCase.setEnabled(enabled)
        }
    }

    /**
     * Saves overlay configuration.
     */
    fun saveConfig(config: OverlayConfig) {
        viewModelScope.launch {
            manageOverlayConfigUseCase.saveConfig(config)
        }
    }
}
