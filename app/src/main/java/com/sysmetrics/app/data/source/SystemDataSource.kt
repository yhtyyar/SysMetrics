package com.sysmetrics.app.data.source

import com.sysmetrics.app.data.model.CpuStats
import com.sysmetrics.app.data.model.MemoryInfo
import com.sysmetrics.app.data.model.TemperatureInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data source for reading system metrics from proc/sys filesystem.
 * All file I/O operations are performed on IO dispatcher.
 */
@Singleton
class SystemDataSource @Inject constructor() {

    companion object {
        private const val PROC_STAT = "/proc/stat"
        private const val PROC_MEMINFO = "/proc/meminfo"
        private const val SYS_THERMAL = "/sys/class/thermal"
    }

    /**
     * Reads CPU statistics from /proc/stat.
     */
    suspend fun readCpuStats(): CpuStats = withContext(Dispatchers.IO) {
        try {
            val file = File(PROC_STAT)
            if (!file.exists() || !file.canRead()) {
                return@withContext CpuStats.EMPTY
            }

            file.bufferedReader().use { reader ->
                val line = reader.readLine() ?: return@withContext CpuStats.EMPTY
                MetricsParser.parseCpuStats(line)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error reading CPU stats")
            CpuStats.EMPTY
        }
    }

    /**
     * Reads memory information from /proc/meminfo.
     */
    suspend fun readMemoryInfo(): MemoryInfo = withContext(Dispatchers.IO) {
        try {
            val file = File(PROC_MEMINFO)
            if (!file.exists() || !file.canRead()) {
                return@withContext MemoryInfo.EMPTY
            }

            val content = file.readText()
            MetricsParser.parseMemoryInfo(content)
        } catch (e: Exception) {
            Timber.e(e, "Error reading memory info")
            MemoryInfo.EMPTY
        }
    }

    /**
     * Reads temperature from thermal zones.
     * Attempts to find CPU temperature from available thermal zones.
     */
    suspend fun readTemperature(): TemperatureInfo = withContext(Dispatchers.IO) {
        try {
            val thermalDir = File(SYS_THERMAL)
            if (!thermalDir.exists() || !thermalDir.canRead()) {
                return@withContext TemperatureInfo.EMPTY
            }

            val thermalZones = mutableMapOf<String, Float>()
            var cpuTemp = 0f

            thermalDir.listFiles()?.filter { it.name.startsWith("thermal_zone") }?.forEach { zone ->
                val tempFile = File(zone, "temp")
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

            TemperatureInfo(cpuTempCelsius = cpuTemp, thermalZones = thermalZones)
        } catch (e: Exception) {
            Timber.e(e, "Error reading temperature")
            TemperatureInfo.EMPTY
        }
    }

    /**
     * Returns the number of available CPU cores.
     */
    fun getCpuCoreCount(): Int = Runtime.getRuntime().availableProcessors()
}
