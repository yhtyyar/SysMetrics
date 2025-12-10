package com.sysmetrics.app.utils

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Process
import timber.log.Timber
import java.io.File

/**
 * Collects process-level statistics for apps
 * Shows CPU and RAM usage per application
 */
class ProcessStatsCollector(private val context: Context) {

    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val packageManager = context.packageManager
    
    // Cache for process stats
    private val previousStats = mutableMapOf<Int, ProcessStat>()
    private var previousTotalCpuTime = 0L

    /**
     * Get statistics for current app (SysMetrics)
     */
    fun getSelfStats(): AppStats {
        val pid = Process.myPid()
        return getStatsForPid(pid, "com.sysmetrics.app") ?: AppStats(
            packageName = "com.sysmetrics.app",
            appName = "SysMetrics",
            cpuPercent = 0f,
            ramMb = 0L
        )
    }

    /**
     * Get top N apps by resource usage
     * @param count Number of top apps to return
     * @return List of AppStats sorted by CPU + RAM usage
     */
    fun getTopApps(count: Int): List<AppStats> {
        try {
            val runningApps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                activityManager.runningAppProcesses ?: emptyList()
            } else {
                emptyList()
            }

            val appStatsList = mutableListOf<AppStats>()

            for (appProcess in runningApps) {
                // Skip system processes and self
                if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_BACKGROUND 
                    && appProcess.processName.startsWith("system")) {
                    continue
                }

                val stats = getStatsForPid(appProcess.pid, appProcess.processName)
                if (stats != null && stats.cpuPercent > 0.1f) {
                    appStatsList.add(stats)
                }
            }

            // Sort by combined score (CPU + RAM usage)
            return appStatsList
                .sortedByDescending { it.cpuPercent + (it.ramMb / 10f) }
                .take(count)

        } catch (e: Exception) {
            Timber.e(e, "Failed to get top apps")
            return emptyList()
        }
    }

    /**
     * Get stats for specific PID
     */
    private fun getStatsForPid(pid: Int, processName: String): AppStats? {
        try {
            // Get RAM usage
            val memoryInfo = android.app.ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)
            
            val pids = intArrayOf(pid)
            val processMemInfo = activityManager.getProcessMemoryInfo(pids)
            val ramKb = if (processMemInfo.isNotEmpty()) {
                processMemInfo[0].totalPss.toLong()
            } else 0L
            
            val ramMb = ramKb / 1024

            // Get CPU usage
            val cpuPercent = calculateCpuUsageForPid(pid)

            // Get app name
            val appName = try {
                val appInfo = packageManager.getApplicationInfo(processName, 0)
                packageManager.getApplicationLabel(appInfo).toString()
            } catch (e: Exception) {
                processName.split(":")[0]
            }

            return AppStats(
                packageName = processName,
                appName = appName,
                cpuPercent = cpuPercent,
                ramMb = ramMb
            )

        } catch (e: Exception) {
            Timber.e(e, "Failed to get stats for PID $pid")
            return null
        }
    }

    /**
     * Calculate CPU usage for specific PID
     */
    private fun calculateCpuUsageForPid(pid: Int): Float {
        try {
            val statFile = File("/proc/$pid/stat")
            if (!statFile.exists() || !statFile.canRead()) {
                return 0f
            }

            val statContent = statFile.readText()
            val stats = statContent.split(" ")
            
            if (stats.size < 17) return 0f

            // CPU time = utime + stime (user + system time)
            val utime = stats[13].toLongOrNull() ?: 0L
            val stime = stats[14].toLongOrNull() ?: 0L
            val totalTime = utime + stime

            // Get total CPU time
            val totalCpuTime = getTotalCpuTime()

            // Calculate delta
            val previousStat = previousStats[pid]
            val cpuPercent = if (previousStat != null && previousTotalCpuTime > 0) {
                val timeDelta = totalTime - previousStat.totalTime
                val totalDelta = totalCpuTime - previousTotalCpuTime
                
                if (totalDelta > 0) {
                    ((timeDelta.toFloat() / totalDelta.toFloat()) * 100f).coerceIn(0f, 100f)
                } else 0f
            } else 0f

            // Update cache
            previousStats[pid] = ProcessStat(totalTime)
            previousTotalCpuTime = totalCpuTime

            return cpuPercent

        } catch (e: Exception) {
            Timber.e(e, "Failed to calculate CPU for PID $pid")
            return 0f
        }
    }

    /**
     * Get total CPU time from /proc/stat
     */
    private fun getTotalCpuTime(): Long {
        try {
            val statFile = File("/proc/stat")
            if (!statFile.exists() || !statFile.canRead()) {
                return 0L
            }

            val line = statFile.bufferedReader().use { it.readLine() }
            val parts = line.split("\\s+".toRegex()).filter { it.isNotEmpty() }
            
            if (parts.size < 8) return 0L

            // Sum all CPU times
            var total = 0L
            for (i in 1..7) {
                total += parts[i].toLongOrNull() ?: 0L
            }
            return total

        } catch (e: Exception) {
            Timber.e(e, "Failed to get total CPU time")
            return 0L
        }
    }

    /**
     * Clear cached stats
     */
    fun clearCache() {
        previousStats.clear()
        previousTotalCpuTime = 0L
    }

    /**
     * Data class for process stats
     */
    private data class ProcessStat(
        val totalTime: Long
    )
}

/**
 * App statistics data class
 */
data class AppStats(
    val packageName: String,
    val appName: String,
    val cpuPercent: Float,
    val ramMb: Long
)
