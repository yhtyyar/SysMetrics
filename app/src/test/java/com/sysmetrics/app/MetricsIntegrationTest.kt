package com.sysmetrics.app

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.sysmetrics.app.native_bridge.FallbackCpuMetricsCollector
import com.sysmetrics.app.native_bridge.FallbackStringFormatter
import com.sysmetrics.app.native_bridge.MetricsCollectorFactory
import com.sysmetrics.app.native_bridge.NativeCpuMetricsCollector
import com.sysmetrics.app.native_bridge.NativeStringFormatter
import org.junit.Rule
import org.junit.Test

/**
 * Integration tests for the metrics collection system.
 * Tests that all components work together correctly.
 */
class MetricsIntegrationTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Test
    fun `factory creates working collectors in integration scenario`() {
        // Given - simulate native libraries not available
        val nativeCpuCollector = NativeCpuMetricsCollector()
        val nativeFormatter = NativeStringFormatter()
        val fallbackCpuCollector = FallbackCpuMetricsCollector()
        val fallbackFormatter = FallbackStringFormatter()

        val factory = MetricsCollectorFactory(
            nativeCpuCollector,
            fallbackCpuCollector,
            nativeFormatter,
            fallbackFormatter
        )

        // When
        val cpuCollector = factory.createCpuCollector()
        val stringFormatter = factory.createStringFormatter()

        // Then - should work regardless of native availability
        val cpuUsage = cpuCollector.getCpuUsage()
        val coreCount = cpuCollector.getCoreCount()

        val timeString = stringFormatter.formatTime(14, 35, true)
        val cpuString = stringFormatter.formatCpu(25.5f)
        val ramString = stringFormatter.formatRam(1024, 4096)
        val selfStatsString = stringFormatter.formatSelfStats(15.2f, 89)

        // Verify outputs are reasonable
        assert(timeString.isNotEmpty())
        assert(cpuString.contains("25.5"))
        assert(ramString.contains("1024"))
        assert(ramString.contains("4096"))
        assert(selfStatsString.contains("15.2"))
        assert(selfStatsString.contains("89"))

        println("Integration test passed:")
        println("CPU Usage: $cpuUsage")
        println("Core Count: $coreCount")
        println("Time: $timeString")
        println("CPU: $cpuString")
        println("RAM: $ramString")
        println("Self: $selfStatsString")
    }
}
