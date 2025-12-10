package com.sysmetrics.app.di

import com.sysmetrics.app.domain.collector.IMetricsCollector
import com.sysmetrics.app.domain.collector.IProcessStatsCollector
import com.sysmetrics.app.utils.MetricsCollector
import com.sysmetrics.app.utils.ProcessStatsCollector
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for binding collector interfaces to their implementations.
 * Using @Binds is more efficient than @Provides for simple interface-to-implementation bindings.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class CollectorModule {

    @Binds
    @Singleton
    abstract fun bindMetricsCollector(
        metricsCollector: MetricsCollector
    ): IMetricsCollector

    @Binds
    @Singleton
    abstract fun bindProcessStatsCollector(
        processStatsCollector: ProcessStatsCollector
    ): IProcessStatsCollector
}
