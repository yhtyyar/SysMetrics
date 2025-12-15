package com.sysmetrics.app.utils

import android.app.ActivityManager
import android.content.Context
import com.sysmetrics.app.core.di.DispatcherProvider
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * CRITICAL TESTS for ProcessStatsCollector
 * These tests MUST PASS for top apps to work
 */
class ProcessStatsCollectorCriticalTest {
    
    private lateinit var collector: ProcessStatsCollector
    private val mockContext = mockk<Context>(relaxed = true)
    private val mockActivityManager = mockk<ActivityManager>()
    private val testDispatcher = StandardTestDispatcher()
    private val dispatcherProvider = object : DispatcherProvider {
        override val main = Dispatchers.Main
        override val io = testDispatcher
        override val default = testDispatcher
    }
    
    @Before
    fun setup() {
        every { 
            mockContext.getSystemService(Context.ACTIVITY_SERVICE) 
        } returns mockActivityManager
        
        collector = ProcessStatsCollector(
            context = mockContext,
            dispatcherProvider = dispatcherProvider
        )
    }
    
    @Test
    fun `CRITICAL - getTopApps MUST NOT crash with empty process list`() = runTest(testDispatcher) {
        // GIVEN: No running processes
        every { mockActivityManager.runningAppProcesses } returns emptyList()
        
        // WHEN
        val topApps = collector.getTopApps(count = 3, sortBy = "combined")
        testDispatcher.scheduler.advanceUntilIdle()
        
        // THEN: Must return empty list, not crash
        assertNotNull("Top apps MUST NOT be null", topApps)
        assertTrue("Top apps should be empty", topApps.isEmpty())
    }
    
    @Test
    fun `CRITICAL - getTopApps MUST NOT crash with null process list`() = runTest(testDispatcher) {
        // GIVEN: Null process list (can happen on some devices)
        every { mockActivityManager.runningAppProcesses } returns null
        
        // WHEN
        val topApps = collector.getTopApps(count = 3, sortBy = "combined")
        testDispatcher.scheduler.advanceUntilIdle()
        
        // THEN: Must handle gracefully
        assertNotNull("Top apps MUST NOT be null", topApps)
        assertTrue("Top apps should be empty when input is null", topApps.isEmpty())
    }
    
    @Test
    fun `CRITICAL - getTopApps with zero count returns empty`() = runTest(testDispatcher) {
        // GIVEN: Valid processes but count = 0
        every { mockActivityManager.runningAppProcesses } returns listOf(
            createMockProcess("com.app1", 1000)
        )
        
        // WHEN
        val topApps = collector.getTopApps(count = 0, sortBy = "combined")
        testDispatcher.scheduler.advanceUntilIdle()
        
        // THEN
        assertTrue("Top apps with count=0 should be empty", topApps.isEmpty())
    }
    
    @Test
    fun `CRITICAL - getTopApps with negative count returns empty`() = runTest(testDispatcher) {
        // GIVEN
        every { mockActivityManager.runningAppProcesses } returns listOf(
            createMockProcess("com.app1", 1000)
        )
        
        // WHEN
        val topApps = collector.getTopApps(count = -1, sortBy = "combined")
        testDispatcher.scheduler.advanceUntilIdle()
        
        // THEN
        assertTrue("Top apps with negative count should be empty", topApps.isEmpty())
    }
    
    @Test
    fun `CRITICAL - getTopApps excludes self process`() = runTest(testDispatcher) {
        // GIVEN: List with self process
        every { mockContext.packageName } returns "com.sysmetrics.app"
        every { mockActivityManager.runningAppProcesses } returns listOf(
            createMockProcess("com.sysmetrics.app", 1000), // Self
            createMockProcess("com.other.app", 2000)
        )
        
        // WHEN
        val topApps = collector.getTopApps(count = 10, sortBy = "combined")
        testDispatcher.scheduler.advanceUntilIdle()
        
        // THEN: Self should be excluded
        assertFalse(
            "Top apps MUST NOT include self",
            topApps.any { it.packageName == "com.sysmetrics.app" }
        )
    }
    
    @Test
    fun `CRITICAL - getTopApps returns valid data`() = runTest(testDispatcher) {
        // GIVEN: Multiple processes
        every { mockContext.packageName } returns "com.sysmetrics.app"
        every { mockActivityManager.runningAppProcesses } returns listOf(
            createMockProcess("com.app1", 1000),
            createMockProcess("com.app2", 2000),
            createMockProcess("com.app3", 3000)
        )
        
        // WHEN
        val topApps = collector.getTopApps(count = 3, sortBy = "combined")
        testDispatcher.scheduler.advanceUntilIdle()
        
        // THEN
        assertNotNull("Top apps MUST NOT be null", topApps)
        assertTrue("Top apps size <= requested count", topApps.size <= 3)
        
        // All values must be valid
        topApps.forEach { app ->
            assertNotNull("Package name MUST NOT be null", app.packageName)
            assertTrue("CPU MUST be >= 0", app.cpuPercent >= 0f)
            assertTrue("RAM MUST be >= 0", app.ramMb >= 0)
            assertFalse("CPU MUST NOT be NaN", app.cpuPercent.isNaN())
        }
    }
    
    /**
     * Helper to create mock process
     */
    private fun createMockProcess(packageName: String, pid: Int): ActivityManager.RunningAppProcessInfo {
        return ActivityManager.RunningAppProcessInfo().apply {
            this.processName = packageName
            this.pid = pid
            this.importance = ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
            this.pkgList = arrayOf(packageName)
        }
    }
}
