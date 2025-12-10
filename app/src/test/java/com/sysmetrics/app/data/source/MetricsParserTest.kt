package com.sysmetrics.app.data.source

import com.sysmetrics.app.data.model.CpuStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for MetricsParser.
 */
class MetricsParserTest {

    @Test
    fun `parseCpuStats with valid line returns correct values`() {
        val line = "cpu  10132153 290696 3084719 46828483 16683 0 25195 0 0 0"
        
        val stats = MetricsParser.parseCpuStats(line)
        
        assertEquals(10132153L, stats.user)
        assertEquals(290696L, stats.nice)
        assertEquals(3084719L, stats.system)
        assertEquals(46828483L, stats.idle)
        assertEquals(16683L, stats.iowait)
        assertEquals(0L, stats.irq)
        assertEquals(25195L, stats.softirq)
    }

    @Test
    fun `parseCpuStats with empty line returns EMPTY`() {
        val stats = MetricsParser.parseCpuStats("")
        
        assertEquals(CpuStats.EMPTY, stats)
    }

    @Test
    fun `parseCpuStats with insufficient parts returns EMPTY`() {
        val line = "cpu  100 200"
        
        val stats = MetricsParser.parseCpuStats(line)
        
        assertEquals(CpuStats.EMPTY, stats)
    }

    @Test
    fun `calculateCpuUsage with EMPTY previous returns zero`() {
        val current = CpuStats(
            user = 1000, nice = 0, system = 500,
            idle = 8000, iowait = 0, irq = 0, softirq = 0
        )
        
        val usage = MetricsParser.calculateCpuUsage(CpuStats.EMPTY, current)
        
        assertEquals(0f, usage, 0.001f)
    }

    @Test
    fun `calculateCpuUsage returns correct percentage`() {
        val previous = CpuStats(
            user = 1000, nice = 0, system = 500,
            idle = 8000, iowait = 0, irq = 0, softirq = 0
        )
        val current = CpuStats(
            user = 1200, nice = 0, system = 600,
            idle = 8200, iowait = 0, irq = 0, softirq = 0
        )
        
        // Total diff: 500, Active diff: 300
        // Expected: 300/500 * 100 = 60%
        val usage = MetricsParser.calculateCpuUsage(previous, current)
        
        assertEquals(60f, usage, 0.001f)
    }

    @Test
    fun `calculateCpuUsage returns zero when no change`() {
        val stats = CpuStats(
            user = 1000, nice = 0, system = 500,
            idle = 8000, iowait = 0, irq = 0, softirq = 0
        )
        
        val usage = MetricsParser.calculateCpuUsage(stats, stats)
        
        assertEquals(0f, usage, 0.001f)
    }

    @Test
    fun `calculateCpuUsage clamps value to 100 percent max`() {
        val previous = CpuStats(
            user = 0, nice = 0, system = 0,
            idle = 100, iowait = 0, irq = 0, softirq = 0
        )
        val current = CpuStats(
            user = 200, nice = 0, system = 0,
            idle = 100, iowait = 0, irq = 0, softirq = 0
        )
        
        val usage = MetricsParser.calculateCpuUsage(previous, current)
        
        assertTrue(usage <= 100f)
    }

    @Test
    fun `parseMemoryInfo with valid content returns correct values`() {
        val content = """
            MemTotal:        8097000 kB
            MemFree:         2345678 kB
            MemAvailable:    3456789 kB
            Buffers:         123456 kB
            Cached:          234567 kB
            SwapTotal:       1048576 kB
        """.trimIndent()
        
        val memInfo = MetricsParser.parseMemoryInfo(content)
        
        assertEquals(8097000L, memInfo.totalKb)
        assertEquals(2345678L, memInfo.freeKb)
        assertEquals(3456789L, memInfo.availableKb)
        assertEquals(123456L, memInfo.buffersKb)
        assertEquals(234567L, memInfo.cachedKb)
    }

    @Test
    fun `parseMemoryInfo with empty content returns EMPTY`() {
        val memInfo = MetricsParser.parseMemoryInfo("")
        
        assertEquals(0L, memInfo.totalKb)
        assertEquals(0L, memInfo.freeKb)
    }

    @Test
    fun `parseMemoryInfo calculates usedKb correctly`() {
        val content = """
            MemTotal:        8000000 kB
            MemFree:         2000000 kB
            MemAvailable:    3000000 kB
        """.trimIndent()
        
        val memInfo = MetricsParser.parseMemoryInfo(content)
        
        // used = total - available = 8000000 - 3000000 = 5000000
        assertEquals(5000000L, memInfo.usedKb)
    }

    @Test
    fun `parseMemoryInfo calculates usagePercent correctly`() {
        val content = """
            MemTotal:        8000000 kB
            MemAvailable:    4000000 kB
        """.trimIndent()
        
        val memInfo = MetricsParser.parseMemoryInfo(content)
        
        // usage = (8000000 - 4000000) / 8000000 * 100 = 50%
        assertEquals(50f, memInfo.usagePercent, 0.001f)
    }

    @Test
    fun `parseTemperature with millidegrees returns celsius`() {
        val temp = MetricsParser.parseTemperature("65000")
        
        assertEquals(65f, temp, 0.001f)
    }

    @Test
    fun `parseTemperature with whitespace returns correct value`() {
        val temp = MetricsParser.parseTemperature("  45500\n")
        
        assertEquals(45.5f, temp, 0.001f)
    }

    @Test
    fun `parseTemperature with invalid content returns zero`() {
        val temp = MetricsParser.parseTemperature("invalid")
        
        assertEquals(0f, temp, 0.001f)
    }

    @Test
    fun `CpuStats total calculation is correct`() {
        val stats = CpuStats(
            user = 100, nice = 50, system = 30,
            idle = 800, iowait = 10, irq = 5, softirq = 5
        )
        
        assertEquals(1000L, stats.total())
    }

    @Test
    fun `CpuStats active calculation excludes idle`() {
        val stats = CpuStats(
            user = 100, nice = 50, system = 30,
            idle = 800, iowait = 10, irq = 5, softirq = 5
        )
        
        // active = user + nice + system + irq + softirq = 100 + 50 + 30 + 5 + 5 = 190
        assertEquals(190L, stats.active())
    }
}
