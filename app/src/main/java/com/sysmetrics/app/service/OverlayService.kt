package com.sysmetrics.app.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.sysmetrics.app.R
import com.sysmetrics.app.SysMetricsApp
import com.sysmetrics.app.data.model.OverlayConfig
import com.sysmetrics.app.data.model.OverlayPosition
import com.sysmetrics.app.domain.usecase.GetSystemMetricsUseCase
import com.sysmetrics.app.domain.usecase.ManageOverlayConfigUseCase
import com.sysmetrics.app.ui.MainActivity
import com.sysmetrics.app.ui.overlay.OverlayView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Foreground service that manages the floating overlay window.
 * Collects system metrics and updates the overlay view in real-time.
 */
@AndroidEntryPoint
class OverlayService : Service() {

    @Inject
    lateinit var getSystemMetricsUseCase: GetSystemMetricsUseCase

    @Inject
    lateinit var manageOverlayConfigUseCase: ManageOverlayConfigUseCase

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var windowManager: WindowManager? = null
    private var overlayView: OverlayView? = null
    private var metricsJob: Job? = null
    private var configJob: Job? = null

    private var currentConfig: OverlayConfig = OverlayConfig.DEFAULT

    override fun onCreate() {
        super.onCreate()
        Timber.d("OverlayService created")
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("OverlayService started")

        startForeground(SysMetricsApp.NOTIFICATION_ID, createNotification())

        if (overlayView == null) {
            serviceScope.launch {
                currentConfig = manageOverlayConfigUseCase.observeConfig().first()
                createOverlayView()
                startMetricsCollection()
                observeConfigChanges()
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        Timber.d("OverlayService destroyed")
        removeOverlayView()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createOverlayView() {
        try {
            overlayView = OverlayView(this).apply {
                updateConfig(currentConfig)
            }

            val params = createLayoutParams()
            windowManager?.addView(overlayView, params)
            Timber.d("Overlay view added")
        } catch (e: Exception) {
            Timber.e(e, "Failed to create overlay view")
        }
    }

    private fun createLayoutParams(): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        }

        return WindowManager.LayoutParams(
            OVERLAY_WIDTH,
            OVERLAY_HEIGHT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = getGravity(currentConfig.position)
            x = currentConfig.positionX
            y = currentConfig.positionY
        }
    }

    private fun getGravity(position: OverlayPosition): Int = when (position) {
        OverlayPosition.TOP_LEFT -> Gravity.TOP or Gravity.START
        OverlayPosition.TOP_RIGHT -> Gravity.TOP or Gravity.END
        OverlayPosition.BOTTOM_LEFT -> Gravity.BOTTOM or Gravity.START
        OverlayPosition.BOTTOM_RIGHT -> Gravity.BOTTOM or Gravity.END
    }

    private fun startMetricsCollection() {
        metricsJob?.cancel()
        metricsJob = serviceScope.launch {
            getSystemMetricsUseCase(currentConfig.updateIntervalMs).collectLatest { metrics ->
                overlayView?.updateMetrics(metrics)
            }
        }
    }

    private fun observeConfigChanges() {
        configJob?.cancel()
        configJob = serviceScope.launch {
            manageOverlayConfigUseCase.observeConfig().collect { config ->
                if (config != currentConfig) {
                    val needsRepositioning = config.position != currentConfig.position ||
                            config.positionX != currentConfig.positionX ||
                            config.positionY != currentConfig.positionY

                    val needsIntervalChange = config.updateIntervalMs != currentConfig.updateIntervalMs

                    currentConfig = config
                    overlayView?.updateConfig(config)

                    if (needsRepositioning) {
                        updateOverlayPosition()
                    }

                    if (needsIntervalChange) {
                        startMetricsCollection()
                    }
                }
            }
        }
    }

    private fun updateOverlayPosition() {
        try {
            overlayView?.let { view ->
                val params = view.layoutParams as? WindowManager.LayoutParams ?: return
                params.gravity = getGravity(currentConfig.position)
                params.x = currentConfig.positionX
                params.y = currentConfig.positionY
                windowManager?.updateViewLayout(view, params)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to update overlay position")
        }
    }

    private fun removeOverlayView() {
        metricsJob?.cancel()
        configJob?.cancel()

        try {
            overlayView?.let { view ->
                windowManager?.removeView(view)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to remove overlay view")
        }
        overlayView = null
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, SysMetricsApp.NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(R.drawable.ic_monitor)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        private const val OVERLAY_WIDTH = 220
        private const val OVERLAY_HEIGHT = 280
    }
}
