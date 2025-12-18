package com.sysmetrics.app.native_bridge

import com.sysmetrics.app.domain.collector.ICpuMetricsCollector
import timber.log.Timber

/**
 * Native implementation of CPU metrics collection.
 * Uses JNI for high-performance CPU monitoring.
 */
class NativeCpuMetricsCollector : ICpuMetricsCollector {

    private var isLoaded = false

    init {
        try {
            System.loadLibrary("sysmetrics_native")
            isLoaded = true
            Timber.d("Native CPU metrics library loaded successfully")
        } catch (e: UnsatisfiedLinkError) {
            Timber.w(e, "Failed to load native CPU library, using fallback")
            isLoaded = false
        }
    }

    override fun getCpuUsage(): Float {
        return if (isLoaded) {
            runCatching { getCpuUsageNative() }.getOrDefault(-1f)
        } else {
            -1f
        }
    }

    override fun resetBaseline() {
        if (isLoaded) {
            runCatching { resetCpuBaselineNative() }
        }
    }

    override fun getCoreCount(): Int {
        return if (isLoaded) {
            runCatching { getCpuCoreCountNative() }.getOrDefault(-1)
        } else {
            -1
        }
    }

    override fun getProcessCpuStatsNative(pid: Int): ICpuMetricsCollector.ProcessCpuData? {
        return if (isLoaded) {
            runCatching { 
                val stats = getProcessCpuStats(pid)
                if (stats != null && stats.size >= 3) {
                    ICpuMetricsCollector.ProcessCpuData(
                        utime = stats[0],
                        stime = stats[1],
                        totalTime = stats[2]
                    )
                } else {
                    null
                }
            }.getOrDefault(null)
        } else {
            null
        }
    }

    override fun isNativeAvailable(): Boolean = isLoaded

    // Native method declarations
    private external fun getCpuUsageNative(): Float
    private external fun resetCpuBaselineNative()
    private external fun getCpuCoreCountNative(): Int
    private external fun getProcessCpuStats(pid: Int): LongArray?
}
