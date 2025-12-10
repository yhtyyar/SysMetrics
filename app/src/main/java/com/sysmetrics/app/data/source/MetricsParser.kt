package com.sysmetrics.app.data.source

import com.sysmetrics.app.data.model.CpuStats
import com.sysmetrics.app.data.model.MemoryInfo
import timber.log.Timber

/**
 * Parser for system metrics from proc/sys filesystem.
 * Optimized for minimal allocations and maximum performance.
 */
object MetricsParser {

    private val WHITESPACE_REGEX = "\\s+".toRegex()

    /**
     * Parses CPU statistics from a /proc/stat line.
     * Format: cpu user nice system idle iowait irq softirq steal guest guest_nice
     */
    fun parseCpuStats(statLine: String): CpuStats {
        return try {
            Timber.tag("PARSER").v("\ud83d\udd0d Parsing CPU line (length=%d): '%s'", 
                statLine.length, statLine.take(80))
            
            val parts = statLine.split(WHITESPACE_REGEX).filter { it.isNotEmpty() }
            Timber.tag("PARSER").v("\ud83d\udce6 Split into %d parts: %s", parts.size, parts.take(10))
            
            if (parts.size < 8) {
                Timber.tag("PARSER").e("\u274c Insufficient parts: %d (need at least 8)", parts.size)
                return CpuStats.EMPTY
            }
            
            if (parts[0] != "cpu") {
                Timber.tag("PARSER").w("\u26a0\ufe0f First part is '%s', expected 'cpu'", parts[0])
            }

            val result = CpuStats(
                user = parts[1].toLongOrNull() ?: 0,
                nice = parts[2].toLongOrNull() ?: 0,
                system = parts[3].toLongOrNull() ?: 0,
                idle = parts[4].toLongOrNull() ?: 0,
                iowait = parts[5].toLongOrNull() ?: 0,
                irq = parts[6].toLongOrNull() ?: 0,
                softirq = parts[7].toLongOrNull() ?: 0
            )
            
            Timber.tag("PARSER").d("\u2705 Parsed: user=%d, nice=%d, system=%d, idle=%d, iowait=%d, irq=%d, softirq=%d",
                result.user, result.nice, result.system, result.idle, 
                result.iowait, result.irq, result.softirq)
            
            result
        } catch (e: Exception) {
            Timber.tag("PARSER").e(e, "\u274c Exception parsing CPU stats")
            CpuStats.EMPTY
        }
    }

    /**
     * Calculates CPU usage percentage between two snapshots.
     * Returns value between 0-100.
     */
    fun calculateCpuUsage(previous: CpuStats, current: CpuStats): Float {
        if (previous == CpuStats.EMPTY) return 0f

        val prevTotal = previous.total()
        val currTotal = current.total()
        val totalDiff = currTotal - prevTotal

        if (totalDiff <= 0) return 0f

        val prevActive = previous.active()
        val currActive = current.active()
        val activeDiff = currActive - prevActive

        return ((activeDiff.toFloat() / totalDiff.toFloat()) * 100f).coerceIn(0f, 100f)
    }

    /**
     * Parses memory information from /proc/meminfo content.
     */
    fun parseMemoryInfo(content: String): MemoryInfo {
        return try {
            val memMap = mutableMapOf<String, Long>()

            content.lineSequence().forEach { line ->
                val parts = line.split(WHITESPACE_REGEX).filter { it.isNotEmpty() }
                if (parts.size >= 2) {
                    val key = parts[0].removeSuffix(":")
                    val value = parts[1].toLongOrNull()
                    if (value != null) {
                        memMap[key] = value
                    }
                }
            }

            MemoryInfo(
                totalKb = memMap["MemTotal"] ?: 0,
                freeKb = memMap["MemFree"] ?: 0,
                availableKb = memMap["MemAvailable"] ?: memMap["MemFree"] ?: 0,
                buffersKb = memMap["Buffers"] ?: 0,
                cachedKb = memMap["Cached"] ?: 0
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse memory info")
            MemoryInfo.EMPTY
        }
    }

    /**
     * Parses temperature value from thermal zone file.
     * Temperature is typically in millidegrees Celsius.
     */
    fun parseTemperature(content: String): Float {
        return try {
            val milliCelsius = content.trim().toLongOrNull() ?: 0L
            milliCelsius / 1000f
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse temperature")
            0f
        }
    }
}
