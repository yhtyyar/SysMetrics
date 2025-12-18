package com.sysmetrics.app.native_bridge

import com.sysmetrics.app.domain.formatter.IStringFormatter
import timber.log.Timber

/**
 * Native implementation of string formatting.
 * Uses JNI for zero-allocation string formatting.
 */
class NativeStringFormatter : IStringFormatter {

    private var isLoaded = false

    init {
        try {
            System.loadLibrary("sysmetrics_native")
            isLoaded = true
            Timber.d("Native formatting library loaded successfully")
        } catch (e: UnsatisfiedLinkError) {
            Timber.w(e, "Failed to load native formatting library, using fallback")
            isLoaded = false
        }
    }

    override fun formatTime(hour: Int, minute: Int): String {
        return if (isLoaded) {
            runCatching { formatTimeString(hour, minute, true) }
                .getOrDefault(String.format("%02d:%02d", hour, minute))
        } else {
            String.format("%02d:%02d", hour, minute)
        }
    }

    override fun formatCpu(cpuPercent: Float): String {
        return if (isLoaded) {
            runCatching { formatCpuString(cpuPercent) }
                .getOrDefault(String.format("CPU: %.1f%%", cpuPercent))
        } else {
            String.format("CPU: %.1f%%", cpuPercent)
        }
    }

    override fun formatRam(usedMb: Long, totalMb: Long): String {
        return if (isLoaded) {
            runCatching { formatRamString(usedMb, totalMb) }
                .getOrDefault(String.format("RAM: %d/%d MB", usedMb, totalMb))
        } else {
            String.format("RAM: %d/%d MB", usedMb, totalMb)
        }
    }

    override fun formatSelfStats(cpuPercent: Float, ramMb: Long): String {
        return if (isLoaded) {
            runCatching { formatSelfStatsString(cpuPercent, ramMb) }
                .getOrDefault(String.format("Self: %.1f%% / %dM", cpuPercent, ramMb))
        } else {
            String.format("Self: %.1f%% / %dM", cpuPercent, ramMb)
        }
    }

    override fun isNativeAvailable(): Boolean = isLoaded

    // Native method declarations
    private external fun formatTimeString(hour: Int, minute: Int, use24h: Boolean): String
    private external fun formatCpuString(cpuPercent: Float): String
    private external fun formatRamString(usedMb: Long, totalMb: Long): String
    private external fun formatSelfStatsString(cpuPercent: Float, ramMb: Long): String
}
