package com.sysmetrics.app.data.source.network

import com.sysmetrics.app.core.di.DispatcherProvider
import com.sysmetrics.app.data.model.network.InterfaceStats
import com.sysmetrics.app.data.model.network.NetworkSnapshot
import com.sysmetrics.app.data.model.network.NetworkTrafficStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.BufferedReader
import java.io.File
import java.io.FileReader

/**
 * Data source for network traffic statistics.
 * Parses /proc/net/dev for accurate system-wide traffic monitoring.
 * 
 * ## Implementation Notes:
 * - Uses snapshot pattern: calculates delta between two /proc/net/dev readings
 * - Ignores loopback interface (lo) 
 * - Handles interfaces that appear/disappear dynamically
 * - Thread-safe with proper synchronization
 * 
 * ## /proc/net/dev Format:
 * ```
 * Inter-|   Receive                                    |  Transmit
 *  face |bytes packets errs drop fifo frame compressed |bytes packets errs drop fifo colls carrier
 *    lo: 1234567   12345    0    0    0     0          0  1234567   12345    0    0    0     0       0
 *  eth0: 98765432  654321    0    0    0     0          0  87654321  123456    0    0    0     0       0
 * ```
 *
 * @param dispatcherProvider Coroutine dispatcher provider for IO operations
 */
