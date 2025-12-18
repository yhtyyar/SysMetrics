package com.sysmetrics.app.native_bridge

import com.sysmetrics.app.domain.collector.ICpuMetricsCollector
import com.sysmetrics.app.domain.formatter.IStringFormatter
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for MetricsCollectorFactory.
 * Tests factory logic for selecting native vs fallback implementations.
 */
class MetricsCollectorFactoryTest {

    private lateinit var factory: MetricsCollectorFactory

    @Test
    fun `createCpuCollector returns native when available`() {
        // Given
        val nativeCollector = mockk<NativeCpuMetricsCollector>()
        every { nativeCollector.isNativeAvailable() } returns true

        val fallbackCollector = mockk<FallbackCpuMetricsCollector>()
        factory = MetricsCollectorFactory(nativeCollector, fallbackCollector, mockk(), mockk())

        // When
        val result = factory.createCpuCollector()

        // Then
        assertTrue(result is NativeCpuMetricsCollector)
    }

    @Test
    fun `createCpuCollector returns fallback when native unavailable`() {
        // Given
        val nativeCollector = mockk<NativeCpuMetricsCollector>()
        every { nativeCollector.isNativeAvailable() } returns false

        val fallbackCollector = mockk<FallbackCpuMetricsCollector>()
        factory = MetricsCollectorFactory(nativeCollector, fallbackCollector, mockk(), mockk())

        // When
        val result = factory.createCpuCollector()

        // Then
        assertTrue(result is FallbackCpuMetricsCollector)
    }

    @Test
    fun `createStringFormatter returns native when available`() {
        // Given
        val nativeFormatter = mockk<NativeStringFormatter>()
        every { nativeFormatter.isNativeAvailable() } returns true

        val fallbackFormatter = mockk<FallbackStringFormatter>()
        factory = MetricsCollectorFactory(mockk(), mockk(), nativeFormatter, fallbackFormatter)

        // When
        val result = factory.createStringFormatter()

        // Then
        assertTrue(result is NativeStringFormatter)
    }

    @Test
    fun `createStringFormatter returns fallback when native unavailable`() {
        // Given
        val nativeFormatter = mockk<NativeStringFormatter>()
        every { nativeFormatter.isNativeAvailable() } returns false

        val fallbackFormatter = mockk<FallbackStringFormatter>()
        factory = MetricsCollectorFactory(mockk(), mockk(), nativeFormatter, fallbackFormatter)

        // When
        val result = factory.createStringFormatter()

        // Then
        assertTrue(result is FallbackStringFormatter)
    }
}
