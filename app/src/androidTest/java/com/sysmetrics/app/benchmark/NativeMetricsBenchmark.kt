package com.sysmetrics.app.benchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sysmetrics.app.native_bridge.NativeMetrics
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Benchmark tests for native C++ metrics collection.
 * 
 * These tests compare native vs Kotlin performance.
 * Expected improvement: ~5-10x faster for native implementation.
 */
@RunWith(AndroidJUnit4::class)
class NativeMetricsBenchmark {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @Before
    fun setup() {
        // Skip tests if native library is not available
        Assume.assumeTrue(
            "Native library not available",
            NativeMetrics.isNativeAvailable()
        )
    }

    /**
     * Benchmark native CPU usage collection.
     * Target: < 0.05ms (50 microseconds)
     */
    @Test
    fun benchmarkNativeCpuUsage() {
        // Reset baseline first
        NativeMetrics.resetCpuBaselineNative()
        
        benchmarkRule.measureRepeated {
            NativeMetrics.getCpuUsageNative()
        }
    }

    /**
     * Benchmark native memory stats collection.
     * Target: < 0.1ms (100 microseconds)
     */
    @Test
    fun benchmarkNativeMemoryStats() {
        benchmarkRule.measureRepeated {
            NativeMetrics.getMemoryStatsNative()
        }
    }

    /**
     * Benchmark native temperature reading.
     * Target: < 0.05ms (50 microseconds)
     */
    @Test
    fun benchmarkNativeTemperature() {
        benchmarkRule.measureRepeated {
            NativeMetrics.getTemperatureNative()
        }
    }

    /**
     * Benchmark native CPU core count.
     * Target: < 0.02ms (20 microseconds)
     */
    @Test
    fun benchmarkNativeCpuCoreCount() {
        benchmarkRule.measureRepeated {
            NativeMetrics.getCpuCoreCountNative()
        }
    }

    /**
     * Benchmark complete native metrics collection.
     * This simulates a full metrics collection cycle using native code.
     */
    @Test
    fun benchmarkFullNativeCollection() {
        NativeMetrics.resetCpuBaselineNative()
        
        benchmarkRule.measureRepeated {
            NativeMetrics.getCpuUsageNative()
            NativeMetrics.getMemoryStatsNative()
            NativeMetrics.getTemperatureNative()
            NativeMetrics.getCpuCoreCountNative()
        }
    }
}
