package com.sysmetrics.app.data.model

/**
 * Data class representing complete system metrics snapshot.
 */
data class SystemMetrics(
    val cpuUsage: Float = 0f,
    val cpuCores: Int = 0,
    val ramUsedMb: Long = 0,
    val ramTotalMb: Long = 0,
    val ramUsagePercent: Float = 0f,
    val temperatureCelsius: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        val EMPTY = SystemMetrics()
    }
}

/**
 * CPU statistics from /proc/stat.
 */
data class CpuStats(
    val user: Long = 0,
    val nice: Long = 0,
    val system: Long = 0,
    val idle: Long = 0,
    val iowait: Long = 0,
    val irq: Long = 0,
    val softirq: Long = 0
) {
    fun total(): Long = user + nice + system + idle + iowait + irq + softirq
    fun active(): Long = user + nice + system + irq + softirq

    companion object {
        val EMPTY = CpuStats()
    }
}

/**
 * Memory metrics from /proc/meminfo.
 */
data class MemoryInfo(
    val totalKb: Long = 0,
    val freeKb: Long = 0,
    val availableKb: Long = 0,
    val buffersKb: Long = 0,
    val cachedKb: Long = 0
) {
    /**
     * Used memory calculated as total - available.
     * Clamped to 0 to prevent negative values.
     */
    val usedKb: Long
        get() = (totalKb - availableKb).coerceAtLeast(0)

    /**
     * Memory usage percentage (0-100).
     * Clamped to valid range.
     */
    val usagePercent: Float
        get() = if (totalKb > 0) {
            ((usedKb.toFloat() / totalKb) * 100f).coerceIn(0f, 100f)
        } else 0f

    companion object {
        val EMPTY = MemoryInfo()
    }
}

/**
 * Temperature metrics from /sys/class/thermal/.
 */
data class TemperatureInfo(
    val cpuTempCelsius: Float = 0f,
    val thermalZones: Map<String, Float> = emptyMap()
) {
    companion object {
        val EMPTY = TemperatureInfo()
    }
}
