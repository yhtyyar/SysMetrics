package com.sysmetrics.app.data.source

import com.sysmetrics.app.core.common.Constants.Cache
import com.sysmetrics.app.core.common.Constants.SystemPaths
import com.sysmetrics.app.core.di.DispatcherProvider
import com.sysmetrics.app.data.model.CpuStats
import com.sysmetrics.app.data.model.MemoryInfo
import com.sysmetrics.app.data.model.TemperatureInfo
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

/**
 * Data source for reading system metrics from proc/sys filesystem.
 * Implements caching to reduce file I/O operations.
 * All file I/O operations are performed on IO dispatcher.
 */

class SystemDataSource constructor(
    private val dispatcherProvider: DispatcherProvider
) {
    // Cached values with timestamps
    private var cachedMemoryInfo: MemoryInfo? = null
    private var memoryInfoTimestamp: Long = 0L
    
    private var cachedTemperature: TemperatureInfo? = null
    private var temperatureTimestamp: Long = 0L

    /**
     * Reads CPU statistics from /proc/stat.
     * CPU stats are not cached as they need to be read fresh for accurate calculations.
     */
    suspend fun readCpuStats(): CpuStats = withContext(dispatcherProvider.io) {
        try {
            val file = File(SystemPaths.PROC_STAT)
            Timber.tag("SYS_DATA").v("📁 Reading CPU stats from: %s", file.absolutePath)
            
            if (!file.exists()) {
                Timber.tag("SYS_DATA").e("❌ /proc/stat does NOT exist!")
                return@withContext CpuStats.EMPTY
            }
            
            if (!file.canRead()) {
                Timber.tag("SYS_DATA").e("❌ /proc/stat exists but CANNOT READ (permission denied?)")
                return@withContext CpuStats.EMPTY
            }

            file.bufferedReader().use { reader ->
                val line = reader.readLine()
                if (line == null) {
                    Timber.tag("SYS_DATA").e("❌ /proc/stat is EMPTY (readLine returned null)")
                    return@withContext CpuStats.EMPTY
                }
                
                Timber.tag("SYS_DATA").d("📝 Raw /proc/stat line: '%s'", line.take(100))
                
                val result = MetricsParser.parseCpuStats(line)
                
                Timber.tag("SYS_DATA").d("📦 Parsed CpuStats: total=%d, user=%d, system=%d, idle=%d",
                    result.total(), result.user, result.system, result.idle)
                
                if (result.total() == 0L) {
                    Timber.tag("SYS_DATA").e("❌ Parsed CpuStats has ZERO total! Parsing failed?")
                }
                
                result
            }
        } catch (e: Exception) {
            Timber.tag("SYS_DATA").e(e, "❌ EXCEPTION reading CPU stats")
            CpuStats.EMPTY
        }
    }

    /**
     * Reads memory information from /proc/meminfo.
     * Results are cached for [Cache.MEMORY_CACHE_DURATION_MS] milliseconds.
     */
    suspend fun readMemoryInfo(): MemoryInfo = withContext(dispatcherProvider.io) {
        val now = System.currentTimeMillis()
        
        // Return cached value if still valid
        cachedMemoryInfo?.let { cached ->
            if (now - memoryInfoTimestamp < Cache.MEMORY_CACHE_DURATION_MS) {
                return@withContext cached
            }
        }
        
        try {
            val file = File(SystemPaths.PROC_MEMINFO)
            if (!file.exists() || !file.canRead()) {
                return@withContext MemoryInfo.EMPTY
            }

            val content = file.readText()
            val result = MetricsParser.parseMemoryInfo(content)
            
            // Update cache
            cachedMemoryInfo = result
            memoryInfoTimestamp = now
            
            result
        } catch (e: Exception) {
            Timber.e(e, "Error reading memory info")
            MemoryInfo.EMPTY
        }
    }

    /**
     * Reads temperature from thermal zones.
     * Results are cached for [Cache.TEMPERATURE_CACHE_DURATION_MS] milliseconds.
     * Attempts to find CPU temperature from available thermal zones.
     */
    suspend fun readTemperature(): TemperatureInfo = withContext(dispatcherProvider.io) {
        val now = System.currentTimeMillis()
        
        // Return cached value if still valid
        cachedTemperature?.let { cached ->
            if (now - temperatureTimestamp < Cache.TEMPERATURE_CACHE_DURATION_MS) {
                return@withContext cached
            }
        }
        
        try {
            val thermalDir = File(SystemPaths.SYS_THERMAL)
            if (!thermalDir.exists() || !thermalDir.canRead()) {
                return@withContext TemperatureInfo.EMPTY
            }

            val thermalZones = mutableMapOf<String, Float>()
            var cpuTemp = 0f

            thermalDir.listFiles()
                ?.asSequence()
                ?.filter { it.name.startsWith(SystemPaths.THERMAL_ZONE_PREFIX) }
                ?.forEach { zone ->
                    val tempFile = File(zone, SystemPaths.TEMP_FILE)
                    if (tempFile.exists() && tempFile.canRead()) {
                        val temp = MetricsParser.parseTemperature(tempFile.readText())
                        if (temp > 0f) {
                            thermalZones[zone.name] = temp
                            if (cpuTemp == 0f) {
                                cpuTemp = temp
                            }
                        }
                    }
                }

            val result = TemperatureInfo(cpuTempCelsius = cpuTemp, thermalZones = thermalZones)
            
            // Update cache
            cachedTemperature = result
            temperatureTimestamp = now
            
            result
        } catch (e: Exception) {
            Timber.e(e, "Error reading temperature")
            TemperatureInfo.EMPTY
        }
    }

    /**
     * Returns the number of available CPU cores.
     * Value is cached after first read.
     */
    fun getCpuCoreCount(): Int {
        return Runtime.getRuntime().availableProcessors()
    }
    
    /**
     * Clears all cached values.
     * Useful when forcing a fresh read of all metrics.
     */
    fun clearCache() {
        cachedMemoryInfo = null
        memoryInfoTimestamp = 0L
        cachedTemperature = null
        temperatureTimestamp = 0L
    }
}
