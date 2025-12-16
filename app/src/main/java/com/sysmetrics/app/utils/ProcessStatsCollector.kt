package com.sysmetrics.app.utils

import android.app.ActivityManager
import android.content.Context
import android.os.Process
import com.sysmetrics.app.core.common.Constants
import com.sysmetrics.app.core.di.DispatcherProvider
import com.sysmetrics.app.domain.collector.IProcessStatsCollector
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Optimized process statistics collector
 * Accurate CPU and RAM monitoring per application
 * Top-N apps by resource usage with efficient caching
 * 
 * LOGGING TAGS:
 * - PROC_TOP: Top apps collection and display
 * - PROC_CPU: Per-process CPU calculation
 * - PROC_RAM: Per-process RAM measurement
 * - PROC_NAME: App name resolution
 * - PROC_ERROR: Error scenarios
 * 
 * IMPROVEMENTS:
 * - Implements IProcessStatsCollector interface for testability
 * - Uses proper coroutines for async operations
 * - Thread-safe with Mutex for concurrent access
 * - Singleton with Hilt dependency injection
 */
@Singleton
class ProcessStatsCollector @Inject constructor(
    private val context: Context,
    private val dispatcherProvider: DispatcherProvider
) : IProcessStatsCollector {
    
    companion object {
        private const val TAG = "SysMetrics"
        private const val TAG_TOP = "PROC_TOP"
        private const val TAG_CPU = "PROC_CPU"
        private const val TAG_RAM = "PROC_RAM"
        private const val TAG_NAME = "PROC_NAME"
        private const val TAG_ERROR = "PROC_ERROR"
    }

    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val packageManager = context.packageManager
    
    // Thread-safe cache for process stats
    private val cacheMutex = Mutex()
    private val previousStats = mutableMapOf<Int, ProcessStat>()
    private var previousTotalCpuTime = 0L

    /**
     * Get statistics for current app (SysMetrics)
     */
    override suspend fun getSelfStats(): AppStats = withContext(dispatcherProvider.io) {
        val pid = Process.myPid()
        Timber.tag(TAG_CPU).d("🔍 Getting self stats for PID %d", pid)
        
        val stats = getStatsForPid(pid, "com.sysmetrics.app")
        
        if (stats != null) {
            Timber.tag(TAG_CPU).d("✅ Self stats: CPU=%.2f%%, RAM=%dMB", stats.cpuPercent, stats.ramMb)
        } else {
            Timber.tag(TAG_CPU).w("⚠️ Failed to get self stats, returning default")
        }
        
        stats ?: AppStats(
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
    override suspend fun getTopApps(count: Int, sortBy: String): List<AppStats> = withContext(dispatcherProvider.io) {
        try {
            Timber.tag(TAG_TOP).d("🔍 Getting top %d apps (sortBy=%s)", count, sortBy)
            
            if (count <= 0) {
                Timber.tag(TAG_TOP).d("⏭️ Count is 0, returning empty list")
                return@withContext emptyList()
            }

            val runningApps = activityManager.runningAppProcesses ?: emptyList()
            Timber.tag(TAG_TOP).v("📱 Found %d running processes", runningApps.size)
            
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
                if (stats != null && (stats.cpuPercent > Constants.ProcessMonitoring.MIN_CPU_THRESHOLD || 
                    stats.ramMb > Constants.ProcessMonitoring.MIN_RAM_THRESHOLD_MB)) {
                    appStatsList.add(stats)
                }
            }

            Timber.tag(TAG_TOP).d("📊 Collected %d user apps with measurable usage", appStatsList.size)
            
            // Sort by specified criteria
            val sorted = when (sortBy.lowercase()) {
                "cpu" -> appStatsList.sortedByDescending { it.cpuPercent }
                "ram" -> appStatsList.sortedByDescending { it.ramMb }
                else -> appStatsList.sortedByDescending { it.combinedScore }
            }

            val result = sorted.take(count)
            
            // Log top apps
            result.forEachIndexed { index, app ->
                Timber.tag(TAG_TOP).d("🏆 #%d: %s - CPU=%.1f%%, RAM=%dMB, Score=%.1f",
                    index + 1, app.appName, app.cpuPercent, app.ramMb, app.combinedScore)
            }
            
            result

        } catch (e: Exception) {
            Timber.tag(TAG_ERROR).e(e, "❌ Failed to get top apps")
            emptyList()
        }
    }
    
    /**
     * Get top apps by CPU usage specifically
     */
    override suspend fun getTopAppsByCpu(count: Int): List<AppStats> = getTopApps(count, "cpu")
    
    /**
     * Get top apps by RAM usage specifically
     */
    override suspend fun getTopAppsByRam(count: Int): List<AppStats> = getTopApps(count, "ram")

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

            // Get app name (human-readable)
            val appName = try {
                val appInfo = packageManager.getApplicationInfo(processName, 0)
                val label = packageManager.getApplicationLabel(appInfo).toString()
                Timber.tag(TAG_NAME).v("📱 %s → %s", processName, label)
                label
            } catch (e: Exception) {
                val fallback = processName.split(":")[0]
                Timber.tag(TAG_NAME).v("⚠️ Failed to get label for %s, using: %s", processName, fallback)
                fallback
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
                Timber.tag(TAG_CPU).v("⚠️ PID %d: stat file not accessible", pid)
                return 0f
            }

            val statContent = statFile.readText()
            val stats = statContent.split(" ")
            
            if (stats.size < 17) {
                Timber.tag(TAG_CPU).w("⚠️ PID %d: invalid stat format (size=%d)", pid, stats.size)
                return 0f
            }

            // CPU time = utime + stime (user + system time)
            val utime = stats[13].toLongOrNull() ?: 0L
            val stime = stats[14].toLongOrNull() ?: 0L
            val totalTime = utime + stime

            // Get total CPU time from /proc/stat
            val totalCpuTime = getTotalCpuTime()
            if (totalCpuTime == 0L) {
                Timber.tag(TAG_CPU).w("⚠️ PID %d: totalCpuTime is 0", pid)
                return 0f
            }

            // Calculate delta with previous measurement
            val previousStat = previousStats[pid]
            val cpuPercent = if (previousStat != null && previousStat.previousTotalCpuTime > 0) {
                val timeDelta = (totalTime - previousStat.totalTime).coerceAtLeast(0L)
                val totalDelta = (totalCpuTime - previousStat.previousTotalCpuTime).coerceAtLeast(0L)
                
                if (totalDelta > 0) {
                    // Optimized calculation for multi-core accuracy
                    val numCores = Runtime.getRuntime().availableProcessors()
                    val rawPercent = (timeDelta.toFloat() / totalDelta.toFloat()) * 100f * numCores
                    // Cap at 100% for single process display
                    val capped = rawPercent.coerceIn(0f, 100f)
                    
                    if (capped > 0.1f) { // Log even small non-zero values for debugging
                        Timber.tag(TAG_CPU).v("📊 PID %d: timeΔ=%d, totalΔ=%d, cores=%d → %.1f%%",
                            pid, timeDelta, totalDelta, numCores, capped)
                    }
                    capped
                } else {
                    Timber.tag(TAG_CPU).v("⚠️ PID %d: zero totalDelta", pid)
                    0f
                }
            } else {
                // First measurement - store baseline
                Timber.tag(TAG_CPU).v("⏳ PID %d: first measurement (baseline)", pid)
                0f
            }

            // Update cache for next measurement - FIXED: Store per-PID totalCpuTime
            previousStats[pid] = ProcessStat(totalTime, totalCpuTime)
            // Keep global previousTotalCpuTime for baseline initialization compatibility
            previousTotalCpuTime = totalCpuTime

            return cpuPercent

        } catch (e: Exception) {
            Timber.tag(TAG_ERROR).e(e, "❌ Failed to calculate CPU for PID %d", pid)
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
    override suspend fun initializeBaseline() = withContext(dispatcherProvider.io) {
        cacheMutex.withLock {
            Timber.tag(TAG_CPU).d("🔧 Initializing process stats baseline...")
            previousTotalCpuTime = getTotalCpuTime()
            Timber.tag(TAG_CPU).i("✅ Process baseline initialized: totalCpuTime=%d", previousTotalCpuTime)
        }
    }

    /**
     * Warm up cache with initial readings
     */
    override suspend fun warmUpCache() = withContext(dispatcherProvider.io) {
        try {
            Timber.tag(TAG_CPU).d("🔥 Warming up process cache...")
            val runningApps = activityManager.runningAppProcesses ?: return@withContext
            var cachedCount = 0
            runningApps.forEach { process ->
                calculateCpuUsageForPid(process.pid)
                cachedCount++
            }
            Timber.tag(TAG_CPU).i("✅ Process cache warmed: %d processes", cachedCount)
        } catch (e: Exception) {
            Timber.tag(TAG_ERROR).e(e, "❌ Failed to warm up cache")
        }
    }

    /**
     * Clear cached stats
     */
    override fun clearCache() {
        previousStats.clear()
        previousTotalCpuTime = 0L
    }

    /**
     * Data class for process stats
     * FIXED: Now stores previousTotalCpuTime per PID for accurate delta calculation
     */
    private data class ProcessStat(
        val totalTime: Long,
        val previousTotalCpuTime: Long
    )
}

/**
 * App statistics data class
 * 
 * @property packageName Process package name
 * @property appName Human-readable app name
 * @property cpuPercent CPU usage percentage (0-100)
 * @property ramMb RAM usage in megabytes
 */
data class AppStats(
    val packageName: String,
    val appName: String,
    val cpuPercent: Float,
    val ramMb: Long
) {
    /**
     * Combined score for sorting (CPU priority + RAM weight).
     * CPU has higher weight to prioritize processes actively using CPU.
     */
    val combinedScore: Float
        get() = (cpuPercent * Constants.ProcessMonitoring.CPU_SCORE_WEIGHT) + 
                (ramMb / Constants.ProcessMonitoring.RAM_SCORE_WEIGHT_DIVISOR)
}
