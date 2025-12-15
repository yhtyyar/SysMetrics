package com.sysmetrics.app.utils

import android.content.Context
import com.sysmetrics.app.core.di.DispatcherProvider
import com.sysmetrics.app.data.model.CpuStats
import com.sysmetrics.app.data.source.SystemDataSource
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * CRITICAL TESTS for MetricsCollector
 * These tests MUST PASS for the app to work
 * If any fail, build should be blocked
 */
class MetricsCollectorCriticalTest {
    
    private lateinit var metricsCollector: MetricsCollector
    private val mockContext = mockk<Context>(relaxed = true)
    private val mockSystemDataSource = mockk<SystemDataSource>()
    private val testDispatcher = StandardTestDispatcher()
    private val dispatcherProvider = object : DispatcherProvider {
        override val main = Dispatchers.Main
        override val io = testDispatcher
        override val default = testDispatcher
    }
    
    @Before
    fun setup() {
        metricsCollector = MetricsCollector(
            context = mockContext,
            systemDataSource = mockSystemDataSource,
            dispatcherProvider = dispatcherProvider
        )
    }
    
    @Test
    fun `CRITICAL - getCpuUsage MUST NOT return negative values`() = runTest(testDispatcher) {
        // GIVEN: /proc/stat fails (Android 10+)
        coEvery { mockSystemDataSource.readCpuStats() } returns CpuStats.EMPTY
        
        // Mock ActivityManager for fallback
        mockActivityManager()
        
        // WHEN
        val cpu = metricsCollector.getCpuUsage()
        
        // THEN: Must return valid 0-100 range, NEVER negative
        assertTrue("CPU usage MUST be >= 0, got $cpu", cpu >= 0f)
        assertTrue("CPU usage MUST be <= 100, got $cpu", cpu <= 100f)
        assertFalse("CPU usage MUST NOT be NaN", cpu.isNaN())
        assertFalse("CPU usage MUST NOT be Infinite", cpu.isInfinite())
    }
    
    @Test
    fun `CRITICAL - getRamUsage MUST NOT return zero or negative values`() = runTest(testDispatcher) {
        // GIVEN: Mock ActivityManager
        mockActivityManager()
        
        // WHEN
        val (usedMb, totalMb, percent) = metricsCollector.getRamUsage()
        
        // THEN: Must return valid non-zero values
        assertTrue("Total RAM MUST be > 0, got $totalMb", totalMb > 0)
        assertTrue("Used RAM MUST be >= 0, got $usedMb", usedMb >= 0)
        assertTrue("RAM percent MUST be >= 0, got $percent", percent >= 0f)
        assertTrue("RAM percent MUST be <= 100, got $percent", percent <= 100f)
        assertFalse("RAM percent MUST NOT be NaN", percent.isNaN())
    }
    
    @Test
    fun `CRITICAL - CPU fallback to load average works when proc stat fails`() = runTest(testDispatcher) {
        // GIVEN: /proc/stat returns EMPTY (permission denied)
        coEvery { mockSystemDataSource.readCpuStats() } returns CpuStats.EMPTY
        
        // Mock ActivityManager
        mockActivityManager()
        
        // WHEN
        val cpu = metricsCollector.getCpuUsage()
        
        // THEN: Fallback method must provide valid data
        assertTrue("Fallback CPU MUST work, got $cpu", cpu >= 0f && cpu <= 100f)
    }
    
    @Test
    fun `CRITICAL - Multiple consecutive calls MUST NOT crash`() = runTest(testDispatcher) {
        // GIVEN
        coEvery { mockSystemDataSource.readCpuStats() } returns CpuStats.EMPTY
        mockActivityManager()
        
        // WHEN: Call multiple times rapidly
        repeat(10) {
            val cpu = metricsCollector.getCpuUsage()
            val (usedMb, totalMb, percent) = metricsCollector.getRamUsage()
            
            // THEN: All values valid
            assertTrue("Call $it: CPU valid", cpu >= 0f && cpu <= 100f)
            assertTrue("Call $it: RAM valid", totalMb > 0 && percent >= 0f && percent <= 100f)
        }
    }
    
    @Test
    fun `CRITICAL - initializeBaseline MUST NOT throw exception`() = runTest(testDispatcher) {
        // GIVEN
        coEvery { mockSystemDataSource.readCpuStats() } returns CpuStats.EMPTY
        
        // WHEN/THEN: Must not throw
        try {
            metricsCollector.initializeBaseline()
            // Success - no exception
        } catch (e: Exception) {
            fail("initializeBaseline MUST NOT throw exception, got: ${e.message}")
        }
    }
    
    @Test
    fun `CRITICAL - getCpuUsage with valid proc stat data`() = runTest(testDispatcher) {
        // GIVEN: Valid CPU stats
        val stats1 = CpuStats(
            user = 1000L,
            nice = 100L,
            system = 500L,
            idle = 8000L,
            iowait = 200L,
            irq = 100L,
            softirq = 100L
        )
        val stats2 = CpuStats(
            user = 1500L,
            nice = 100L,
            system = 600L,
            idle = 8200L,
            iowait = 200L,
            irq = 100L,
            softirq = 100L
        )
        
        coEvery { mockSystemDataSource.readCpuStats() } returnsMany listOf(stats1, stats2)
        
        // WHEN
        metricsCollector.initializeBaseline()
        testDispatcher.scheduler.advanceUntilIdle()
        val cpu = metricsCollector.getCpuUsage()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // THEN
        assertTrue("CPU from /proc/stat valid", cpu >= 0f && cpu <= 100f)
    }
    
    @Test
    fun `CRITICAL - RAM values are consistent`() = runTest(testDispatcher) {
        // GIVEN
        mockActivityManager()
        
        // WHEN: Get RAM multiple times
        val ram1 = metricsCollector.getRamUsage()
        val ram2 = metricsCollector.getRamUsage()
        
        // THEN: Total RAM should be consistent
        assertEquals("Total RAM should be same", ram1.second, ram2.second)
        assertTrue("Used RAM should be reasonable", ram1.first <= ram1.second)
        assertTrue("Used RAM should be reasonable", ram2.first <= ram2.second)
    }
    
    /**
     * Helper to mock ActivityManager for fallback methods
     */
    private fun mockActivityManager() {
        val mockActivityManager = mockk<android.app.ActivityManager>()
        val mockMemInfo = android.app.ActivityManager.MemoryInfo().apply {
            totalMem = 4L * 1024 * 1024 * 1024 // 4GB
            availMem = 2L * 1024 * 1024 * 1024 // 2GB available
        }
        
        every { mockActivityManager.getMemoryInfo(any()) } answers {
            val info = firstArg<android.app.ActivityManager.MemoryInfo>()
            info.totalMem = mockMemInfo.totalMem
            info.availMem = mockMemInfo.availMem
        }
        
        every { 
            mockContext.getSystemService(Context.ACTIVITY_SERVICE) 
        } returns mockActivityManager
    }
}
