package com.sysmetrics.app.native_bridge

import com.sysmetrics.app.domain.formatter.IStringFormatter

/**
 * Fallback implementation of string formatting.
 * Uses standard Java/Kotlin formatting when native code is unavailable.
 */
class FallbackStringFormatter : IStringFormatter {

    override fun formatTime(hour: Int, minute: Int): String {
        return String.format("%02d:%02d", hour, minute)
    }

    override fun formatCpu(cpuPercent: Float): String {
        return String.format("CPU: %.1f%%", cpuPercent)
    }

    override fun formatRam(usedMb: Long, totalMb: Long): String {
        return String.format("RAM: %d/%d MB", usedMb, totalMb)
    }

    override fun formatSelfStats(cpuPercent: Float, ramMb: Long): String {
        return String.format("Self: %.1f%% / %dM", cpuPercent, ramMb)
    }

    override fun isNativeAvailable(): Boolean = false
}
