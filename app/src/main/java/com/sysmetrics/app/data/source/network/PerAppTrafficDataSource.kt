package com.sysmetrics.app.data.source.network

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.TrafficStats
import android.os.Build
import android.os.Process
import com.sysmetrics.app.core.di.DispatcherProvider
import com.sysmetrics.app.data.model.network.PerAppTrafficStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.util.concurrent.ConcurrentHashMap

/**
 * Data source for per-application network traffic statistics.
 * 
 * ## Data Sources (in order of preference):
 * 1. /proc/net/xt_qtaguid/stats - Most accurate, requires elevated permissions
 * 2. TrafficStats API - Works on all devices, but limited to UID-level stats
 * 
 * ## Implementation Notes:
 * - Caches app info (name, icon) to avoid repeated PackageManager queries
 * - Handles apps that are installed/uninstalled during monitoring
 * - Gracefully degrades if xt_qtaguid is not available
 * - Thread-safe with ConcurrentHashMap for caching
 *
 * @param context Application context for PackageManager access
 * @param dispatcherProvider Coroutine dispatcher provider
 */
class PerAppTrafficDataSource(
    private val context: Context,
    private val dispatcherProvider: DispatcherProvider
) {
    companion object {
        private const val TAG = "PERAPP_TRAFFIC_DS"
        private const val PROC_XT_QTAGUID = "/proc/net/xt_qtaguid/stats"
        private const val MIN_TIME_DELTA_MS = 100L
        private const val CACHE_CLEANUP_THRESHOLD = 100
    }

    private val packageManager: PackageManager = context.packageManager

    // Cache for app info to avoid repeated PM queries
    private val appInfoCache = ConcurrentHashMap<Int, CachedAppInfo>()

    // Previous traffic snapshot for delta calculation
    @Volatile
    private var previousSnapshot: Map<Int, UidTrafficSnapshot> = emptyMap()

    @Volatile
    private var previousTimestamp: Long = 0L

    @Volatile
    private var isInitialized: Boolean = false

    // Session start values for cumulative tracking
    private val sessionStartBytes = ConcurrentHashMap<Int, Pair<Long, Long>>()

    /**
     * Cached application info to avoid repeated PackageManager queries.
     */
    private data class CachedAppInfo(
        val packageName: String,
        val appName: String,
        val appIcon: Drawable?,
        val lastAccessed: Long = System.currentTimeMillis()
    )

    /**
     * Traffic snapshot for a single UID.
     */
    private data class UidTrafficSnapshot(
        val uid: Int,
        val rxBytes: Long,
        val txBytes: Long,
        val timestamp: Long
    )

    /**
     * Gets per-app traffic statistics.
     * Returns top N apps sorted by current traffic.
     *
     * @param topN Number of top apps to return (default 5)
     * @return List of [PerAppTrafficStats] sorted by total speed
     */
    suspend fun getPerAppStats(topN: Int = 5): List<PerAppTrafficStats> = withContext(dispatcherProvider.io) {
        try {
            val currentSnapshot = readCurrentSnapshot()
            val currentTime = System.currentTimeMillis()

            if (!isInitialized) {
                initializeBaseline(currentSnapshot, currentTime)
                return@withContext emptyList()
            }

            val timeDeltaMs = currentTime - previousTimestamp
            if (timeDeltaMs < MIN_TIME_DELTA_MS) {
                return@withContext emptyList()
            }

            val timeDeltaSec = timeDeltaMs / 1000f
            val stats = mutableListOf<PerAppTrafficStats>()

            currentSnapshot.forEach { (uid, current) ->
                val previous = previousSnapshot[uid]
                if (previous != null) {
                    val rxDelta = (current.rxBytes - previous.rxBytes).coerceAtLeast(0L)
                    val txDelta = (current.txBytes - previous.txBytes).coerceAtLeast(0L)

                    val ingressBps = (rxDelta / timeDeltaSec).toLong()
                    val egressBps = (txDelta / timeDeltaSec).toLong()

                    // Only include apps with traffic
                    if (ingressBps > 0 || egressBps > 0) {
                        val appInfo = getAppInfo(uid)
                        val sessionStart = sessionStartBytes[uid] ?: Pair(current.rxBytes, current.txBytes)

                        stats.add(
                            PerAppTrafficStats(
                                uid = uid,
                                packageName = appInfo.packageName,
                                appName = appInfo.appName,
                                appIcon = appInfo.appIcon,
                                ingressBytesPerSec = ingressBps,
                                egressBytesPerSec = egressBps,
                                totalIngressBytes = current.rxBytes,
                                totalEgressBytes = current.txBytes,
                                sessionIngressBytes = current.rxBytes - sessionStart.first,
                                sessionEgressBytes = current.txBytes - sessionStart.second,
                                lastActiveTimestamp = currentTime
                            )
                        )
                    }
                }
            }

            previousSnapshot = currentSnapshot
            previousTimestamp = currentTime

            // Clean up cache periodically
            if (appInfoCache.size > CACHE_CLEANUP_THRESHOLD) {
                cleanupCache()
            }

            stats.sortedByDescending { it.totalSpeedBytesPerSec }.take(topN)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error getting per-app stats")
            emptyList()
        }
    }

    /**
     * Observes per-app traffic as a Flow.
     *
     * @param intervalMs Update interval in milliseconds
     * @param topN Number of top apps to include
     * @return Flow of per-app traffic stats lists
     */
    fun observePerAppStats(intervalMs: Long = 1000L, topN: Int = 5): Flow<List<PerAppTrafficStats>> = flow {
        while (true) {
            emit(getPerAppStats(topN))
            kotlinx.coroutines.delay(intervalMs)
        }
    }

    /**
     * Reads current traffic snapshot from available source.
     */
    private fun readCurrentSnapshot(): Map<Int, UidTrafficSnapshot> {
        // Try xt_qtaguid first (more accurate)
        val xtQtaguidStats = tryReadXtQtaguid()
        if (xtQtaguidStats.isNotEmpty()) {
            return xtQtaguidStats
        }

        // Fallback to TrafficStats API
        return readTrafficStatsApi()
    }

    /**
     * Tries to read /proc/net/xt_qtaguid/stats.
     * Returns empty map if not available.
     * 
     * Format:
     * idx iface acct_tag_hex uid_tag_int ... rx_bytes rx_packets tx_bytes tx_packets ...
     */
    private fun tryReadXtQtaguid(): Map<Int, UidTrafficSnapshot> {
        val file = File(PROC_XT_QTAGUID)
        if (!file.exists() || !file.canRead()) {
            return emptyMap()
        }

        val result = mutableMapOf<Int, UidTrafficSnapshot>()
        val timestamp = System.currentTimeMillis()

        try {
            BufferedReader(FileReader(file)).use { reader ->
                var lineNumber = 0
                reader.forEachLine { line ->
                    lineNumber++
                    // Skip header
                    if (lineNumber == 1) return@forEachLine

                    try {
                        val parts = line.split(" ").filter { it.isNotBlank() }
                        if (parts.size >= 8) {
                            val uid = parts[3].toIntOrNull() ?: return@forEachLine
                            val rxBytes = parts[5].toLongOrNull() ?: 0L
                            val txBytes = parts[7].toLongOrNull() ?: 0L

                            // Aggregate by UID (multiple entries per UID possible)
                            val existing = result[uid]
                            if (existing != null) {
                                result[uid] = existing.copy(
                                    rxBytes = existing.rxBytes + rxBytes,
                                    txBytes = existing.txBytes + txBytes
                                )
                            } else {
                                result[uid] = UidTrafficSnapshot(uid, rxBytes, txBytes, timestamp)
                            }
                        }
                    } catch (e: Exception) {
                        // Skip malformed lines
                    }
                }
            }
            Timber.tag(TAG).v("Read ${result.size} UIDs from xt_qtaguid")
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Error reading xt_qtaguid")
        }

        return result
    }

    /**
     * Reads traffic stats using TrafficStats API.
     * Limited to installed apps with assigned UIDs.
     */
    private fun readTrafficStatsApi(): Map<Int, UidTrafficSnapshot> {
        val result = mutableMapOf<Int, UidTrafficSnapshot>()
        val timestamp = System.currentTimeMillis()

        try {
            // Get all installed apps
            val packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

            packages.forEach { appInfo ->
                val uid = appInfo.uid

                // Skip system UIDs if already processed
                if (result.containsKey(uid)) return@forEach

                val rxBytes = TrafficStats.getUidRxBytes(uid)
                val txBytes = TrafficStats.getUidTxBytes(uid)

                // Only include apps with traffic data
                if (rxBytes != TrafficStats.UNSUPPORTED.toLong() && 
                    txBytes != TrafficStats.UNSUPPORTED.toLong() &&
                    (rxBytes > 0 || txBytes > 0)) {
                    result[uid] = UidTrafficSnapshot(uid, rxBytes, txBytes, timestamp)
                }
            }

            // Also track our own app
            val myUid = Process.myUid()
            if (!result.containsKey(myUid)) {
                val rxBytes = TrafficStats.getUidRxBytes(myUid)
                val txBytes = TrafficStats.getUidTxBytes(myUid)
                if (rxBytes != TrafficStats.UNSUPPORTED.toLong()) {
                    result[myUid] = UidTrafficSnapshot(myUid, rxBytes, txBytes, timestamp)
                }
            }

            Timber.tag(TAG).v("Read ${result.size} UIDs from TrafficStats API")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error reading TrafficStats API")
        }

        return result
    }

    /**
     * Gets or creates cached app info for a UID.
     */
    private fun getAppInfo(uid: Int): CachedAppInfo {
        // Check cache first
        appInfoCache[uid]?.let { cached ->
            return cached.copy(lastAccessed = System.currentTimeMillis())
        }

        // Resolve app info
        val appInfo = resolveAppInfo(uid)
        appInfoCache[uid] = appInfo
        return appInfo
    }

    /**
     * Resolves app name and icon for a UID.
     */
    private fun resolveAppInfo(uid: Int): CachedAppInfo {
        try {
            val packages = packageManager.getPackagesForUid(uid)
            if (packages.isNullOrEmpty()) {
                return createSystemAppInfo(uid)
            }

            val packageName = packages[0]
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            val appName = packageManager.getApplicationLabel(appInfo).toString()
            val appIcon = try {
                packageManager.getApplicationIcon(appInfo)
            } catch (e: Exception) {
                null
            }

            return CachedAppInfo(
                packageName = packageName,
                appName = appName,
                appIcon = appIcon
            )
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Error resolving app info for UID $uid")
            return createSystemAppInfo(uid)
        }
    }

    /**
     * Creates app info for system/unknown UIDs.
     */
    private fun createSystemAppInfo(uid: Int): CachedAppInfo {
        val name = when {
            uid == Process.SYSTEM_UID -> "Android System"
            uid == 0 -> "Root"
            uid < Process.FIRST_APPLICATION_UID -> "System (UID $uid)"
            else -> "Unknown (UID $uid)"
        }
        return CachedAppInfo(
            packageName = "android.uid.$uid",
            appName = name,
            appIcon = null
        )
    }

    /**
     * Initializes baseline for delta calculations.
     */
    private fun initializeBaseline(snapshot: Map<Int, UidTrafficSnapshot>, timestamp: Long) {
        previousSnapshot = snapshot
        previousTimestamp = timestamp

        // Initialize session start bytes
        snapshot.forEach { (uid, data) ->
            sessionStartBytes[uid] = Pair(data.rxBytes, data.txBytes)
        }

        isInitialized = true
        Timber.tag(TAG).d("Per-app traffic baseline initialized with ${snapshot.size} UIDs")
    }

    /**
     * Resets baseline and clears caches.
     */
    fun resetBaseline() {
        isInitialized = false
        previousSnapshot = emptyMap()
        previousTimestamp = 0L
        sessionStartBytes.clear()
        Timber.tag(TAG).d("Per-app traffic baseline reset")
    }

    /**
     * Clears app info cache.
     */
    fun clearCache() {
        appInfoCache.clear()
        Timber.tag(TAG).d("App info cache cleared")
    }

    /**
     * Cleans up stale cache entries.
     */
    private fun cleanupCache() {
        val cutoffTime = System.currentTimeMillis() - 300_000 // 5 minutes
        val staleEntries = appInfoCache.entries.filter { it.value.lastAccessed < cutoffTime }
        staleEntries.forEach { appInfoCache.remove(it.key) }
        Timber.tag(TAG).d("Cleaned up ${staleEntries.size} stale cache entries")
    }

    /**
     * Checks if xt_qtaguid is available.
     */
    fun isXtQtaguidAvailable(): Boolean {
        val file = File(PROC_XT_QTAGUID)
        return file.exists() && file.canRead()
    }

    /**
     * Gets traffic for a specific package.
     */
    suspend fun getTrafficForPackage(packageName: String): PerAppTrafficStats? = withContext(dispatcherProvider.io) {
        try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            val uid = appInfo.uid

            val rxBytes = TrafficStats.getUidRxBytes(uid)
            val txBytes = TrafficStats.getUidTxBytes(uid)

            if (rxBytes == TrafficStats.UNSUPPORTED.toLong()) {
                return@withContext null
            }

            val cachedInfo = getAppInfo(uid)

            PerAppTrafficStats(
                uid = uid,
                packageName = packageName,
                appName = cachedInfo.appName,
                appIcon = cachedInfo.appIcon,
                totalIngressBytes = rxBytes,
                totalEgressBytes = txBytes
            )
        } catch (e: PackageManager.NameNotFoundException) {
            Timber.tag(TAG).w("Package not found: $packageName")
            null
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error getting traffic for $packageName")
            null
        }
    }
}
