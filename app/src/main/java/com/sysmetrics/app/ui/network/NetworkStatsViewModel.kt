package com.sysmetrics.app.ui.network

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sysmetrics.app.data.model.network.NetworkAlert
import com.sysmetrics.app.data.model.network.NetworkAlertConfig
import com.sysmetrics.app.data.model.network.NetworkDisplayMode
import com.sysmetrics.app.data.model.network.NetworkTrafficStats
import com.sysmetrics.app.data.model.network.NetworkTypeInfo
import com.sysmetrics.app.data.model.network.PerAppTrafficStats
import com.sysmetrics.app.domain.usecase.network.GetNetworkStatsUseCase
import com.sysmetrics.app.domain.usecase.network.MonitorNetworkTrafficUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * ViewModel for network statistics UI.
 * Manages UI state and coordinates between use cases.
 *
 * @param getNetworkStatsUseCase Use case for getting network stats
 * @param monitorNetworkTrafficUseCase Use case for continuous monitoring
 */
class NetworkStatsViewModel(
    private val getNetworkStatsUseCase: GetNetworkStatsUseCase,
    private val monitorNetworkTrafficUseCase: MonitorNetworkTrafficUseCase
) : ViewModel() {

    companion object {
        private const val TAG = "NET_STATS_VM"
    }

    /**
     * UI State for network statistics screen.
     */
    data class NetworkUiState(
        val trafficStats: NetworkTrafficStats = NetworkTrafficStats.EMPTY,
        val networkType: NetworkTypeInfo = NetworkTypeInfo.DISCONNECTED,
        val perAppStats: List<PerAppTrafficStats> = emptyList(),
        val displayMode: NetworkDisplayMode = NetworkDisplayMode.COMPACT,
        val alertConfig: NetworkAlertConfig = NetworkAlertConfig.DEFAULT,
        val isMonitoring: Boolean = false,
        val isLoading: Boolean = false,
        val isNativeAvailable: Boolean = false,
        val isMonitoringAvailable: Boolean = false,
        val error: String? = null
    ) {
        val isConnected: Boolean
            get() = networkType.type != com.sysmetrics.app.data.model.network.NetworkTypeEnum.NONE

        val hasActiveTraffic: Boolean
            get() = trafficStats.ingressBytesPerSec > 0 || trafficStats.egressBytesPerSec > 0
    }

    // UI State
    private val _uiState = MutableStateFlow(NetworkUiState())
    val uiState: StateFlow<NetworkUiState> = _uiState.asStateFlow()

    // Alerts
    private val _alerts = MutableSharedFlow<NetworkAlert>(extraBufferCapacity = 10)
    val alerts: SharedFlow<NetworkAlert> = _alerts.asSharedFlow()

    // Monitoring job
    private var monitoringJob: Job? = null

    init {
        // Check availability on init
        _uiState.update { state ->
            state.copy(
                isNativeAvailable = getNetworkStatsUseCase.isNativeAvailable(),
                isMonitoringAvailable = getNetworkStatsUseCase.isMonitoringAvailable()
            )
        }

        // Load initial state
        loadInitialState()

        // Observe alerts
        monitorNetworkTrafficUseCase.observeAlerts()
            .onEach { alert -> _alerts.emit(alert) }
            .catch { e -> Timber.tag(TAG).e(e, "Error observing alerts") }
            .launchIn(viewModelScope)
    }

    /**
     * Loads initial network state.
     */
    private fun loadInitialState() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                val state = getNetworkStatsUseCase()
                val alertConfig = monitorNetworkTrafficUseCase.getAlertConfig()

                _uiState.update { current ->
                    current.copy(
                        trafficStats = state.trafficStats,
                        networkType = state.networkType,
                        perAppStats = state.perAppStats,
                        alertConfig = alertConfig,
                        isNativeAvailable = state.isNativeAvailable,
                        isMonitoringAvailable = state.isMonitoringAvailable,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error loading initial state")
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    /**
     * Starts continuous monitoring.
     *
     * @param intervalMs Update interval in milliseconds
     */
    fun startMonitoring(intervalMs: Long = 1000L) {
        if (monitoringJob?.isActive == true) {
            Timber.tag(TAG).d("Monitoring already active")
            return
        }

        Timber.tag(TAG).d("Starting monitoring with interval ${intervalMs}ms")

        monitoringJob = monitorNetworkTrafficUseCase.observeAll(intervalMs)
            .onEach { state ->
                _uiState.update { current ->
                    current.copy(
                        trafficStats = state.trafficStats,
                        networkType = state.networkType,
                        perAppStats = state.perAppStats,
                        alertConfig = state.alertConfig,
                        isMonitoring = true,
                        error = null
                    )
                }
            }
            .catch { e ->
                Timber.tag(TAG).e(e, "Error in monitoring flow")
                _uiState.update { it.copy(error = e.message, isMonitoring = false) }
            }
            .launchIn(viewModelScope)

        _uiState.update { it.copy(isMonitoring = true) }
    }

    /**
     * Stops monitoring.
     */
    fun stopMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = null
        _uiState.update { it.copy(isMonitoring = false) }
        Timber.tag(TAG).d("Monitoring stopped")
    }

    /**
     * Refreshes current stats (one-time fetch).
     */
    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                val state = getNetworkStatsUseCase()
                _uiState.update { current ->
                    current.copy(
                        trafficStats = state.trafficStats,
                        networkType = state.networkType,
                        perAppStats = state.perAppStats,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error refreshing stats")
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    /**
     * Sets display mode.
     */
    fun setDisplayMode(mode: NetworkDisplayMode) {
        _uiState.update { it.copy(displayMode = mode) }
        Timber.tag(TAG).d("Display mode changed to $mode")
    }

    /**
     * Cycles through display modes.
     */
    fun cycleDisplayMode() {
        val modes = NetworkDisplayMode.values()
        val currentIndex = modes.indexOf(_uiState.value.displayMode)
        val nextIndex = (currentIndex + 1) % modes.size
        setDisplayMode(modes[nextIndex])
    }

    /**
     * Updates alert configuration.
     */
    fun updateAlertConfig(config: NetworkAlertConfig) {
        viewModelScope.launch {
            try {
                monitorNetworkTrafficUseCase.setAlertConfig(config)
                _uiState.update { it.copy(alertConfig = config) }
                Timber.tag(TAG).d("Alert config updated")
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error updating alert config")
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    /**
     * Resets baseline and peak values.
     */
    fun resetBaseline() {
        monitorNetworkTrafficUseCase.resetBaseline()
        refresh()
        Timber.tag(TAG).d("Baseline reset")
    }

    /**
     * Clears error state.
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Gets formatted stats string for current display mode.
     */
    fun getFormattedStats(): String {
        val state = _uiState.value
        return when (state.displayMode) {
            NetworkDisplayMode.COMPACT -> state.trafficStats.formatCompact()
            NetworkDisplayMode.EXTENDED -> state.trafficStats.formatExtended()
            NetworkDisplayMode.PER_APP -> state.perAppStats.take(3).joinToString("\n") { it.formatDisplay() }
            NetworkDisplayMode.COMBINED -> buildString {
                append(state.trafficStats.formatCompact())
                append(" | ${state.networkType.formatCompact()}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopMonitoring()
        Timber.tag(TAG).d("ViewModel cleared")
    }
}

/**
 * Factory for creating NetworkStatsViewModel.
 */
class NetworkStatsViewModelFactory(
    private val getNetworkStatsUseCase: GetNetworkStatsUseCase,
    private val monitorNetworkTrafficUseCase: MonitorNetworkTrafficUseCase
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NetworkStatsViewModel::class.java)) {
            return NetworkStatsViewModel(
                getNetworkStatsUseCase,
                monitorNetworkTrafficUseCase
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
