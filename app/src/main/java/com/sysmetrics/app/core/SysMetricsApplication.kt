package com.sysmetrics.app.core

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.sysmetrics.app.core.di.AppContainer
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

/**
 * Application class for SysMetrics.
 * Initializes Hilt DI, logging, and WorkManager.
 * Maintains AppContainer for backward compatibility during migration.
 */
@HiltAndroidApp
class SysMetricsApplication : Application(), Configuration.Provider {
    
    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    
    // Legacy AppContainer for backward compatibility
    lateinit var appContainer: AppContainer
        private set
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Timber for logging
        if (timber.log.Timber.treeCount == 0) {
            Timber.plant(Timber.DebugTree())
        }
        
        // Initialize legacy AppContainer for backward compatibility
        appContainer = AppContainer(this)
        
        Timber.i("SysMetrics Application initialized with Hilt")
    }
    
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
}
