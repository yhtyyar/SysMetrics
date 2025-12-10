package com.sysmetrics.app.utils

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Process
import timber.log.Timber
import java.io.File

/**
 * Optimized process statistics collector
 * Accurate CPU and RAM monitoring per application
 * Top-3 apps by resource usage with efficient caching
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
     * Shows only user-installed apps (not system apps)
     * @param count Number of top apps to return
     * @param sortBy Sorting criteria: "cpu", "ram", or "combined"
     * @return List of AppStats sorted by specified criteria
     */
    fun getTopApps(count: Int, sortBy: String = "combined"): List<AppStats> {
        try {
            if (count <= 0) return emptyList()

            val runningApps = activityManager.runningAppProcesses ?: emptyList()
            val appStatsList = mutableListOf<AppStats>()

            for (appProcess in runningApps) {
                val packageName = appProcess.processName.split(":")[0]
                
                // Skip current app (SysMetrics)
                if (packageName == context.packageName) {
                    continue
                }

                // Check if it's a user-installed app
                if (!isUserApp(packageName)) {
                    continue
                }

                // Get stats for this process
                val stats = getStatsForPid(appProcess.pid, appProcess.processName)
                
                // Only include apps with measurable resource usage
                if (stats != null && (stats.cpuPercent > 0.01f || stats.ramMb > 10)) {
                    appStatsList.add(stats)
                }
            }

            // Sort by specified criteria
            val sorted = when (sortBy.lowercase()) {
                "cpu" -> appStatsList.sortedByDescending { it.cpuPercent }
                "ram" -> appStatsList.sortedByDescending { it.ramMb }
                else -> appStatsList.sortedByDescending { it.combinedScore }
            }

            return sorted.take(count)

        } catch (e: Exception) {
            Timber.e(e, "Failed to get top apps")
            return emptyList()
        }
    }
    
    /**
     * Get top apps by CPU usage specifically
     */
    fun getTopAppsByCpu(count: Int): List<AppStats> = getTopApps(count, "cpu")
    
    /**
     * Get top apps by RAM usage specifically
     */
    fun getTopAppsByRam(count: Int): List<AppStats> = getTopApps(count, "ram")

    /**
     * Check if package is a user-installed app (not system app)
     * @return true if user app, false if system app
     */
    private fun isUserApp(packageName: String): Boolean {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            
            // User app if:
            // 1. Not a system app (FLAG_SYSTEM)
            // 2. Or is updated system app (FLAG_UPDATED_SYSTEM_APP)
            val isSystemApp = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
            val isUpdatedSystemApp = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
            
            // Return true only for user-installed apps or updated system apps
            !isSystemApp || isUpdatedSystemApp
            
        } catch (e: Exception) {
            // If we can't get app info, assume it's a system process
            false
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
     * Calculate CPU usage for specific PID (optimized)
     * Uses delta measurement with proper timing for accuracy under load
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

            // Get total CPU time from /proc/stat
            val totalCpuTime = getTotalCpuTime()
            if (totalCpuTime == 0L) return 0f

            // Calculate delta with previous measurement
            val previousStat = previousStats[pid]
            val cpuPercent = if (previousStat != null && previousTotalCpuTime > 0) {
                val timeDelta = (totalTime - previousStat.totalTime).coerceAtLeast(0L)
                val totalDelta = (totalCpuTime - previousTotalCpuTime).coerceAtLeast(0L)
                
                if (totalDelta > 0) {
                    // Optimized calculation for multi-core accuracy
                    val numCores = Runtime.getRuntime().availableProcessors()
                    val rawPercent = (timeDelta.toFloat() / totalDelta.toFloat()) * 100f * numCores
                    // Cap at 100% for single process display
                    rawPercent.coerceIn(0f, 100f)
                } else 0f
            } else {
                // First measurement - store baseline
                0f
            }

            // Update cache for next measurement
            previousStats[pid] = ProcessStat(totalTime)

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
     * Initialize baseline for accurate measurement
     */
    fun initializeBaseline() {
        Timber.d("Initializing process stats baseline")
        previousTotalCpuTime = getTotalCpuTime()
    }

    /**
     * Warm up cache with initial readings
     */
    fun warmUpCache() {
        try {
            val runningApps = activityManager.runningAppProcesses ?: return
            runningApps.forEach { process ->
                calculateCpuUsageForPid(process.pid)
            }
            Timber.d("Process stats cache warmed up")
        } catch (e: Exception) {
            Timber.e(e, "Failed to warm up cache")
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
) {
    /**
     * Combined score for sorting (CPU priority + RAM weight)
     */
    val combinedScore: Float
        get() = (cpuPercent * 10f) + (ramMb / 100f)
}
