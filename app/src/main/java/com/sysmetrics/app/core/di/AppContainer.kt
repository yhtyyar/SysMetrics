package com.sysmetrics.app.core.di

import android.content.Context
import com.sysmetrics.app.data.repository.PreferencesRepository
import com.sysmetrics.app.data.repository.SystemMetricsRepository
import com.sysmetrics.app.data.source.*
import com.sysmetrics.app.domain.repository.IPreferencesRepository
import com.sysmetrics.app.domain.repository.ISystemMetricsRepository
import com.sysmetrics.app.domain.usecase.GetSystemMetricsUseCase
import com.sysmetrics.app.domain.usecase.ManageOverlayConfigUseCase
import com.sysmetrics.app.native_bridge.*
import com.sysmetrics.app.utils.*

/**
 * Manual Dependency Injection Container.
 * Creates and manages application dependencies.
 */
class AppContainer(private val context: Context) {
    
    // Core
    private val dispatcherProvider: DispatcherProvider by lazy {
        DefaultDispatcherProvider()
    }
    
    // Device Utils
    val deviceUtils: DeviceUtils by lazy {
        DeviceUtils(context)
    }
    
    // Native Bridge
    private val nativeCpuMetricsCollector by lazy {
        NativeCpuMetricsCollector()
    }
    
    private val fallbackCpuMetricsCollector by lazy {
        FallbackCpuMetricsCollector()
    }
    
    private val nativeStringFormatter by lazy {
        NativeStringFormatter()
    }
    
    private val fallbackStringFormatter by lazy {
        FallbackStringFormatter()
    }
    
    private val metricsCollectorFactory by lazy {
        MetricsCollectorFactory(
            nativeCpuMetricsCollector,
            fallbackCpuMetricsCollector,
            nativeStringFormatter,
            fallbackStringFormatter
        )
    }
    
    val cpuMetricsCollector by lazy {
        metricsCollectorFactory.createCpuCollector()
    }
    
    val stringFormatter by lazy {
        metricsCollectorFactory.createStringFormatter()
    }
    
    // Data Sources
    private val systemDataSource by lazy {
        SystemDataSource(dispatcherProvider)
    }
    
    private val preferencesDataSource by lazy {
        PreferencesDataSource(context)
    }
    
    private val gpuDataSource by lazy {
        GpuDataSource(dispatcherProvider)
    }
    
    private val networkDataSource by lazy {
        NetworkDataSource(dispatcherProvider)
    }
    
    private val batteryDataSource by lazy {
        BatteryDataSource(context, dispatcherProvider)
    }
    
    // Repositories
    val systemMetricsRepository: ISystemMetricsRepository by lazy {
        SystemMetricsRepository(
            systemDataSource,
            gpuDataSource,
            networkDataSource,
            batteryDataSource
        )
    }
    
    val preferencesRepository: IPreferencesRepository by lazy {
        PreferencesRepository(preferencesDataSource)
    }
    
    // Use Cases
    val getSystemMetricsUseCase by lazy {
        GetSystemMetricsUseCase(systemMetricsRepository)
    }
    
    val manageOverlayConfigUseCase by lazy {
        ManageOverlayConfigUseCase(preferencesRepository)
    }
    
    // Collectors
    val metricsCollector by lazy {
        MetricsCollector(context, systemDataSource, dispatcherProvider)
    }
    
    val processStatsCollector by lazy {
        ProcessStatsCollector(context, dispatcherProvider, cpuMetricsCollector)
    }
    
    // Utils
    val adaptivePerformanceMonitor by lazy {
        AdaptivePerformanceMonitor()
    }
}
