package com.sysmetrics.app.core.di

import android.content.Context
import com.sysmetrics.app.data.repository.NetworkStatsRepository
import com.sysmetrics.app.data.source.network.NetworkStatsDataSource
import com.sysmetrics.app.data.source.network.NetworkTypeDetector
import com.sysmetrics.app.data.source.network.PerAppTrafficDataSource
import com.sysmetrics.app.domain.repository.INetworkStatsRepository
import com.sysmetrics.app.domain.usecase.network.GetNetworkStatsUseCase
import com.sysmetrics.app.domain.usecase.network.MonitorNetworkTrafficUseCase
import com.sysmetrics.app.native_bridge.NativeNetworkMetrics
import com.sysmetrics.app.ui.network.NetworkStatsViewModel
import com.sysmetrics.app.ui.network.NetworkStatsViewModelFactory

/**
 * Manual dependency injection module for network traffic monitoring.
 * Provides factory methods for creating all network-related dependencies.
 *
 * ## Usage:
 * ```kotlin
 * val networkModule = NetworkModule(applicationContext, dispatcherProvider)
 * val viewModelFactory = networkModule.provideViewModelFactory()
 * ```
 *
 * Note: This is a manual DI solution. Can be migrated to Hilt when enabled.
 */
class NetworkModule(
    private val context: Context,
    private val dispatcherProvider: DispatcherProvider
) {
    // Lazy initialization for singleton-like behavior within module scope

    private val nativeNetworkMetrics: NativeNetworkMetrics by lazy {
        NativeNetworkMetrics()
    }

    private val networkStatsDataSource: NetworkStatsDataSource by lazy {
        NetworkStatsDataSource(dispatcherProvider)
    }

    private val networkTypeDetector: NetworkTypeDetector by lazy {
        NetworkTypeDetector(context)
    }

    private val perAppTrafficDataSource: PerAppTrafficDataSource by lazy {
        PerAppTrafficDataSource(context, dispatcherProvider)
    }

    private val networkStatsRepository: INetworkStatsRepository by lazy {
        NetworkStatsRepository(
            context = context,
            networkStatsDataSource = networkStatsDataSource,
            networkTypeDetector = networkTypeDetector,
            perAppTrafficDataSource = perAppTrafficDataSource,
            nativeNetworkMetrics = nativeNetworkMetrics
        )
    }

    private val getNetworkStatsUseCase: GetNetworkStatsUseCase by lazy {
        GetNetworkStatsUseCase(networkStatsRepository)
    }

    private val monitorNetworkTrafficUseCase: MonitorNetworkTrafficUseCase by lazy {
        MonitorNetworkTrafficUseCase(networkStatsRepository)
    }

    /**
     * Provides NetworkStatsDataSource instance.
     */
    fun provideNetworkStatsDataSource(): NetworkStatsDataSource = networkStatsDataSource

    /**
     * Provides NetworkTypeDetector instance.
     */
    fun provideNetworkTypeDetector(): NetworkTypeDetector = networkTypeDetector

    /**
     * Provides PerAppTrafficDataSource instance.
     */
    fun providePerAppTrafficDataSource(): PerAppTrafficDataSource = perAppTrafficDataSource

    /**
     * Provides NativeNetworkMetrics JNI bridge instance.
     */
    fun provideNativeNetworkMetrics(): NativeNetworkMetrics = nativeNetworkMetrics

    /**
     * Provides NetworkStatsRepository instance.
     */
    fun provideNetworkStatsRepository(): INetworkStatsRepository = networkStatsRepository

    /**
     * Provides GetNetworkStatsUseCase instance.
     */
    fun provideGetNetworkStatsUseCase(): GetNetworkStatsUseCase = getNetworkStatsUseCase

    /**
     * Provides MonitorNetworkTrafficUseCase instance.
     */
    fun provideMonitorNetworkTrafficUseCase(): MonitorNetworkTrafficUseCase = monitorNetworkTrafficUseCase

    /**
     * Provides ViewModelFactory for NetworkStatsViewModel.
     */
    fun provideViewModelFactory(): NetworkStatsViewModelFactory {
        return NetworkStatsViewModelFactory(
            getNetworkStatsUseCase = getNetworkStatsUseCase,
            monitorNetworkTrafficUseCase = monitorNetworkTrafficUseCase
        )
    }

    /**
     * Resets all data sources and caches.
     * Call when starting a fresh monitoring session.
     */
    fun resetAll() {
        networkStatsDataSource.resetBaseline()
        perAppTrafficDataSource.resetBaseline()
        nativeNetworkMetrics.resetBaseline()
    }

    /**
     * Clears all caches.
     */
    fun clearCaches() {
        perAppTrafficDataSource.clearCache()
    }

    /**
     * Checks if native implementation is available.
     */
    fun isNativeAvailable(): Boolean = nativeNetworkMetrics.isNativeAvailable()

    /**
     * Checks if network monitoring is available.
     */
    fun isMonitoringAvailable(): Boolean {
        return nativeNetworkMetrics.isNativeAvailable() || networkStatsDataSource.isAvailable()
    }

    companion object {
        @Volatile
        private var instance: NetworkModule? = null

        /**
         * Gets or creates singleton instance.
         * Thread-safe with double-checked locking.
         */
        fun getInstance(context: Context, dispatcherProvider: DispatcherProvider): NetworkModule {
            return instance ?: synchronized(this) {
                instance ?: NetworkModule(context.applicationContext, dispatcherProvider).also {
                    instance = it
                }
            }
        }

        /**
         * Clears singleton instance.
         * Useful for testing or app restart.
         */
        fun clearInstance() {
            synchronized(this) {
                instance?.clearCaches()
                instance = null
            }
        }
    }
}
