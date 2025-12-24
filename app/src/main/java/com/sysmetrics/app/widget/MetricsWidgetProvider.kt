package com.sysmetrics.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.sysmetrics.app.R
import com.sysmetrics.app.ui.MainActivityOverlay
import timber.log.Timber
import java.io.File

/**
 * Widget provider for displaying system metrics on the home screen.
 * Shows CPU and RAM usage with color indicators.
 */
class MetricsWidgetProvider : AppWidgetProvider() {

    companion object {
        private const val TAG = "MetricsWidget"
        const val ACTION_REFRESH = "com.sysmetrics.app.widget.ACTION_REFRESH"
        
        // Cache for CPU delta calculation
        private var lastCpuTotal = 0L
        private var lastCpuIdle = 0L
        
        /**
         * Update all widgets.
         */
        fun updateAllWidgets(context: Context) {
            val intent = Intent(context, MetricsWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val widgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, MetricsWidgetProvider::class.java)
            )
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)
            context.sendBroadcast(intent)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Timber.tag(TAG).d("Updating ${appWidgetIds.size} widgets")
        
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        if (intent.action == ACTION_REFRESH) {
            Timber.tag(TAG).d("Manual refresh requested")
            updateAllWidgets(context)
        }
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        try {
            val cpuUsage = getCpuUsage()
            val ramUsage = getRamUsage(context)
            
            val views = RemoteViews(context.packageName, R.layout.widget_metrics).apply {
                // CPU
                setTextViewText(R.id.tvWidgetCpuValue, String.format("%.0f%%", cpuUsage))
                setInt(R.id.tvWidgetCpuValue, "setTextColor", getColorForValue(context, cpuUsage))
                setProgressBar(R.id.pbWidgetCpu, 100, cpuUsage.toInt(), false)
                
                // RAM
                setTextViewText(R.id.tvWidgetRamValue, String.format("%.0f%%", ramUsage))
                setInt(R.id.tvWidgetRamValue, "setTextColor", getColorForValue(context, ramUsage))
                setProgressBar(R.id.pbWidgetRam, 100, ramUsage.toInt(), false)
                
                // Click to open app
                val openAppIntent = Intent(context, MainActivityOverlay::class.java)
                val openAppPendingIntent = PendingIntent.getActivity(
                    context, 0, openAppIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                setOnClickPendingIntent(R.id.layoutWidgetRoot, openAppPendingIntent)
                
                // Refresh button
                val refreshIntent = Intent(context, MetricsWidgetProvider::class.java).apply {
                    action = ACTION_REFRESH
                }
                val refreshPendingIntent = PendingIntent.getBroadcast(
                    context, 0, refreshIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                setOnClickPendingIntent(R.id.btnWidgetRefresh, refreshPendingIntent)
            }
            
            appWidgetManager.updateAppWidget(appWidgetId, views)
            Timber.tag(TAG).v("Widget $appWidgetId updated: CPU=$cpuUsage%, RAM=$ramUsage%")
            
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to update widget $appWidgetId")
        }
    }

    private fun getCpuUsage(): Float {
        return try {
            val statFile = File("/proc/stat")
            if (!statFile.exists()) return 0f
            
            val line = statFile.readLines().firstOrNull { it.startsWith("cpu ") } ?: return 0f
            val parts = line.split("\\s+".toRegex()).filter { it.isNotEmpty() }
            
            if (parts.size < 5) return 0f
            
            val user = parts[1].toLongOrNull() ?: 0L
            val nice = parts[2].toLongOrNull() ?: 0L
            val system = parts[3].toLongOrNull() ?: 0L
            val idle = parts[4].toLongOrNull() ?: 0L
            val iowait = parts.getOrNull(5)?.toLongOrNull() ?: 0L
            val irq = parts.getOrNull(6)?.toLongOrNull() ?: 0L
            val softirq = parts.getOrNull(7)?.toLongOrNull() ?: 0L
            
            val currentIdle = idle + iowait
            val currentTotal = user + nice + system + idle + iowait + irq + softirq
            
            // Calculate delta from last reading
            val deltaTotal = currentTotal - lastCpuTotal
            val deltaIdle = currentIdle - lastCpuIdle
            
            // Update cache
            lastCpuTotal = currentTotal
            lastCpuIdle = currentIdle
            
            // First reading - return 0
            if (deltaTotal == 0L) return 0f
            
            val cpuUsage = ((deltaTotal - deltaIdle).toFloat() / deltaTotal) * 100f
            cpuUsage.coerceIn(0f, 100f)
        } catch (e: Exception) {
            0f
        }
    }

    private fun getRamUsage(context: Context): Float {
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) 
                as android.app.ActivityManager
            val memInfo = android.app.ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memInfo)
            
            val usedMem = memInfo.totalMem - memInfo.availMem
            ((usedMem.toFloat() / memInfo.totalMem) * 100f).coerceIn(0f, 100f)
        } catch (e: Exception) {
            0f
        }
    }

    private fun getColorForValue(context: Context, percent: Float): Int {
        return when {
            percent < 50 -> context.getColor(R.color.metric_success)
            percent < 80 -> context.getColor(R.color.metric_warning)
            else -> context.getColor(R.color.metric_error)
        }
    }
}
