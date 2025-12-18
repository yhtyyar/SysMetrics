package com.sysmetrics.app.utils

import com.sysmetrics.app.core.common.Constants
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for AppStats data class.
 * Tests calculation logic and data integrity.
 */
class AppStatsTest {

    @Test
    fun `combinedScore calculates correctly for high CPU low RAM`() {
        // Given
        val appStats = AppStats(
            packageName = "com.example.app",
            appName = "Test App",
            cpuPercent = 20.0f,
            ramMb = 50L
        )

        // When
        val combinedScore = appStats.combinedScore

        // Then
        val expected = (20.0f * Constants.ProcessMonitoring.CPU_SCORE_WEIGHT) +
                      (50L / Constants.ProcessMonitoring.RAM_SCORE_WEIGHT_DIVISOR.toFloat())
        assertEquals(expected, combinedScore, 0.001f)
    }

    @Test
    fun `combinedScore calculates correctly for low CPU high RAM`() {
        // Given
        val appStats = AppStats(
            packageName = "com.example.app2",
            appName = "Test App 2",
            cpuPercent = 5.0f,
            ramMb = 200L
        )

        // When
        val combinedScore = appStats.combinedScore

        // Then
        val expected = (5.0f * Constants.ProcessMonitoring.CPU_SCORE_WEIGHT) +
                      (200L / Constants.ProcessMonitoring.RAM_SCORE_WEIGHT_DIVISOR.toFloat())
        assertEquals(expected, combinedScore, 0.001f)
    }

    @Test
    fun `combinedScore calculates correctly for balanced usage`() {
        // Given
        val appStats = AppStats(
            packageName = "com.example.app3",
            appName = "Test App 3",
            cpuPercent = 10.0f,
            ramMb = 100L
        )

        // When
        val combinedScore = appStats.combinedScore

        // Then
        val expected = (10.0f * Constants.ProcessMonitoring.CPU_SCORE_WEIGHT) +
                      (100L / Constants.ProcessMonitoring.RAM_SCORE_WEIGHT_DIVISOR.toFloat())
        assertEquals(expected, combinedScore, 0.001f)
    }

    @Test
    fun `AppStats constructor stores values correctly`() {
        // Given
        val packageName = "com.test.app"
        val appName = "Test App"
        val cpuPercent = 15.5f
        val ramMb = 75L

        // When
        val appStats = AppStats(packageName, appName, cpuPercent, ramMb)

        // Then
        assertEquals(packageName, appStats.packageName)
        assertEquals(appName, appStats.appName)
        assertEquals(cpuPercent, appStats.cpuPercent)
        assertEquals(ramMb, appStats.ramMb)
    }

    @Test
    fun `copy creates new instance with modified values`() {
        // Given
        val original = AppStats("com.test.app", "Test App", 10.0f, 50L)

        // When
        val copied = original.copy(cpuPercent = 15.0f, ramMb = 75L)

        // Then
        assertEquals("com.test.app", copied.packageName)
        assertEquals("Test App", copied.appName)
        assertEquals(15.0f, copied.cpuPercent)
        assertEquals(75L, copied.ramMb)
    }
}
