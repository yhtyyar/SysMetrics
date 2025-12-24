package com.sysmetrics.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sysmetrics.app.data.model.OverlayConfig
import com.sysmetrics.app.data.model.SystemMetrics
import com.sysmetrics.app.domain.repository.IMetricsHistoryRepository
import com.sysmetrics.app.domain.repository.MetricsStatistics
import com.sysmetrics.app.domain.usecase.ExportMetricsUseCase
import com.sysmetrics.app.domain.usecase.GetSystemMetricsUseCase
import com.sysmetrics.app.domain.usecase.ManageOverlayConfigUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for MainActivityOverlay.
 * Manages system metrics, overlay state, and export functionality.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val getSystemMetricsUseCase: GetSystemMetricsUseCase,
    private val manageOverlayConfigUseCase: ManageOverlayConfigUseCase,
    private val historyRepository: IMetricsHistoryRepository,
    private val exportMetricsUseCase: ExportMetricsUseCase
) : ViewModel() {

    companion object {
        private const val TAG = "MainViewModel"
        private const val DEFAULT_UPDATE_INTERVAL = 1000L
    }

    // ============== UI State ==============
    
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    // ============== Events ==============
    
    private val _events = MutableSharedFlow<MainEvent>()
    val events: SharedFlow<MainEvent> = _events.asSharedFlow()

    // ============== Metrics ==============
    
    val systemMetrics: StateFlow<SystemMetrics> = getSystemMetricsUseCase(DEFAULT_UPDATE_INTERVAL)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SystemMetrics.EMPTY
        )

    val overlayConfig: StateFlow<OverlayConfig> = manageOverlayConfigUseCase.observeConfig()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = OverlayConfig.DEFAULT
        )

    val overlayEnabled: StateFlow<Boolean> = manageOverlayConfigUseCase.observeEnabled()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    init {
        Timber.tag(TAG).d("MainViewModel initialized")
        loadStatistics()
    }

    // ============== Overlay Control ==============

    fun setOverlayEnabled(enabled: Boolean) {
        viewModelScope.launch {
            manageOverlayConfigUseCase.setEnabled(enabled)
            _uiState.value = _uiState.value.copy(isOverlayActive = enabled)
        }
    }

    fun toggleOverlay() {
        setOverlayEnabled(!overlayEnabled.value)
    }

    fun saveConfig(config: OverlayConfig) {
        viewModelScope.launch {
            manageOverlayConfigUseCase.saveConfig(config)
        }
    }

    // ============== History & Statistics ==============

    fun loadStatistics() {
        viewModelScope.launch {
            try {
                val stats = historyRepository.getStatistics(24)
                _uiState.value = _uiState.value.copy(
                    statistics = stats,
                    historyCount = stats.totalEntries
                )
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to load statistics")
            }
        }
    }

    fun saveCurrentMetrics() {
        viewModelScope.launch {
            try {
                historyRepository.saveMetrics(systemMetrics.value)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to save metrics")
            }
        }
    }

    // ============== Export ==============

    fun exportMetrics(hours: Int = 24, format: ExportMetricsUseCase.ExportFormat) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true)
            
            val result = exportMetricsUseCase.export(hours, format)
            
            _uiState.value = _uiState.value.copy(isExporting = false)
            
            if (result.success && result.shareIntent != null) {
                _events.emit(MainEvent.ShareExport(result.shareIntent))
            } else {
                _events.emit(MainEvent.ShowError(result.errorMessage ?: "Export failed"))
            }
        }
    }

    // ============== Cleanup ==============

    fun cleanupOldHistory() {
        viewModelScope.launch {
            val deleted = historyRepository.cleanupOldEntries()
            if (deleted > 0) {
                loadStatistics()
            }
        }
    }
}

/**
 * UI state for main screen.
 */
data class MainUiState(
    val isOverlayActive: Boolean = false,
    val isExporting: Boolean = false,
    val statistics: MetricsStatistics? = null,
    val historyCount: Int = 0
)

/**
 * One-time events for main screen.
 */
sealed class MainEvent {
    data class ShareExport(val intent: android.content.Intent) : MainEvent()
    data class ShowError(val message: String) : MainEvent()
    data class ShowMessage(val message: String) : MainEvent()
}
