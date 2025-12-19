package com.sysmetrics.app.data.model.advanced

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Peak statistics tracked over a time window.
 * Used for peak notification toasts.
 */
data class PeakStats(
    val cpuPeak: Float = 0f,
    val cpuPeakTime: Long = 0L,
    val cpuAvg: Float = 0f,
    
    val ramPeakMb: Long = 0L,
    val ramPeakTime: Long = 0L,
    val ramAvgMb: Float = 0f,
    
    val tempPeak: Float = 0f,
    val tempPeakTime: Long = 0L,
    val tempAvg: Float = 0f,
    
    val netIngressPeakMbps: Float = 0f,
    val netIngressPeakTime: Long = 0L,
    
    val netEgressPeakMbps: Float = 0f,
    val netEgressPeakTime: Long = 0L,
    
    val fpsPeak: Int = 0,
    val fpsMin: Int = 0,
    val fpsAvg: Float = 0f,
    val frameDrops: Int = 0,
    
    val windowStartTime: Long = 0L,
    val windowEndTime: Long = System.currentTimeMillis()
) {
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    
    fun formatCpuPeakTime(): String = formatTime(cpuPeakTime)
    fun formatRamPeakTime(): String = formatTime(ramPeakTime)
    fun formatTempPeakTime(): String = formatTime(tempPeakTime)
    fun formatNetIngressPeakTime(): String = formatTime(netIngressPeakTime)
    fun formatNetEgressPeakTime(): String = formatTime(netEgressPeakTime)
    
    private fun formatTime(timestamp: Long): String {
        return if (timestamp > 0) timeFormat.format(Date(timestamp)) else "--:--:--"
    }
    
    fun toDisplayString(
        showCpu: Boolean = true,
        showRam: Boolean = true,
        showTemp: Boolean = true,
        showNet: Boolean = true,
        showFps: Boolean = false
    ): String = buildString {
        appendLine("📊 Peak Stats (Last Window)")
        appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        
        if (showCpu) {
            appendLine("🔥 CPU Peak: ${String.format("%.1f", cpuPeak)}% (${formatCpuPeakTime()})")
        }
        if (showRam) {
            appendLine("💾 RAM Peak: ${ramPeakMb}MB (${formatRamPeakTime()})")
        }
        if (showTemp && tempPeak > 0) {
            appendLine("🌡️ Temp Peak: ${String.format("%.1f", tempPeak)}°C (${formatTempPeakTime()})")
        }
        if (showNet) {
            appendLine("📡 Net Peak: ↓${String.format("%.1f", netIngressPeakMbps)}Mbps ↑${String.format("%.1f", netEgressPeakMbps)}Mbps")
        }
        if (showFps && fpsPeak > 0) {
            appendLine("🎮 FPS: ${fpsMin}-${fpsPeak} (avg: ${String.format("%.1f", fpsAvg)}, drops: $frameDrops)")
        }
        
        appendLine()
        append("Avg: CPU ${String.format("%.1f", cpuAvg)}% | RAM ${String.format("%.0f", ramAvgMb)}MB")
    }
    
    companion object {
        val EMPTY = PeakStats()
    }
}

/**
 * Single metric peak value with timestamp.
 */
data class MetricPeak(
    val value: Float,
    val timestamp: Long,
    val metricType: MetricType
)
