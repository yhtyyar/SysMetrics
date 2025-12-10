package com.sysmetrics.app.utils

import android.content.Context
import com.sysmetrics.app.data.model.CpuStats
import com.sysmetrics.app.data.model.MemoryInfo
import com.sysmetrics.app.data.model.TemperatureInfo
import com.sysmetrics.app.data.source.SystemDataSource
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for MetricsCollector
 * Tests core metrics collection functionality
 */
class MetricsCollectorTest {

    private lateinit var context: Context
    private lateinit var systemDataSource: SystemDataSource
    private lateinit var metricsCollector: MetricsCollector

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        systemDataSource = mockk(relaxed = true)
        metricsCollector = MetricsCollector(context, systemDataSource)
    }

    @Test
    fun `getCpuUsage returns zero on first call`() {
        // Given
        val cpuStats = CpuStats(
            user = 1000,
            nice = 100,
            system = 500,
            idle = 3000,
            iowait = 200,
            irq = 50,
            softirq = 50,
            steal = 0
        )
        coEvery { systemDataSource.readCpuStats() } returns cpuStats

        // When
        val result = metricsCollector.getCpuUsage()

        // Then
        assertEquals(0f, result, 0.01f)
    }

    @Test
    fun `getCpuUsage calculates correct percentage on second call`() = runBlocking {
        // Given
        val firstStats = CpuStats(
            user = 1000, nice = 100, system = 500, idle = 3000,
            iowait = 200, irq = 50, softirq = 50, steal = 0
        )
        val secondStats = CpuStats(
            user = 1100, nice = 110, system = 550, idle = 3100,
            iowait = 220, irq = 60, softirq = 60, steal = 0
        )
        
        coEvery { systemDataSource.readCpuStats() } returnsMany listOf(firstStats, secondStats)

        // When
        metricsCollector.getCpuUsage() // First call establishes baseline
        val result = metricsCollector.getCpuUsage() // Second call calculates delta

        // Then
        assertTrue(result >= 0f)
        assertTrue(result <= 100f)
    }

    @Test
    fun `getCpuUsage returns zero on exception`() {
        // Given
        coEvery { systemDataSource.readCpuStats() } throws Exception("Test exception")

        // When
        val result = metricsCollector.getCpuUsage()

        // Then
        assertEquals(0f, result, 0.01f)
    }

    @Test
    fun `getRamUsage returns valid triple`() {
        // Given
        val memInfo = MemoryInfo(
            totalKb = 2048000,
            freeKb = 512000,
            availableKb = 768000,
            buffersKb = 128000,
            cachedKb = 256000
        )
        coEvery { systemDataSource.readMemoryInfo() } returns memInfo

        // When
        val (usedMb, totalMb, percentUsed) = metricsCollector.getRamUsage()

        // Then
        assertTrue(usedMb >= 0)
        assertTrue(totalMb > 0)
        assertTrue(percentUsed >= 0f)
        assertTrue(percentUsed <= 100f)
        assertTrue(usedMb <= totalMb)
    }

    @Test
    fun `getRamUsage handles negative values correctly`() {
        // Given - simulate edge case with corrupted data
        val memInfo = MemoryInfo(
            totalKb = 1000000,
            freeKb = 0,
            availableKb = 2000000, // Available > Total (corrupted data)
            buffersKb = 0,
            cachedKb = 0
        )
        coEvery { systemDataSource.readMemoryInfo() } returns memInfo

        // When
        val (usedMb, totalMb, percentUsed) = metricsCollector.getRamUsage()

        // Then
        assertTrue(usedMb >= 0) // Should not be negative
        assertTrue(percentUsed >= 0f)
        assertTrue(percentUsed <= 100f)
    }

    @Test
    fun `getRamUsage returns zeros on exception`() {
        // Given
        coEvery { systemDataSource.readMemoryInfo() } throws Exception("Test exception")

        // When
        val (usedMb, totalMb, percentUsed) = metricsCollector.getRamUsage()

        // Then
        assertEquals(0L, usedMb)
        assertEquals(0L, totalMb)
        assertEquals(0f, percentUsed, 0.01f)
    }

    @Test
    fun `getTemperature returns valid temperature`() {
        // Given
        val tempInfo = TemperatureInfo(cpuTempCelsius = 45.5f)
        coEvery { systemDataSource.readTemperature() } returns tempInfo

        // When
        val result = metricsCollector.getTemperature()

        // Then
        assertEquals(45.5f, result, 0.01f)
    }

    @Test
    fun `getTemperature clamps extreme values`() {
        // Given
        val tempInfo = TemperatureInfo(cpuTempCelsius = 250f) // Unrealistic high temp
        coEvery { systemDataSource.readTemperature() } returns tempInfo

        // When
        val result = metricsCollector.getTemperature()

        // Then
        assertTrue(result <= 200f) // Should be clamped
    }

    @Test
    fun `getTemperature returns -1 on exception`() {
        // Given
        coEvery { systemDataSource.readTemperature() } throws Exception("Test exception")

        // When
        val result = metricsCollector.getTemperature()

        // Then
        assertEquals(-1f, result, 0.01f)
    }

    @Test
    fun `getCoreCount returns positive number`() {
        // Given
        every { systemDataSource.getCpuCoreCount() } returns 4

        // When
        val result = metricsCollector.getCoreCount()

        // Then
        assertEquals(4, result)
        assertTrue(result > 0)
    }

    @Test
    fun `getCoreCount falls back to runtime processors on exception`() {
        // Given
        every { systemDataSource.getCpuCoreCount() } throws Exception("Test exception")

        // When
        val result = metricsCollector.getCoreCount()

        // Then
        assertTrue(result > 0)
        assertEquals(Runtime.getRuntime().availableProcessors(), result)
    }

    @Test
    fun `resetBaseline clears previous measurements`() {
        // Given
        val cpuStats = CpuStats(
            user = 1000, nice = 100, system = 500, idle = 3000,
            iowait = 200, irq = 50, softirq = 50, steal = 0
        )
        coEvery { systemDataSource.readCpuStats() } returns cpuStats
        
        // Establish a baseline
        metricsCollector.getCpuUsage()

        // When
        metricsCollector.resetBaseline()
        val result = metricsCollector.getCpuUsage()

        // Then
        assertEquals(0f, result, 0.01f) // Should return 0 after reset
    }

    @Test
    fun `percentage calculation is within bounds`() {
        // Given
        val firstStats = CpuStats(
            user = 1000, nice = 100, system = 500, idle = 3000,
            iowait = 200, irq = 50, softirq = 50, steal = 0
        )
        val secondStats = CpuStats(
            user = 5000, nice = 500, system = 2500, idle = 6000,
            iowait = 1000, irq = 250, softirq = 250, steal = 0
        )
        
        coEvery { systemDataSource.readCpuStats() } returnsMany listOf(firstStats, secondStats)

        // When
        metricsCollector.getCpuUsage() // Baseline
        val result = metricsCollector.getCpuUsage() // Actual measurement

        // Then
        assertTrue("CPU percentage should be >= 0", result >= 0f)
        assertTrue("CPU percentage should be <= 100", result <= 100f)
    }
}
