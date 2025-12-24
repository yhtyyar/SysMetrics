package com.sysmetrics.app.data.repository

import com.sysmetrics.app.data.model.BatteryInfo
import com.sysmetrics.app.data.model.CpuStats
import com.sysmetrics.app.data.model.GpuInfo
import com.sysmetrics.app.data.model.MemoryInfo
import com.sysmetrics.app.data.model.NetworkStats
import com.sysmetrics.app.data.model.SystemMetrics
import com.sysmetrics.app.data.model.TemperatureInfo
import com.sysmetrics.app.data.source.BatteryDataSource
import com.sysmetrics.app.data.source.GpuDataSource
import com.sysmetrics.app.data.source.NetworkDataSource
import com.sysmetrics.app.data.source.SystemDataSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for SystemMetricsRepository.
 * Tests metrics collection and flow emission.
 */
class SystemMetricsRepositoryTest {

    private lateinit var systemDataSource: SystemDataSource
    private lateinit var gpuDataSource: GpuDataSource
    private lateinit var networkDataSource: NetworkDataSource
    private lateinit var batteryDataSource: BatteryDataSource
    private lateinit var repository: SystemMetricsRepository

    private val testCpuStats = CpuStats(
        user = 1000L,
        nice = 100L,
        system = 500L,
        idle = 8000L,
        iowait = 200L,
        irq = 50L,
        softirq = 50L
    )

    private val testMemoryInfo = MemoryInfo(
        totalKb = 8_000_000L,
        freeKb = 2_000_000L,
        availableKb = 3_000_000L,
        buffersKb = 100_000L,
        cachedKb = 500_000L
    )

    private val testTemperatureInfo = TemperatureInfo(
        cpuTempCelsius = 45.5f,
        thermalZones = mapOf("thermal_zone0" to 45.5f)
    )

    @Before
    fun setup() {
        systemDataSource = mockk(relaxed = true)
        gpuDataSource = mockk(relaxed = true)
        networkDataSource = mockk(relaxed = true)
        batteryDataSource = mockk(relaxed = true)

        coEvery { systemDataSource.readCpuStats() } returns testCpuStats
        coEvery { systemDataSource.readMemoryInfo() } returns testMemoryInfo
        coEvery { systemDataSource.readTemperature() } returns testTemperatureInfo
        coEvery { systemDataSource.getCpuCoreCount() } returns 4
        coEvery { gpuDataSource.readGpuInfo() } returns GpuInfo.UNAVAILABLE
        coEvery { networkDataSource.readNetworkStats() } returns NetworkStats.EMPTY
        coEvery { batteryDataSource.readBatteryInfo() } returns BatteryInfo.UNAVAILABLE

        repository = SystemMetricsRepository(
            systemDataSource,
            gpuDataSource,
            networkDataSource,
            batteryDataSource
        )
    }

    @Test
    fun `collectMetrics should return valid SystemMetrics`() = runTest {
        // When
        val metrics = repository.collectMetrics()

        // Then
        assertEquals(4, metrics.cpuCores)
        assertTrue(metrics.ramTotalMb > 0)
        assertTrue(metrics.ramUsedMb >= 0)
        assertTrue(metrics.ramUsagePercent >= 0f)
        assertTrue(metrics.ramUsagePercent <= 100f)
    }

    @Test
    fun `collectMetrics should include temperature when available`() = runTest {
        // When
        val metrics = repository.collectMetrics()

        // Then
        assertEquals(45.5f, metrics.temperatureCelsius, 0.01f)
    }

    @Test
    fun `collectMetrics should call all data sources`() = runTest {
        // When
        repository.collectMetrics()

        // Then
        coVerify { systemDataSource.readCpuStats() }
        coVerify { systemDataSource.readMemoryInfo() }
        coVerify { systemDataSource.readTemperature() }
        coVerify { gpuDataSource.readGpuInfo() }
        coVerify { networkDataSource.readNetworkStats() }
        coVerify { batteryDataSource.readBatteryInfo() }
    }

    @Test
    fun `resetBaseline should clear all caches`() = runTest {
        // When
        repository.resetBaseline()

        // Then
        coVerify { systemDataSource.clearCache() }
        coVerify { gpuDataSource.clearCache() }
        coVerify { networkDataSource.resetBaseline() }
        coVerify { batteryDataSource.clearCache() }
    }

    @Test
    fun `getMetricsFlow should emit metrics`() = runTest {
        // When
        val metrics = repository.getMetricsFlow(100L).first()

        // Then
        assertTrue(metrics.timestamp > 0)
    }

    @Test
    fun `collectMetrics should return EMPTY on exception`() = runTest {
        // Given
        coEvery { systemDataSource.readCpuStats() } throws RuntimeException("Test exception")

        // When
        val metrics = repository.collectMetrics()

        // Then
        assertEquals(SystemMetrics.EMPTY, metrics)
    }

    @Test
    fun `CPU usage should be calculated correctly between snapshots`() = runTest {
        // Given - first snapshot
        val firstStats = CpuStats(
            user = 1000L, nice = 0L, system = 500L,
            idle = 8000L, iowait = 0L, irq = 0L, softirq = 0L
        )
        val secondStats = CpuStats(
            user = 1200L, nice = 0L, system = 600L,
            idle = 8200L, iowait = 0L, irq = 0L, softirq = 0L
        )
        
        coEvery { systemDataSource.readCpuStats() } returnsMany listOf(firstStats, secondStats)

        // When - first call establishes baseline
        repository.collectMetrics()
        // Second call calculates delta
        val metrics = repository.collectMetrics()

        // Then - CPU usage should be between 0-100
        assertTrue(metrics.cpuUsage >= 0f)
        assertTrue(metrics.cpuUsage <= 100f)
    }
}
