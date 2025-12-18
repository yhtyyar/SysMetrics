package com.sysmetrics.app.core

import android.app.Application
import com.sysmetrics.app.core.di.AppContainer
import timber.log.Timber

/**
 * Application class for SysMetrics.
 * Initializes logging and dependency injection container.
 */
class SysMetricsApplication : Application() {
    
    lateinit var appContainer: AppContainer
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Timber for logging
        Timber.plant(Timber.DebugTree())
        
        // Initialize DI container
        appContainer = AppContainer(this)
        
        Timber.i("SysMetrics Application initialized")
    }
}
