package com.sysmetrics.app.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for data models.
 */
class SystemMetricsTest {

    @Test
    fun `SystemMetrics EMPTY has default values`() {
        val empty = SystemMetrics.EMPTY
        
        assertEquals(0f, empty.cpuUsage, 0.001f)
        assertEquals(0, empty.cpuCores)
        assertEquals(0L, empty.ramUsedMb)
        assertEquals(0L, empty.ramTotalMb)
        assertEquals(0f, empty.ramUsagePercent, 0.001f)
        assertEquals(0f, empty.temperatureCelsius, 0.001f)
    }

    @Test
    fun `CpuStats EMPTY has zero values`() {
        val empty = CpuStats.EMPTY
        
        assertEquals(0L, empty.user)
        assertEquals(0L, empty.nice)
        assertEquals(0L, empty.system)
        assertEquals(0L, empty.idle)
        assertEquals(0L, empty.total())
        assertEquals(0L, empty.active())
    }

    @Test
    fun `MemoryInfo usedKb calculation`() {
        val memInfo = MemoryInfo(
            totalKb = 8000000,
            freeKb = 2000000,
            availableKb = 3000000,
            buffersKb = 100000,
            cachedKb = 500000
        )
        
        assertEquals(5000000L, memInfo.usedKb)
    }

    @Test
    fun `MemoryInfo usagePercent calculation`() {
        val memInfo = MemoryInfo(
            totalKb = 8000000,
            freeKb = 2000000,
            availableKb = 6000000,
            buffersKb = 0,
            cachedKb = 0
        )
        
        // (8000000 - 6000000) / 8000000 * 100 = 25%
        assertEquals(25f, memInfo.usagePercent, 0.001f)
    }

    @Test
    fun `MemoryInfo usagePercent returns zero when total is zero`() {
        val memInfo = MemoryInfo.EMPTY
        
        assertEquals(0f, memInfo.usagePercent, 0.001f)
    }

    @Test
    fun `TemperatureInfo EMPTY has zero values`() {
        val empty = TemperatureInfo.EMPTY
        
        assertEquals(0f, empty.cpuTempCelsius, 0.001f)
        assertEquals(emptyMap<String, Float>(), empty.thermalZones)
    }

    @Test
    fun `OverlayConfig DEFAULT has expected values`() {
        val default = OverlayConfig.DEFAULT
        
        assertEquals(20, default.positionX)
        assertEquals(20, default.positionY)
        assertEquals(OverlayPosition.TOP_LEFT, default.position)
        assertEquals(1000L, default.updateIntervalMs)
        assertEquals(0.85f, default.opacity, 0.001f)
        assertEquals(true, default.showCpu)
        assertEquals(true, default.showRam)
        assertEquals(true, default.showTemperature)
    }
}
