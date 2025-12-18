package com.sysmetrics.app.native_bridge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * Unit tests for FallbackStringFormatter.
 * Tests string formatting functionality when native code is unavailable.
 */
class FallbackStringFormatterTest {

    private val formatter = FallbackStringFormatter()

    @Test
    fun `formatTime with 24h format returns correct string`() {
        // Given
        val hour = 14
        val minute = 35
        val use24h = true

        // When
        val result = formatter.formatTime(hour, minute, use24h)

        // Then
        assertEquals("14:35", result)
    }

    @Test
    fun `formatTime with 12h format AM returns correct string`() {
        // Given
        val hour = 9
        val minute = 15
        val use24h = false

        // When
        val result = formatter.formatTime(hour, minute, use24h)

        // Then
        assertEquals("9:15 AM", result)
    }

    @Test
    fun `formatTime with 12h format PM returns correct string`() {
        // Given
        val hour = 15
        val minute = 45
        val use24h = false

        // When
        val result = formatter.formatTime(hour, minute, use24h)

        // Then
        assertEquals("3:45 PM", result)
    }

    @Test
    fun `formatTime with 12h format midnight returns correct string`() {
        // Given
        val hour = 0
        val minute = 0
        val use24h = false

        // When
        val result = formatter.formatTime(hour, minute, use24h)

        // Then
        assertEquals("12:00 AM", result)
    }

    @Test
    fun `formatTime with 12h format noon returns correct string`() {
        // Given
        val hour = 12
        val minute = 30
        val use24h = false

        // When
        val result = formatter.formatTime(hour, minute, use24h)

        // Then
        assertEquals("12:30 PM", result)
    }

    @Test
    fun `formatCpu returns correct string for integer percentage`() {
        // Given
        val cpuPercent = 45.0f

        // When
        val result = formatter.formatCpu(cpuPercent)

        // Then
        assertEquals("CPU: 45.0%", result)
    }

    @Test
    fun `formatCpu returns correct string for decimal percentage`() {
        // Given
        val cpuPercent = 12.5f

        // When
        val result = formatter.formatCpu(cpuPercent)

        // Then
        assertEquals("CPU: 12.5%", result)
    }

    @Test
    fun `formatRam returns correct string`() {
        // Given
        val usedMb = 1250L
        val totalMb = 4096L

        // When
        val result = formatter.formatRam(usedMb, totalMb)

        // Then
        assertEquals("RAM: 1250/4096 MB", result)
    }

    @Test
    fun `formatSelfStats returns correct string`() {
        // Given
        val cpuPercent = 8.5f
        val ramMb = 145L

        // When
        val result = formatter.formatSelfStats(cpuPercent, ramMb)

        // Then
        assertEquals("Self: 8.5% / 145M", result)
    }

    @Test
    fun `isNativeAvailable returns false for fallback implementation`() {
        // When
        val result = formatter.isNativeAvailable()

        // Then
        assertFalse(result)
    }
}
