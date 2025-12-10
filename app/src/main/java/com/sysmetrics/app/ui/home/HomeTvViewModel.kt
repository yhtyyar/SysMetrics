package com.sysmetrics.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sysmetrics.app.data.repository.SystemMetricsRepository
import com.sysmetrics.app.data.source.SystemDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for HomeTvFragment
 * Manages real-time system metrics monitoring
 */
@HiltViewModel
class HomeTvViewModel @Inject constructor(
    private val metricsRepository: SystemMetricsRepository,
    private val systemDataSource: SystemDataSource
) : ViewModel() {

    private val _cpuUsage = MutableStateFlow(0f)
    val cpuUsage: StateFlow<Float> = _cpuUsage.asStateFlow()

    private val _memoryInfo = MutableStateFlow(MemoryData())
    val memoryInfo: StateFlow<MemoryData> = _memoryInfo.asStateFlow()

    private val _temperature = MutableStateFlow(0f)
    val temperature: StateFlow<Float> = _temperature.asStateFlow()

    private val _systemInfo = MutableStateFlow(SystemInfoData())
    val systemInfo: StateFlow<SystemInfoData> = _systemInfo.asStateFlow()

    private val updateIntervalMs = 500L // Update every 500ms

    init {
        startMonitoring()
        loadSystemInfo()
    }

    private fun startMonitoring() {
        // Monitor CPU usage
        viewModelScope.launch {
            var previousStats = systemDataSource.readCpuStats()
            while (isActive) {
                delay(updateIntervalMs)
                
                val currentStats = systemDataSource.readCpuStats()
                val usage = calculateCpuUsage(previousStats, currentStats)
                _cpuUsage.value = usage
                previousStats = currentStats
            }
        }

        // Monitor memory
        viewModelScope.launch {
            while (isActive) {
                delay(updateIntervalMs)
                
                val memInfo = systemDataSource.readMemoryInfo()
                _memoryInfo.value = MemoryData(
                    totalMemory = memInfo.totalKb * 1024, // Convert to bytes
                    usedMemory = memInfo.usedKb * 1024,
                    availableMemory = memInfo.availableKb * 1024
                )
            }
        }

        // Monitor temperature
        viewModelScope.launch {
            while (isActive) {
                delay(updateIntervalMs)
                
                val tempInfo = systemDataSource.readTemperature()
                _temperature.value = tempInfo.cpuTempCelsius
            }
        }

        // Update uptime
        viewModelScope.launch {
            while (isActive) {
                delay(1000L) // Update uptime every second
                
                _systemInfo.value = _systemInfo.value.copy(
                    uptimeMillis = android.os.SystemClock.elapsedRealtime()
                )
            }
        }
    }

    private fun loadSystemInfo() {
        _systemInfo.value = SystemInfoData(
            cpuCores = systemDataSource.getCpuCoreCount(),
            uptimeMillis = android.os.SystemClock.elapsedRealtime()
        )
    }

    private fun calculateCpuUsage(
        previous: com.sysmetrics.app.data.model.CpuStats,
        current: com.sysmetrics.app.data.model.CpuStats
    ): Float {
        val totalDelta = (current.total() - previous.total()).toFloat()
        if (totalDelta <= 0) return 0f

        val idleDelta = (current.idle - previous.idle).toFloat()
        val usage = ((totalDelta - idleDelta) / totalDelta) * 100f

        return usage.coerceIn(0f, 100f)
    }

    data class MemoryData(
        val totalMemory: Long = 0L,
        val usedMemory: Long = 0L,
        val availableMemory: Long = 0L
    )

    data class SystemInfoData(
        val cpuCores: Int = 0,
        val uptimeMillis: Long = 0L
    )
}
