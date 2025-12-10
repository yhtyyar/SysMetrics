package com.sysmetrics.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import timber.log.Timber
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for SysMetrics.
 * Initializes Hilt dependency injection and Timber logging.
 */
@HiltAndroidApp
class SysMetricsApp : Application() {

    override fun onCreate() {
        super.onCreate()
        initTimber()
        createNotificationChannel()
        Timber.d("SysMetrics application initialized")
    }

    private fun initTimber() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "System Monitor",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "SysMetrics overlay service notification"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "sysmetrics_service"
        const val NOTIFICATION_ID = 1001
    }
}
