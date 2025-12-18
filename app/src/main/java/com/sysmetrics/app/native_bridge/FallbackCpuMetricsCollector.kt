package com.sysmetrics.app.native_bridge

import com.sysmetrics.app.domain.collector.ICpuMetricsCollector
import timber.log.Timber

/**
 * Fallback implementation of CPU metrics collection.
 * Uses standard Android APIs when native code is unavailable.
 */
class FallbackCpuMetricsCollector : ICpuMetricsCollector {

    override fun getCpuUsage(): Float = -1f

    override fun resetBaseline() {
        Timber.d("Fallback CPU collector: baseline reset (no-op)")
    }

    override fun getCoreCount(): Int {
        return try {
            Runtime.getRuntime().availableProcessors()
        } catch (e: Exception) {
            Timber.w(e, "Failed to get core count in fallback")
            4 // Safe default
        }
    }

    override fun getProcessCpuStatsNative(pid: Int): ICpuMetricsCollector.ProcessCpuData? {
        return null // Fallback doesn't support native process stats
    }

    override fun isNativeAvailable(): Boolean = false
}
