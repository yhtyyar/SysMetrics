package com.sysmetrics.app.data.source

import android.net.TrafficStats
import com.sysmetrics.app.core.di.DispatcherProvider
import com.sysmetrics.app.data.model.NetworkStats
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data source for network traffic statistics.
 * Tracks download/upload speeds using TrafficStats API.
 */
@Singleton
class NetworkDataSource @Inject constructor(
    private val dispatcherProvider: DispatcherProvider
) {
    companion object {
        private const val TAG = "NETWORK_DATA"
        private const val MIN_TIME_DELTA_MS = 100L // Minimum time between measurements
    }

    private var lastRxBytes = 0L
    private var lastTxBytes = 0L
    private var lastTimestamp = 0L
    private var isInitialized = false

    /**
     * Reads current network statistics.
     * Calculates speeds based on delta from previous reading.
     */
    suspend fun readNetworkStats(): NetworkStats = withContext(dispatcherProvider.io) {
        try {
            val currentRx = TrafficStats.getTotalRxBytes()
            val currentTx = TrafficStats.getTotalTxBytes()
            val currentTime = System.currentTimeMillis()

            // Check if TrafficStats is supported
            if (currentRx == TrafficStats.UNSUPPORTED.toLong() || 
                currentTx == TrafficStats.UNSUPPORTED.toLong()) {
                Timber.tag(TAG).w("TrafficStats not supported on this device")
                return@withContext NetworkStats(isAvailable = false)
            }

            // Initialize baseline on first call
            if (!isInitialized) {
                lastRxBytes = currentRx
                lastTxBytes = currentTx
                lastTimestamp = currentTime
                isInitialized = true
                
                Timber.tag(TAG).d("Network monitoring initialized")
                
                // Return zeros for first call
                return@withContext NetworkStats(
                    downloadSpeedKbps = 0f,
                    uploadSpeedKbps = 0f,
                    totalDownloadMb = currentRx / (1024f * 1024f),
                    totalUploadMb = currentTx / (1024f * 1024f),
                    isAvailable = true
                )
            }

            val timeDeltaMs = currentTime - lastTimestamp
            
            // Skip calculation if time delta is too small
            if (timeDeltaMs < MIN_TIME_DELTA_MS) {
                return@withContext NetworkStats(
                    downloadSpeedKbps = 0f,
                    uploadSpeedKbps = 0f,
                    totalDownloadMb = currentRx / (1024f * 1024f),
                    totalUploadMb = currentTx / (1024f * 1024f),
                    isAvailable = true
                )
            }

            val timeDeltaSec = timeDeltaMs / 1000f

            // Calculate speeds in KB/s
            val rxDelta = (currentRx - lastRxBytes).coerceAtLeast(0L)
            val txDelta = (currentTx - lastTxBytes).coerceAtLeast(0L)
            
            val downloadSpeedKbps = (rxDelta / timeDeltaSec / 1024f).coerceAtLeast(0f)
            val uploadSpeedKbps = (txDelta / timeDeltaSec / 1024f).coerceAtLeast(0f)

            // Update last values
            lastRxBytes = currentRx
            lastTxBytes = currentTx
            lastTimestamp = currentTime

            val stats = NetworkStats(
                downloadSpeedKbps = downloadSpeedKbps,
                uploadSpeedKbps = uploadSpeedKbps,
                totalDownloadMb = currentRx / (1024f * 1024f),
                totalUploadMb = currentTx / (1024f * 1024f),
                isAvailable = true
            )

            if (downloadSpeedKbps > 0f || uploadSpeedKbps > 0f) {
                Timber.tag(TAG).v("Network: ${stats.formatDownloadSpeed()}, ${stats.formatUploadSpeed()}")
            }

            stats
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error reading network stats")
            NetworkStats.EMPTY
        }
    }

    /**
     * Resets the baseline for network statistics.
     * Useful when resuming monitoring after a pause.
     */
    fun resetBaseline() {
        isInitialized = false
        lastRxBytes = 0L
        lastTxBytes = 0L
        lastTimestamp = 0L
        Timber.tag(TAG).d("Network baseline reset")
    }

    /**
     * Gets instantaneous network stats without speed calculation.
     */
    suspend fun getTotalStats(): Pair<Float, Float> = withContext(dispatcherProvider.io) {
        try {
            val currentRx = TrafficStats.getTotalRxBytes()
            val currentTx = TrafficStats.getTotalTxBytes()
            
            if (currentRx == TrafficStats.UNSUPPORTED.toLong() || 
                currentTx == TrafficStats.UNSUPPORTED.toLong()) {
                return@withContext Pair(0f, 0f)
            }
            
            Pair(
                currentRx / (1024f * 1024f), // Download in MB
                currentTx / (1024f * 1024f)  // Upload in MB
            )
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error getting total stats")
            Pair(0f, 0f)
        }
    }
}
