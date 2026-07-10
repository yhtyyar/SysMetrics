package com.sysmetrics.app.core

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.sysmetrics.app.BuildConfig
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

/**
 * Application class for SysMetrics.
 * Initializes Hilt DI and WorkManager. All dependencies flow through Hilt —
 * the legacy manual AppContainer has been fully removed.
 */
@HiltAndroidApp
class SysMetricsApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()

        // Initialize Timber for logging (debug builds only — never log metrics to logcat in production)
        if (BuildConfig.DEBUG && timber.log.Timber.treeCount == 0) {
            Timber.plant(Timber.DebugTree())
        }

        Timber.i("SysMetrics Application initialized with Hilt")
    }
    
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
}
