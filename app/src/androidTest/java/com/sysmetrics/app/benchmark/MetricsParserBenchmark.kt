package com.sysmetrics.app.benchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sysmetrics.app.data.model.CpuStats
import com.sysmetrics.app.data.source.MetricsParser
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Benchmark tests for MetricsParser performance validation.
 * 
 * Expected performance targets:
 * - CPU parsing: < 0.1ms per operation
 * - Memory parsing: < 0.5ms per operation
 * - CPU calculation: < 0.01ms per operation
 */
@RunWith(AndroidJUnit4::class)
class MetricsParserBenchmark {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    // Sample data for benchmarking
    private val sampleCpuLine = "cpu  10132153 290696 3084719 46828483 16683 0 25195 0 0 0"
    
    private val sampleMemInfo = """
        MemTotal:        8097000 kB
        MemFree:         2345678 kB
        MemAvailable:    3456789 kB
        Buffers:         123456 kB
        Cached:          234567 kB
        SwapCached:      12345 kB
        Active:          2345678 kB
        Inactive:        1234567 kB
        Active(anon):    1234567 kB
        Inactive(anon):  123456 kB
        Active(file):    1111111 kB
        Inactive(file):  1111110 kB
        Unevictable:     0 kB
        Mlocked:         0 kB
        SwapTotal:       2097148 kB
        SwapFree:        2097148 kB
        Dirty:           1234 kB
        Writeback:       0 kB
        AnonPages:       1234567 kB
        Mapped:          234567 kB
        Shmem:           12345 kB
        Slab:            234567 kB
    """.trimIndent()

    private val prevStats = CpuStats(
        user = 10000000, nice = 290000, system = 3000000,
        idle = 46000000, iowait = 16000, irq = 0, softirq = 25000
    )

    private val currStats = CpuStats(
        user = 10132153, nice = 290696, system = 3084719,
        idle = 46828483, iowait = 16683, irq = 0, softirq = 25195
    )

    /**
     * Benchmark CPU stats parsing from /proc/stat line.
     * Target: < 0.1ms (100 microseconds)
     */
    @Test
    fun benchmarkParseCpuStats() {
        benchmarkRule.measureRepeated {
            MetricsParser.parseCpuStats(sampleCpuLine)
        }
    }

    /**
     * Benchmark memory info parsing from /proc/meminfo content.
     * Target: < 0.5ms (500 microseconds)
     */
    @Test
    fun benchmarkParseMemInfo() {
        benchmarkRule.measureRepeated {
            MetricsParser.parseMemoryInfo(sampleMemInfo)
        }
    }

    /**
     * Benchmark CPU usage calculation between two snapshots.
     * Target: < 0.01ms (10 microseconds)
     */
    @Test
    fun benchmarkCalculateCpuUsage() {
        benchmarkRule.measureRepeated {
            MetricsParser.calculateCpuUsage(prevStats, currStats)
        }
    }

    /**
     * Benchmark temperature parsing.
     * Target: < 0.01ms (10 microseconds)
     */
    @Test
    fun benchmarkParseTemperature() {
        benchmarkRule.measureRepeated {
            MetricsParser.parseTemperature("65000")
        }
    }

    /**
     * Benchmark complete metrics parsing pipeline.
     * This simulates a full metrics collection cycle.
     */
    @Test
    fun benchmarkFullParsingPipeline() {
        benchmarkRule.measureRepeated {
            // Parse CPU
            val cpuStats = MetricsParser.parseCpuStats(sampleCpuLine)
            
            // Calculate usage
            MetricsParser.calculateCpuUsage(prevStats, cpuStats)
            
            // Parse memory
            MetricsParser.parseMemoryInfo(sampleMemInfo)
            
            // Parse temperature
            MetricsParser.parseTemperature("65000")
        }
    }
}
