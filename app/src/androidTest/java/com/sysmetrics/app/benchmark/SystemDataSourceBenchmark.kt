package com.sysmetrics.app.benchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sysmetrics.app.core.di.DefaultDispatcherProvider
import com.sysmetrics.app.data.source.SystemDataSource
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Benchmark tests for SystemDataSource file I/O operations.
 * 
 * These tests measure actual file reading performance on device.
 * Results may vary based on device storage speed.
 */
@RunWith(AndroidJUnit4::class)
class SystemDataSourceBenchmark {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    private lateinit var dataSource: SystemDataSource

    @Before
    fun setup() {
        dataSource = SystemDataSource(DefaultDispatcherProvider())
    }

    /**
     * Benchmark reading /proc/stat file.
     * Target: < 1ms per read
     */
    @Test
    fun benchmarkReadCpuStats() {
        benchmarkRule.measureRepeated {
            runBlocking {
                dataSource.readCpuStats()
            }
        }
    }

    /**
     * Benchmark reading /proc/meminfo file.
     * Target: < 2ms per read
     */
    @Test
    fun benchmarkReadMemoryInfo() {
        // Clear cache to ensure actual file read
        dataSource.clearCache()
        
        benchmarkRule.measureRepeated {
            runBlocking {
                dataSource.readMemoryInfo()
            }
        }
    }

    /**
     * Benchmark reading /proc/meminfo with caching enabled.
     * Target: < 0.1ms when cached
     */
    @Test
    fun benchmarkReadMemoryInfoCached() {
        // Warm up cache
        runBlocking {
            dataSource.readMemoryInfo()
        }
        
        benchmarkRule.measureRepeated {
            runBlocking {
                dataSource.readMemoryInfo()
            }
        }
    }

    /**
     * Benchmark reading temperature from thermal zones.
     * Target: < 3ms per read (multiple file access)
     */
    @Test
    fun benchmarkReadTemperature() {
        dataSource.clearCache()
        
        benchmarkRule.measureRepeated {
            runBlocking {
                dataSource.readTemperature()
            }
        }
    }

    /**
     * Benchmark complete data collection cycle.
     */
    @Test
    fun benchmarkFullDataCollection() {
        dataSource.clearCache()
        
        benchmarkRule.measureRepeated {
            runBlocking {
                dataSource.readCpuStats()
                dataSource.readMemoryInfo()
                dataSource.readTemperature()
                dataSource.getCpuCoreCount()
            }
        }
    }
}
