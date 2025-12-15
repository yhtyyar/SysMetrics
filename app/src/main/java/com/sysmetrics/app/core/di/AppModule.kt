package com.sysmetrics.app.core.di

import android.content.Context
import com.sysmetrics.app.data.source.SystemDataSource
import com.sysmetrics.app.utils.MetricsCollector
import com.sysmetrics.app.utils.ProcessStatsCollector
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for application-level dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * Provides MetricsCollector singleton
     */
    @Provides
    @Singleton
    fun provideMetricsCollector(
        @ApplicationContext context: Context,
        systemDataSource: SystemDataSource,
        dispatcherProvider: DispatcherProvider
    ): MetricsCollector {
        return MetricsCollector(context, systemDataSource, dispatcherProvider)
    }

    /**
     * Provides ProcessStatsCollector singleton
     */
    @Provides
    @Singleton
    fun provideProcessStatsCollector(
        @ApplicationContext context: Context,
        dispatcherProvider: DispatcherProvider
    ): ProcessStatsCollector {
        return ProcessStatsCollector(context, dispatcherProvider)
    }
}
