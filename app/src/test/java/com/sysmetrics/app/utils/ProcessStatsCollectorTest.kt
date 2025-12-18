package com.sysmetrics.app.utils

import android.app.ActivityManager
import android.content.pm.ApplicationInfo
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for ProcessStatsCollector.
 * Tests process statistics collection logic with mocked dependencies.
 */
class ProcessStatsCollectorTest : ProcessStatsCollectorTestBase() {

    @Test
    fun `getTopApps with count 0 returns empty list`() = runTest {
        // Given
        val count = 0

        // When
        val result = collector.getTopApps(count)

        // Then
        assertEquals(0, result.size)
    }

    @Test
    fun `getTopApps filters system apps correctly`() = runTest {
        // Given
        val runningApps = listOf(
            createMockProcess("com.user.app", "User App", isSystemApp = false),
            createMockProcess("com.system.app", "System App", isSystemApp = true)
        )

        val activityManager = mockk<ActivityManager>()
        every { context.getSystemService(Context.ACTIVITY_SERVICE) } returns activityManager
        every { activityManager.runningAppProcesses } returns runningApps

        val appInfo = mockk<ApplicationInfo>()
        appInfo.flags = if (runningApps[0].processName.contains("user")) 0 else ApplicationInfo.FLAG_SYSTEM
        every { packageManager.getApplicationInfo(any(), 0) } returns appInfo

        // Mock CPU/RAM calculations
        every { cpuMetricsCollector.getCpuUsage() } returns 25.0f

        // When
        val result = collector.getTopApps(5)

        // Then
        assertEquals(1, result.size) // Only user app should be included
        assertEquals("User App", result[0].appName)
    }

    @Test
    fun `getTopApps sorts by CPU usage correctly`() = runTest {
        // Given
        val runningApps = listOf(
            createMockProcess("com.app.low", "Low CPU App", cpuPercent = 5.0f),
            createMockProcess("com.app.high", "High CPU App", cpuPercent = 25.0f),
            createMockProcess("com.app.medium", "Medium CPU App", cpuPercent = 15.0f)
        )

        setupActivityManager(runningApps)

        // When
        val result = collector.getTopAppsByCpu(3)

        // Then
        assertEquals(3, result.size)
        assertEquals("High CPU App", result[0].appName) // Highest CPU first
        assertEquals("Medium CPU App", result[1].appName)
        assertEquals("Low CPU App", result[2].appName)
    }

    @Test
    fun `getTopApps sorts by RAM usage correctly`() = runTest {
        // Given
        val runningApps = listOf(
            createMockProcess("com.app.low", "Low RAM App", ramMb = 50L),
            createMockProcess("com.app.high", "High RAM App", ramMb = 200L),
            createMockProcess("com.app.medium", "Medium RAM App", ramMb = 100L)
        )

        setupActivityManager(runningApps)

        // When
        val result = collector.getTopAppsByRam(3)

        // Then
        assertEquals(3, result.size)
        assertEquals("High RAM App", result[0].appName) // Highest RAM first
        assertEquals("Medium RAM App", result[1].appName)
        assertEquals("Low RAM App", result[2].appName)
    }

    @Test
    fun `getTopApps limits results to specified count`() = runTest {
        // Given
        val runningApps = (1..10).map {
            createMockProcess("com.app.$it", "App $it")
        }

        setupActivityManager(runningApps)

        // When
        val result = collector.getTopApps(3)

        // Then
        assertEquals(3, result.size)
    }

    @Test
    fun `getSelfStats returns valid AppStats for current process`() = runTest {
        // Given
        val activityManager = mockk<ActivityManager>()
        every { context.getSystemService(Context.ACTIVITY_SERVICE) } returns activityManager

        val memoryInfo = mockk<ActivityManager.MemoryInfo>()
        every { activityManager.getMemoryInfo(memoryInfo) } answers {
            memoryInfo.totalMem = 4L * 1024 * 1024 * 1024 // 4GB
            memoryInfo.availMem = 2L * 1024 * 1024 * 1024  // 2GB available
        }

        every { activityManager.getProcessMemoryInfo(any()) } returns arrayOf(
            mockk {
                every { totalPss } returns 50 * 1024 * 1024 // 50MB PSS
            }
        )

        val appInfo = mockk<ApplicationInfo>()
        appInfo.flags = 0 // Not system app
        every { packageManager.getApplicationInfo("com.sysmetrics.app", 0) } returns appInfo
        every { packageManager.getApplicationLabel(appInfo) } returns "SysMetrics"

        // When
        val result = collector.getSelfStats()

        // Then
        assertNotNull(result)
        assertEquals("com.sysmetrics.app", result.packageName)
        assertEquals("SysMetrics", result.appName)
        assertEquals(50L, result.ramMb) // 50MB
    }

    // Helper methods

    private fun createMockProcess(
        packageName: String,
        appName: String,
        isSystemApp: Boolean = false,
        cpuPercent: Float = 10.0f,
        ramMb: Long = 100L
    ): ActivityManager.RunningAppProcessInfo {
        return mockk {
            every { processName } returns packageName
            every { pid } returns packageName.hashCode()
        }
    }

    private fun setupActivityManager(runningApps: List<ActivityManager.RunningAppProcessInfo>) {
        val activityManager = mockk<ActivityManager>()
        every { context.getSystemService(Context.ACTIVITY_SERVICE) } returns activityManager
        every { activityManager.runningAppProcesses } returns runningApps

        // Mock app info for all processes
        val appInfo = mockk<ApplicationInfo>()
        appInfo.flags = 0 // Not system app
        every { packageManager.getApplicationInfo(any(), 0) } returns appInfo
        every { packageManager.getApplicationLabel(appInfo) } answers {
            val packageName = it.invocation.args[0] as String
            packageName.split(".").last().replaceFirstChar { it.uppercase() }
        }

        // Mock memory info
        val memoryInfo = mockk<ActivityManager.MemoryInfo>()
        every { activityManager.getMemoryInfo(memoryInfo) } answers {
            memoryInfo.totalMem = 4L * 1024 * 1024 * 1024 // 4GB
            memoryInfo.availMem = 2L * 1024 * 1024 * 1024  // 2GB available
        }

        every { activityManager.getProcessMemoryInfo(any()) } returns arrayOf(
            mockk {
                every { totalPss } returns 100L * 1024 * 1024 // 100MB PSS
            }
        )
    }
}