class NetworkStatsDataSource(
    private val dispatcherProvider: DispatcherProvider
) {
    companion object {
        private const val TAG = "NET_STATS_DS"
        private const val PROC_NET_DEV = "/proc/net/dev"
        private const val MIN_TIME_DELTA_MS = 50L
        private const val BYTES_TO_MBPS = 8f / (1024f * 1024f)
    }

    @Volatile
    private var previousSnapshot: NetworkSnapshot = NetworkSnapshot.EMPTY

    @Volatile
    private var peakIngressMbps: Float = 0f

    @Volatile
    private var peakEgressMbps: Float = 0f

    @Volatile
    private var peakIngressTimestamp: Long = 0L

    @Volatile
    private var peakEgressTimestamp: Long = 0L

    @Volatile
    private var sessionStartRxBytes: Long = 0L

    @Volatile
    private var sessionStartTxBytes: Long = 0L

    @Volatile
    private var isInitialized: Boolean = false

    /**
     * Reads current network traffic statistics.
     * Calculates speed based on delta from previous snapshot.
     *
     * @return [NetworkTrafficStats] with current speeds and totals
     */
    suspend fun readNetworkStats(): NetworkTrafficStats = withContext(dispatcherProvider.io) {
        try {
            val currentSnapshot = readProcNetDev()
            val currentTime = currentSnapshot.timestamp

            if (!isInitialized) {
                initializeBaseline(currentSnapshot)
                return@withContext createInitialStats(currentSnapshot)
            }

            val timeDeltaMs = currentTime - previousSnapshot.timestamp
            if (timeDeltaMs < MIN_TIME_DELTA_MS) {
                return@withContext createStats(0L, 0L, currentSnapshot)
            }

            val timeDeltaSec = timeDeltaMs / 1000f
            val rxDelta = (currentSnapshot.totalRxBytes - previousSnapshot.totalRxBytes).coerceAtLeast(0L)
            val txDelta = (currentSnapshot.totalTxBytes - previousSnapshot.totalTxBytes).coerceAtLeast(0L)

            val ingressBytesPerSec = (rxDelta / timeDeltaSec).toLong()
            val egressBytesPerSec = (txDelta / timeDeltaSec).toLong()

            val ingressMbps = ingressBytesPerSec * BYTES_TO_MBPS
            val egressMbps = egressBytesPerSec * BYTES_TO_MBPS

            updatePeakValues(ingressMbps, egressMbps, currentTime)
            previousSnapshot = currentSnapshot

            createStats(ingressBytesPerSec, egressBytesPerSec, currentSnapshot)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error reading network stats")
            NetworkTrafficStats.EMPTY
        }
    }

    /**
     * Provides a Flow of network statistics at the specified interval.
     *
     * @param intervalMs Update interval in milliseconds
     * @return Flow emitting [NetworkTrafficStats]
     */
    fun observeNetworkStats(intervalMs: Long = 1000L): Flow<NetworkTrafficStats> = flow {
        while (true) {
            emit(readNetworkStats())
            kotlinx.coroutines.delay(intervalMs)
        }
    }

    /**
     * Parses /proc/net/dev and returns aggregated snapshot.
     */
    private fun readProcNetDev(): NetworkSnapshot {
        val file = File(PROC_NET_DEV)
        if (!file.exists() || !file.canRead()) {
            Timber.tag(TAG).w("/proc/net/dev not accessible")
            return NetworkSnapshot.EMPTY
        }

        val interfaces = mutableMapOf<String, InterfaceStats>()
        var totalRxBytes = 0L
        var totalTxBytes = 0L
        val timestamp = System.currentTimeMillis()

        try {
            BufferedReader(FileReader(file)).use { reader ->
                var lineNumber = 0
                reader.forEachLine { line ->
                    lineNumber++
                    // Skip header lines (first 2 lines)
                    if (lineNumber <= 2) return@forEachLine

                    val stats = parseInterfaceLine(line, timestamp)
                    if (stats != null && !stats.isLoopback) {
                        interfaces[stats.interfaceName] = stats
                        totalRxBytes += stats.rxBytes
                        totalTxBytes += stats.txBytes
                    }
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error parsing /proc/net/dev")
        }

        return NetworkSnapshot(
            interfaces = interfaces,
            totalRxBytes = totalRxBytes,
            totalTxBytes = totalTxBytes,
            timestamp = timestamp
        )
    }

    /**
     * Parses a single interface line from /proc/net/dev.
     * 
     * Format: "interface: rx_bytes rx_packets rx_errs rx_drop ... tx_bytes tx_packets ..."
     */
    private fun parseInterfaceLine(line: String, timestamp: Long): InterfaceStats? {
        try {
            val colonIndex = line.indexOf(':')
            if (colonIndex == -1) return null

            val interfaceName = line.substring(0, colonIndex).trim()
            val values = line.substring(colonIndex + 1).trim().split("\\s+".toRegex())

            if (values.size < 16) {
                Timber.tag(TAG).w("Invalid line format for $interfaceName: ${values.size} values")
                return null
            }

            return InterfaceStats(
                interfaceName = interfaceName,
                rxBytes = values[0].toLongOrNull() ?: 0L,
                rxPackets = values[1].toLongOrNull() ?: 0L,
                rxErrors = values[2].toLongOrNull() ?: 0L,
                rxDropped = values[3].toLongOrNull() ?: 0L,
                txBytes = values[8].toLongOrNull() ?: 0L,
                txPackets = values[9].toLongOrNull() ?: 0L,
                txErrors = values[10].toLongOrNull() ?: 0L,
                txDropped = values[11].toLongOrNull() ?: 0L,
                timestamp = timestamp
            )
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Error parsing interface line: $line")
            return null
        }
    }

    /**
     * Initializes baseline for delta calculations.
     */
    private fun initializeBaseline(snapshot: NetworkSnapshot) {
        previousSnapshot = snapshot
        sessionStartRxBytes = snapshot.totalRxBytes
        sessionStartTxBytes = snapshot.totalTxBytes
        isInitialized = true
        Timber.tag(TAG).d("Network stats baseline initialized: RX=${snapshot.totalRxBytes}, TX=${snapshot.totalTxBytes}")
    }

    /**
     * Creates initial stats (first measurement, no speed data).
     */
    private fun createInitialStats(snapshot: NetworkSnapshot): NetworkTrafficStats {
        return NetworkTrafficStats(
            ingressBytesPerSec = 0L,
            egressBytesPerSec = 0L,
            ingressMbps = 0f,
            egressMbps = 0f,
            peakIngressMbps = 0f,
            peakEgressMbps = 0f,
            peakIngressTimestamp = 0L,
            peakEgressTimestamp = 0L,
            totalIngressBytes = snapshot.totalRxBytes,
            totalEgressBytes = snapshot.totalTxBytes,
            sessionIngressBytes = 0L,
            sessionEgressBytes = 0L,
            timestamp = snapshot.timestamp,
            isAvailable = true
        )
    }

    /**
     * Creates stats with calculated speeds.
     */
    private fun createStats(
        ingressBytesPerSec: Long,
        egressBytesPerSec: Long,
        snapshot: NetworkSnapshot
    ): NetworkTrafficStats {
        return NetworkTrafficStats(
            ingressBytesPerSec = ingressBytesPerSec,
            egressBytesPerSec = egressBytesPerSec,
            ingressMbps = ingressBytesPerSec * BYTES_TO_MBPS,
            egressMbps = egressBytesPerSec * BYTES_TO_MBPS,
            peakIngressMbps = peakIngressMbps,
            peakEgressMbps = peakEgressMbps,
            peakIngressTimestamp = peakIngressTimestamp,
            peakEgressTimestamp = peakEgressTimestamp,
            totalIngressBytes = snapshot.totalRxBytes,
            totalEgressBytes = snapshot.totalTxBytes,
            sessionIngressBytes = snapshot.totalRxBytes - sessionStartRxBytes,
            sessionEgressBytes = snapshot.totalTxBytes - sessionStartTxBytes,
            timestamp = snapshot.timestamp,
            isAvailable = true
        )
    }

    /**
     * Updates peak values if current exceeds previous peak.
     */
    private fun updatePeakValues(ingressMbps: Float, egressMbps: Float, timestamp: Long) {
        if (ingressMbps > peakIngressMbps) {
            peakIngressMbps = ingressMbps
            peakIngressTimestamp = timestamp
            Timber.tag(TAG).d("New peak ingress: %.2f Mbps".format(ingressMbps))
        }
        if (egressMbps > peakEgressMbps) {
            peakEgressMbps = egressMbps
            peakEgressTimestamp = timestamp
            Timber.tag(TAG).d("New peak egress: %.2f Mbps".format(egressMbps))
        }
    }

    /**
     * Resets baseline and peak values.
     * Call when starting a new monitoring session.
     */
    fun resetBaseline() {
        isInitialized = false
        previousSnapshot = NetworkSnapshot.EMPTY
        peakIngressMbps = 0f
        peakEgressMbps = 0f
        peakIngressTimestamp = 0L
        peakEgressTimestamp = 0L
        sessionStartRxBytes = 0L
        sessionStartTxBytes = 0L
        Timber.tag(TAG).d("Network stats baseline reset")
    }

    /**
     * Returns current peak values.
     */
    fun getPeakValues(): Pair<Float, Float> = Pair(peakIngressMbps, peakEgressMbps)

    /**
     * Returns current peak timestamps.
     */
    fun getPeakTimestamps(): Pair<Long, Long> = Pair(peakIngressTimestamp, peakEgressTimestamp)

    /**
     * Returns list of active network interfaces.
     */
    suspend fun getActiveInterfaces(): List<InterfaceStats> = withContext(dispatcherProvider.io) {
        readProcNetDev().activeInterfaces
    }

    /**
     * Checks if /proc/net/dev is accessible.
     */
    fun isAvailable(): Boolean {
        val file = File(PROC_NET_DEV)
        return file.exists() && file.canRead()
    }
}
