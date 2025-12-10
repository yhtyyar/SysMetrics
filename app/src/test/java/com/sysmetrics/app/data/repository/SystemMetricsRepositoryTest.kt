package com.sysmetrics.app.data.repository

import com.sysmetrics.app.core.TestDispatcherProvider
import com.sysmetrics.app.core.di.DispatcherProvider
import com.sysmetrics.app.data.model.CpuStats
import com.sysmetrics.app.data.model.MemoryInfo
import com.sysmetrics.app.data.model.SystemMetrics
import com.sysmetrics.app.data.model.TemperatureInfo
import com.sysmetrics.app.data.source.SystemDataSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Unit tests for SystemMetricsRepository.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SystemMetricsRepositoryTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var dispatcherProvider: DispatcherProvider
    private lateinit var systemDataSource: SystemDataSource
    private lateinit var repository: SystemMetricsRepository

    @Before
    fun setup() {
        dispatcherProvider = TestDispatcherProvider(testDispatcher)
        systemDataSource = mock()
        repository = SystemMetricsRepository(systemDataSource)
    }

    @Test
    fun `collectMetrics returns valid SystemMetrics`() = runTest {
        val cpuStats = CpuStats(
            user = 1000, nice = 0, system = 500,
            idle = 8000, iowait = 0, irq = 0, softirq = 0
        )
        val memoryInfo = MemoryInfo(
            totalKb = 8000000,
            freeKb = 2000000,
            availableKb = 3000000,
            buffersKb = 100000,
            cachedKb = 500000
        )
        val tempInfo = TemperatureInfo(cpuTempCelsius = 55f)

        whenever(systemDataSource.readCpuStats()).thenReturn(cpuStats)
        whenever(systemDataSource.readMemoryInfo()).thenReturn(memoryInfo)
        whenever(systemDataSource.readTemperature()).thenReturn(tempInfo)
        whenever(systemDataSource.getCpuCoreCount()).thenReturn(4)

        // First call to establish baseline
        repository.collectMetrics()
        
        // Second call with same stats
        val metrics = repository.collectMetrics()

        assertEquals(4, metrics.cpuCores)
        assertEquals(4882L, metrics.ramUsedMb) // (8000000 - 3000000) / 1024
        assertEquals(7812L, metrics.ramTotalMb) // 8000000 / 1024
        assertEquals(55f, metrics.temperatureCelsius, 0.001f)
    }

    @Test
    fun `collectMetrics returns zero CPU usage on first call`() = runTest {
        repository.resetBaseline()

        val cpuStats = CpuStats(
            user = 1000, nice = 0, system = 500,
            idle = 8000, iowait = 0, irq = 0, softirq = 0
        )

        whenever(systemDataSource.readCpuStats()).thenReturn(cpuStats)
        whenever(systemDataSource.readMemoryInfo()).thenReturn(MemoryInfo.EMPTY)
        whenever(systemDataSource.readTemperature()).thenReturn(TemperatureInfo.EMPTY)
        whenever(systemDataSource.getCpuCoreCount()).thenReturn(4)

        val metrics = repository.collectMetrics()

        assertEquals(0f, metrics.cpuUsage, 0.001f)
    }

    @Test
    fun `collectMetrics calculates CPU usage after baseline`() = runTest {
        repository.resetBaseline()

        val cpuStats1 = CpuStats(
            user = 1000, nice = 0, system = 500,
            idle = 8000, iowait = 0, irq = 0, softirq = 0
        )
        val cpuStats2 = CpuStats(
            user = 1200, nice = 0, system = 600,
            idle = 8200, iowait = 0, irq = 0, softirq = 0
        )

        whenever(systemDataSource.readCpuStats())
            .thenReturn(cpuStats1)
            .thenReturn(cpuStats2)
        whenever(systemDataSource.readMemoryInfo()).thenReturn(MemoryInfo.EMPTY)
        whenever(systemDataSource.readTemperature()).thenReturn(TemperatureInfo.EMPTY)
        whenever(systemDataSource.getCpuCoreCount()).thenReturn(4)

        // First call establishes baseline
        repository.collectMetrics()
        
        // Second call calculates actual usage
        val metrics = repository.collectMetrics()

        // Expected: 60% (300 active diff out of 500 total diff)
        assertEquals(60f, metrics.cpuUsage, 0.001f)
    }

    @Test
    fun `resetBaseline clears previous CPU stats`() = runTest {
        val cpuStats = CpuStats(
            user = 1000, nice = 0, system = 500,
            idle = 8000, iowait = 0, irq = 0, softirq = 0
        )

        whenever(systemDataSource.readCpuStats()).thenReturn(cpuStats)
        whenever(systemDataSource.readMemoryInfo()).thenReturn(MemoryInfo.EMPTY)
        whenever(systemDataSource.readTemperature()).thenReturn(TemperatureInfo.EMPTY)
        whenever(systemDataSource.getCpuCoreCount()).thenReturn(4)

        // Establish baseline
        repository.collectMetrics()
        
        // Reset
        repository.resetBaseline()
        
        // Next call should return 0 CPU again
        val metrics = repository.collectMetrics()

        assertEquals(0f, metrics.cpuUsage, 0.001f)
    }

    @Test
    fun `getMetricsFlow emits metrics`() = runTest {
        val cpuStats = CpuStats(
            user = 1000, nice = 0, system = 500,
            idle = 8000, iowait = 0, irq = 0, softirq = 0
        )

        whenever(systemDataSource.readCpuStats()).thenReturn(cpuStats)
        whenever(systemDataSource.readMemoryInfo()).thenReturn(MemoryInfo.EMPTY)
        whenever(systemDataSource.readTemperature()).thenReturn(TemperatureInfo.EMPTY)
        whenever(systemDataSource.getCpuCoreCount()).thenReturn(8)

        val metrics = repository.getMetricsFlow(100).first()

        assertTrue(metrics != SystemMetrics.EMPTY || metrics.cpuCores == 8)
    }
}
