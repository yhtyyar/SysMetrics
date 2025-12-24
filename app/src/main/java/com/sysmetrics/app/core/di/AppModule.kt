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
import com.sysmetrics.app.domain.repository.IMetricsHistoryRepository
import com.sysmetrics.app.domain.repository.IPreferencesRepository
import com.sysmetrics.app.domain.repository.ISystemMetricsRepository
import com.sysmetrics.app.domain.usecase.ExportMetricsUseCase
import com.sysmetrics.app.domain.usecase.GetSystemMetricsUseCase
import com.sysmetrics.app.domain.usecase.ManageOverlayConfigUseCase
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
}
