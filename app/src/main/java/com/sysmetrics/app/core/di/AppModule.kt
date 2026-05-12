package com.sysmetrics.app.core.di

import android.content.Context
import com.sysmetrics.app.data.local.MetricsDatabase
import com.sysmetrics.app.data.local.dao.MetricsHistoryDao
import com.sysmetrics.app.data.repository.MetricsHistoryRepository
import com.sysmetrics.app.data.repository.PreferencesRepository
import com.sysmetrics.app.data.repository.SystemMetricsRepository
import com.sysmetrics.app.data.source.BatteryDataSource
import com.sysmetrics.app.data.source.GpuDataSource
import com.sysmetrics.app.data.source.NetworkDataSource
import com.sysmetrics.app.data.source.PreferencesDataSource
import com.sysmetrics.app.data.source.SystemDataSource
import com.sysmetrics.app.data.source.network.NetworkStatsDataSource
import com.sysmetrics.app.domain.collector.ICpuMetricsCollector
import com.sysmetrics.app.domain.collector.IMetricsCollector
import com.sysmetrics.app.domain.collector.IProcessStatsCollector
import com.sysmetrics.app.domain.formatter.IStringFormatter
import com.sysmetrics.app.domain.repository.IMetricsHistoryRepository
import com.sysmetrics.app.domain.repository.IPreferencesRepository
import com.sysmetrics.app.domain.repository.ISystemMetricsRepository
import com.sysmetrics.app.domain.usecase.ExportMetricsUseCase
import com.sysmetrics.app.domain.usecase.GetSystemMetricsUseCase
import com.sysmetrics.app.domain.usecase.ManageOverlayConfigUseCase
import com.sysmetrics.app.native_bridge.FallbackCpuMetricsCollector
import com.sysmetrics.app.native_bridge.FallbackStringFormatter
import com.sysmetrics.app.native_bridge.MetricsCollectorFactory
import com.sysmetrics.app.native_bridge.NativeCpuMetricsCollector
import com.sysmetrics.app.native_bridge.NativeStringFormatter
import com.sysmetrics.app.utils.AdaptivePerformanceMonitor
import com.sysmetrics.app.utils.BatteryAwareMonitor
import com.sysmetrics.app.utils.DeviceUtils
import com.sysmetrics.app.utils.MetricsCollector
import com.sysmetrics.app.utils.ProcessStatsCollector
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for application-wide dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // ============== Dispatcher Provider ==============
    
    @Provides
    @Singleton
    fun provideDispatcherProvider(): DispatcherProvider = DefaultDispatcherProvider()

    // ============== Data Sources ==============

    @Provides
    @Singleton
    fun provideSystemDataSource(
        dispatcherProvider: DispatcherProvider
    ): SystemDataSource = SystemDataSource(dispatcherProvider)

    @Provides
    @Singleton
    fun provideGpuDataSource(
        dispatcherProvider: DispatcherProvider
    ): GpuDataSource = GpuDataSource(dispatcherProvider)

    @Provides
    @Singleton
    fun provideNetworkDataSource(
        dispatcherProvider: DispatcherProvider
    ): NetworkDataSource = NetworkDataSource(dispatcherProvider)

    @Provides
    @Singleton
    fun provideBatteryDataSource(
        @ApplicationContext context: Context,
        dispatcherProvider: DispatcherProvider
    ): BatteryDataSource = BatteryDataSource(context, dispatcherProvider)

    @Provides
    @Singleton
    fun providePreferencesDataSource(
        @ApplicationContext context: Context
    ): PreferencesDataSource = PreferencesDataSource(context)

    // ============== Database ==============

    @Provides
    @Singleton
    fun provideMetricsDatabase(
        @ApplicationContext context: Context
    ): MetricsDatabase = MetricsDatabase.getInstance(context)

    @Provides
    @Singleton
    fun provideMetricsHistoryDao(
        database: MetricsDatabase
    ): MetricsHistoryDao = database.metricsHistoryDao()

    // ============== Repositories ==============

    @Provides
    @Singleton
    fun provideSystemMetricsRepository(
        systemDataSource: SystemDataSource,
        gpuDataSource: GpuDataSource,
        networkDataSource: NetworkDataSource,
        batteryDataSource: BatteryDataSource
    ): ISystemMetricsRepository = SystemMetricsRepository(
        systemDataSource,
        gpuDataSource,
        networkDataSource,
        batteryDataSource
    )

    @Provides
    @Singleton
    fun providePreferencesRepository(
        preferencesDataSource: PreferencesDataSource
    ): IPreferencesRepository = PreferencesRepository(preferencesDataSource)

    @Provides
    @Singleton
    fun provideMetricsHistoryRepository(
        metricsHistoryDao: MetricsHistoryDao
    ): IMetricsHistoryRepository = MetricsHistoryRepository(metricsHistoryDao)

    // ============== Use Cases ==============

    @Provides
    @Singleton
    fun provideGetSystemMetricsUseCase(
        repository: ISystemMetricsRepository
    ): GetSystemMetricsUseCase = GetSystemMetricsUseCase(repository)

    @Provides
    @Singleton
    fun provideManageOverlayConfigUseCase(
        repository: IPreferencesRepository
    ): ManageOverlayConfigUseCase = ManageOverlayConfigUseCase(repository)

    @Provides
    @Singleton
    fun provideExportMetricsUseCase(
        @ApplicationContext context: Context,
        historyRepository: IMetricsHistoryRepository
    ): ExportMetricsUseCase = ExportMetricsUseCase(context, historyRepository)

    // ============== Native Bridge ==============

    @Provides @Singleton
    fun provideNativeCpuMetricsCollector(): NativeCpuMetricsCollector = NativeCpuMetricsCollector()

    @Provides @Singleton
    fun provideFallbackCpuMetricsCollector(): FallbackCpuMetricsCollector = FallbackCpuMetricsCollector()

    @Provides @Singleton
    fun provideNativeStringFormatter(): NativeStringFormatter = NativeStringFormatter()

    @Provides @Singleton
    fun provideFallbackStringFormatter(): FallbackStringFormatter = FallbackStringFormatter()

    @Provides @Singleton
    fun provideMetricsCollectorFactory(
        native: NativeCpuMetricsCollector,
        fallback: FallbackCpuMetricsCollector,
        nativeFormatter: NativeStringFormatter,
        fallbackFormatter: FallbackStringFormatter
    ): MetricsCollectorFactory = MetricsCollectorFactory(native, fallback, nativeFormatter, fallbackFormatter)

    @Provides @Singleton
    fun provideICpuMetricsCollector(
        factory: MetricsCollectorFactory
    ): ICpuMetricsCollector = factory.createCpuCollector()

    @Provides @Singleton
    fun provideIStringFormatter(
        factory: MetricsCollectorFactory
    ): IStringFormatter = factory.createStringFormatter()

    // ============== Utils ==============

    @Provides @Singleton
    fun provideDeviceUtils(
        @ApplicationContext context: Context
    ): DeviceUtils = DeviceUtils(context)

    @Provides @Singleton
    fun provideIMetricsCollector(
        @ApplicationContext context: Context,
        systemDataSource: SystemDataSource,
        dispatcherProvider: DispatcherProvider
    ): IMetricsCollector = MetricsCollector(context, systemDataSource, dispatcherProvider)

    @Provides @Singleton
    fun provideIProcessStatsCollector(
        @ApplicationContext context: Context,
        dispatcherProvider: DispatcherProvider,
        cpuMetricsCollector: ICpuMetricsCollector
    ): IProcessStatsCollector = ProcessStatsCollector(context, dispatcherProvider, cpuMetricsCollector)

    @Provides @Singleton
    fun provideAdaptivePerformanceMonitor(): AdaptivePerformanceMonitor = AdaptivePerformanceMonitor()

    @Provides @Singleton
    fun provideBatteryAwareMonitor(
        @ApplicationContext context: Context
    ): BatteryAwareMonitor = BatteryAwareMonitor(context)

    @Provides @Singleton
    fun provideNetworkStatsDataSource(
        dispatcherProvider: DispatcherProvider
    ): NetworkStatsDataSource = NetworkStatsDataSource(dispatcherProvider)
}
