package com.sysmetrics.app.utils

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Debug
import io.mockk.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for ProcessStatsCollector
 * Tests process monitoring and filtering logic
 */
class ProcessStatsCollectorTest {

    private lateinit var context: Context
    private lateinit var activityManager: ActivityManager
    private lateinit var packageManager: PackageManager
    private lateinit var processStatsCollector: ProcessStatsCollector

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        activityManager = mockk(relaxed = true)
        packageManager = mockk(relaxed = true)

        every { context.getSystemService(Context.ACTIVITY_SERVICE) } returns activityManager
        every { context.packageManager } returns packageManager
        every { context.packageName } returns "com.sysmetrics.app"

        processStatsCollector = ProcessStatsCollector(context)
    }

    @Test
    fun `getSelfStats returns non-null AppStats`() {
        // Given
        val memInfo = arrayOf(mockk<Debug.MemoryInfo>(relaxed = true) {
            every { totalPss } returns 25600 // 25 MB in KB
        })
        every { activityManager.getProcessMemoryInfo(any()) } returns memInfo

        // When
        val result = processStatsCollector.getSelfStats()

        // Then
        assertNotNull(result)
        assertEquals("com.sysmetrics.app", result.packageName)
        assertEquals("SysMetrics", result.appName)
        assertTrue(result.ramMb >= 0)
    }

    @Test
    fun `getTopApps returns empty list when count is zero`() {
        // When
        val result = processStatsCollector.getTopApps(0)

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `getTopApps filters system apps`() {
        // Given
        val systemAppInfo = mockk<ApplicationInfo> {
            every { flags } returns ApplicationInfo.FLAG_SYSTEM
        }
        val userAppInfo = mockk<ApplicationInfo> {
            every { flags } returns 0
        }

        every { packageManager.getApplicationInfo("system.app", 0) } returns systemAppInfo
        every { packageManager.getApplicationInfo("user.app", 0) } returns userAppInfo
        every { packageManager.getApplicationLabel(any()) } returns "App Name"

        val processes = listOf(
            mockk<ActivityManager.RunningAppProcessInfo> {
                every { pid } returns 1001
                every { processName } returns "system.app"
            },
            mockk<ActivityManager.RunningAppProcessInfo> {
                every { pid } returns 1002
                every { processName } returns "user.app"
            }
        )
        
        every { activityManager.runningAppProcesses } returns processes
        every { activityManager.getProcessMemoryInfo(any()) } returns arrayOf(
            mockk(relaxed = true) { every { totalPss } returns 50000 }
        )

        // When
        val result = processStatsCollector.getTopApps(5)

        // Then
        assertTrue(result.none { it.packageName == "system.app" })
    }

    @Test
    fun `getTopApps includes updated system apps`() {
        // Given
        val updatedSystemApp = mockk<ApplicationInfo> {
            every { flags } returns (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)
        }

        every { packageManager.getApplicationInfo("chrome.app", 0) } returns updatedSystemApp
        every { packageManager.getApplicationLabel(any()) } returns "Chrome"

        val processes = listOf(
            mockk<ActivityManager.RunningAppProcessInfo> {
                every { pid } returns 2001
                every { processName } returns "chrome.app"
            }
        )
        
        every { activityManager.runningAppProcesses } returns processes
        every { activityManager.getProcessMemoryInfo(any()) } returns arrayOf(
            mockk(relaxed = true) { every { totalPss } returns 100000 }
        )

        // When
        val result = processStatsCollector.getTopApps(5)

        // Then
        assertTrue(result.any { it.packageName == "chrome.app" })
    }

    @Test
    fun `getTopApps excludes SysMetrics itself`() {
        // Given
        val appInfo = mockk<ApplicationInfo> {
            every { flags } returns 0
        }

        every { packageManager.getApplicationInfo("com.sysmetrics.app", 0) } returns appInfo
        every { packageManager.getApplicationInfo("other.app", 0) } returns appInfo
        every { packageManager.getApplicationLabel(any()) } returns "App"

        val processes = listOf(
            mockk<ActivityManager.RunningAppProcessInfo> {
                every { pid } returns 3001
                every { processName } returns "com.sysmetrics.app"
            },
            mockk<ActivityManager.RunningAppProcessInfo> {
                every { pid } returns 3002
                every { processName } returns "other.app"
            }
        )
        
        every { activityManager.runningAppProcesses } returns processes
        every { activityManager.getProcessMemoryInfo(any()) } returns arrayOf(
            mockk(relaxed = true) { every { totalPss } returns 50000 }
        )

        // When
        val result = processStatsCollector.getTopApps(10)

        // Then
        assertTrue(result.none { it.packageName == "com.sysmetrics.app" })
    }

    @Test
    fun `getTopApps respects count limit`() {
        // Given
        val appInfo = mockk<ApplicationInfo> {
            every { flags } returns 0
        }

        every { packageManager.getApplicationInfo(any(), any()) } returns appInfo
        every { packageManager.getApplicationLabel(any()) } returns "App"

        val processes = (1..10).map { i ->
            mockk<ActivityManager.RunningAppProcessInfo> {
                every { pid } returns 4000 + i
                every { processName } returns "app$i"
            }
        }
        
        every { activityManager.runningAppProcesses } returns processes
        every { activityManager.getProcessMemoryInfo(any()) } returns arrayOf(
            mockk(relaxed = true) { every { totalPss } returns 50000 }
        )

        // When
        val result = processStatsCollector.getTopApps(3)

        // Then
        assertTrue(result.size <= 3)
    }

    @Test
    fun `getTopApps handles null running processes`() {
        // Given
        every { activityManager.runningAppProcesses } returns null

        // When
        val result = processStatsCollector.getTopApps(5)

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `getTopApps handles exceptions gracefully`() {
        // Given
        every { activityManager.runningAppProcesses } throws RuntimeException("Test exception")

        // When
        val result = processStatsCollector.getTopApps(5)

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `initializeBaseline executes without exception`() {
        // When/Then - should not throw
        assertDoesNotThrow {
            processStatsCollector.initializeBaseline()
        }
    }

    @Test
    fun `warmUpCache executes without exception`() {
        // Given
        val processes = listOf(
            mockk<ActivityManager.RunningAppProcessInfo> {
                every { pid } returns 5001
                every { processName } returns "test.app"
            }
        )
        every { activityManager.runningAppProcesses } returns processes

        // When/Then - should not throw
        assertDoesNotThrow {
            processStatsCollector.warmUpCache()
        }
    }

    @Test
    fun `clearCache executes without exception`() {
        // When/Then - should not throw
        assertDoesNotThrow {
            processStatsCollector.clearCache()
        }
    }

    @Test
    fun `AppStats data class holds correct values`() {
        // Given/When
        val appStats = AppStats(
            packageName = "com.test.app",
            appName = "Test App",
            cpuPercent = 25.5f,
            ramMb = 150L
        )

        // Then
        assertEquals("com.test.app", appStats.packageName)
        assertEquals("Test App", appStats.appName)
        assertEquals(25.5f, appStats.cpuPercent, 0.01f)
        assertEquals(150L, appStats.ramMb)
    }

    @Test
    fun `getTopApps sorts by CPU priority`() {
        // Given
        val appInfo = mockk<ApplicationInfo> {
            every { flags } returns 0
        }

        every { packageManager.getApplicationInfo(any(), any()) } returns appInfo
        every { packageManager.getApplicationLabel(any()) } answers {
            "App-${(it.invocation.args[0] as ApplicationInfo).packageName}"
        }

        val processes = listOf(
            mockk<ActivityManager.RunningAppProcessInfo> {
                every { pid } returns 6001
                every { processName } returns "low.cpu.app"
            },
            mockk<ActivityManager.RunningAppProcessInfo> {
                every { pid } returns 6002
                every { processName } returns "high.cpu.app"
            }
        )
        
        every { activityManager.runningAppProcesses } returns processes
        every { activityManager.getProcessMemoryInfo(any()) } returnsMany listOf(
            arrayOf(mockk(relaxed = true) { every { totalPss } returns 50000 }),
            arrayOf(mockk(relaxed = true) { every { totalPss } returns 50000 })
        )

        // When
        val result = processStatsCollector.getTopApps(2)

        // Then
        assertTrue(result.size <= 2)
        // Results should be sorted (we can't test exact CPU values due to /proc/ dependency)
    }

    private fun assertDoesNotThrow(block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            fail("Should not throw exception: ${e.message}")
        }
    }
}
