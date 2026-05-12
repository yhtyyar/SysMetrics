package com.sysmetrics.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sysmetrics.app.data.model.advanced.*
import com.sysmetrics.app.data.repository.SettingsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * ViewModel for advanced monitoring settings screen (SettingsFragment).
 * Manages MonitoringSettings: charts, peak notifications, analytics, FPS, export, retention.
 *
 * Renamed from SettingsViewModel to avoid ambiguity with ui.SettingsViewModel
 * which manages OverlayConfig for SettingsActivity.
 */
class MonitoringSettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SettingsEvent>()
    val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                settingsRepository.settingsFlow.collect { settings ->
                    _uiState.update {
                        it.copy(
                            settings = settings,
                            isLoading = false,
                            error = null
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load settings")
                _uiState.update {
                    it.copy(isLoading = false, error = e.message)
                }
            }
        }
    }

    fun updateUpdateInterval(interval: UpdateInterval) {
        viewModelScope.launch {
            val current = _uiState.value.settings
            saveSettings(current.copy(updateIntervalMs = interval.intervalMs))
        }
    }

    fun updateChartSettings(
        showCpu: Boolean? = null,
        showRam: Boolean? = null,
        showTemp: Boolean? = null,
        showNetwork: Boolean? = null,
        showFps: Boolean? = null,
        height: ChartHeight? = null
    ) {
        viewModelScope.launch {
            val current = _uiState.value.settings
            saveSettings(current.copy(
                showCpuChart = showCpu ?: current.showCpuChart,
                showRamChart = showRam ?: current.showRamChart,
                showTempChart = showTemp ?: current.showTempChart,
                showNetworkChart = showNetwork ?: current.showNetworkChart,
                showFpsChart = showFps ?: current.showFpsChart,
                chartHeight = height ?: current.chartHeight
            ))
        }
    }

    fun updatePeakNotificationSettings(
        enabled: Boolean? = null,
        interval: PeakNotificationInterval? = null,
        showCpu: Boolean? = null,
        showRam: Boolean? = null,
        showTemp: Boolean? = null,
        showNet: Boolean? = null,
        showFps: Boolean? = null,
        toastDuration: Int? = null
    ) {
        viewModelScope.launch {
            val current = _uiState.value.settings
            saveSettings(current.copy(
                peakNotificationsEnabled = enabled ?: current.peakNotificationsEnabled,
                peakNotificationIntervalMs = interval?.intervalMs ?: current.peakNotificationIntervalMs,
                showCpuPeak = showCpu ?: current.showCpuPeak,
                showRamPeak = showRam ?: current.showRamPeak,
                showTempPeak = showTemp ?: current.showTempPeak,
                showNetPeak = showNet ?: current.showNetPeak,
                showFpsPeak = showFps ?: current.showFpsPeak,
                toastDurationMs = toastDuration ?: current.toastDurationMs
            ))
        }
    }

    fun updateAnalyticsSettings(
        show30s: Boolean? = null,
        show1m: Boolean? = null,
        show5m: Boolean? = null,
        showPercentiles: Boolean? = null
    ) {
        viewModelScope.launch {
            val current = _uiState.value.settings
            saveSettings(current.copy(
                show30sAverage = show30s ?: current.show30sAverage,
                show1mAverage = show1m ?: current.show1mAverage,
                show5mAverage = show5m ?: current.show5mAverage,
                showPercentiles = showPercentiles ?: current.showPercentiles
            ))
        }
    }

    fun updateFpsSettings(enabled: Boolean? = null, jankDetection: Boolean? = null, threshold: Int? = null) {
        viewModelScope.launch {
            val current = _uiState.value.settings
            saveSettings(current.copy(
                fpsMonitoringEnabled = enabled ?: current.fpsMonitoringEnabled,
                jankDetectionEnabled = jankDetection ?: current.jankDetectionEnabled,
                fpsThreshold = threshold ?: current.fpsThreshold
            ))
        }
    }

    fun updateExportSettings(format: ExportFormat? = null, range: TimeRange? = null) {
        viewModelScope.launch {
            val current = _uiState.value.settings
            saveSettings(current.copy(
                defaultExportFormat = format ?: current.defaultExportFormat,
                defaultExportRange = range ?: current.defaultExportRange
            ))
        }
    }

    fun updateDataRetention(days: Int? = null, autoDelete: Boolean? = null) {
        viewModelScope.launch {
            val current = _uiState.value.settings
            saveSettings(current.copy(
                dataRetentionDays = days ?: current.dataRetentionDays,
                autoDeleteOldData = autoDelete ?: current.autoDeleteOldData
            ))
        }
    }

    private suspend fun saveSettings(settings: MonitoringSettings) {
        _uiState.update { it.copy(isSaving = true) }
        settingsRepository.saveSettings(settings).fold(
            onSuccess = {
                _events.emit(SettingsEvent.SettingsSaved)
                _uiState.update { it.copy(isSaving = false) }
            },
            onFailure = { error ->
                _events.emit(SettingsEvent.Error(error.message ?: "Failed to save"))
                _uiState.update { it.copy(isSaving = false, error = error.message) }
            }
        )
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            saveSettings(MonitoringSettings.DEFAULT)
            _events.emit(SettingsEvent.SettingsReset)
        }
    }

    class Factory(private val repository: SettingsRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MonitoringSettingsViewModel(repository) as T
        }
    }
}

data class SettingsUiState(
    val settings: MonitoringSettings = MonitoringSettings.DEFAULT,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null
)

sealed class SettingsEvent {
    data object SettingsSaved : SettingsEvent()
    data object SettingsReset : SettingsEvent()
    data class Error(val message: String) : SettingsEvent()
}
