package com.sysmetrics.app.benchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sysmetrics.app.core.di.DefaultDispatcherProvider
import com.sysmetrics.app.data.source.network.NetworkStatsDataSource
import com.sysmetrics.app.data.source.network.NetworkTypeDetector
import com.sysmetrics.app.data.source.network.PerAppTrafficDataSource
import com.sysmetrics.app.native_bridge.NativeNetworkMetrics
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Performance benchmarks for network traffic monitoring.
 * Compares Kotlin vs C++ implementations.
 *
 * ## Running Benchmarks:
 * ```
 * ./gradlew :app:connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.sysmetrics.app.benchmark.NetworkStatsBenchmarkTest
 * ```
 *
 * ## Target Metrics:
 * - Native parsing: <10ms per cycle
 * - Kotlin parsing: <100ms per cycle
 * - Native should be ~10x faster
 */
@RunWith(AndroidJUnit4::class)
class NetworkStatsBenchmarkTest {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    private lateinit var kotlinDataSource: NetworkStatsDataSource
    private lateinit var nativeMetrics: NativeNetworkMetrics
    private lateinit var networkTypeDetector: NetworkTypeDetector
    private lateinit var perAppDataSource: PerAppTrafficDataSource

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dispatcherProvider = DefaultDispatcherProvider()

        kotlinDataSource = NetworkStatsDataSource(dispatcherProvider)
        nativeMetrics = NativeNetworkMetrics()
        networkTypeDetector = NetworkTypeDetector(context)
        perAppDataSource = PerAppTrafficDataSource(context, dispatcherProvider)
    }

    // ==================== Native vs Kotlin Parsing ====================

    /**
     * Benchmark: Native C++ /proc/net/dev parsing.
     * Target: <10ms per iteration
     */
    @Test
    fun benchmarkNativeNetworkStatsParsing() {
        benchmarkRule.measureRepeated {
            nativeMetrics.getNetworkStats()
        }
    }

    /**
     * Benchmark: Kotlin /proc/net/dev parsing.
     * Target: <100ms per iteration
     */
    @Test
    fun benchmarkKotlinNetworkStatsParsing() {
        benchmarkRule.measureRepeated {
            runBlocking {
                kotlinDataSource.readNetworkStats()
            }
        }
    }

    // ==================== Individual Operations ====================

    /**
     * Benchmark: Native getTotalRxBytes.
     */
    @Test
    fun benchmarkNativeGetTotalRxBytes() {
        benchmarkRule.measureRepeated {
            nativeMetrics.getTotalRxBytes()
        }
    }

    /**
     * Benchmark: Native getTotalTxBytes.
     */
    @Test
    fun benchmarkNativeGetTotalTxBytes() {
        benchmarkRule.measureRepeated {
            nativeMetrics.getTotalTxBytes()
        }
    }

    /**
     * Benchmark: Native interface count.
     */
    @Test
    fun benchmarkNativeGetInterfaceCount() {
        benchmarkRule.measureRepeated {
            nativeMetrics.getInterfaceCount()
        }
    }

    // ==================== Network Type Detection ====================

    /**
     * Benchmark: Network type detection.
     */
    @Test
    fun benchmarkNetworkTypeDetection() {
        benchmarkRule.measureRepeated {
            networkTypeDetector.getCurrentNetworkType()
        }
    }

    /**
     * Benchmark: isConnected check.
     */
    @Test
    fun benchmarkIsConnectedCheck() {
        benchmarkRule.measureRepeated {
            networkTypeDetector.isConnected()
        }
    }

    /**
     * Benchmark: isMetered check.
     */
    @Test
    fun benchmarkIsMeteredCheck() {
        benchmarkRule.measureRepeated {
            networkTypeDetector.isMetered()
        }
    }

    // ==================== Per-App Traffic ====================

    /**
     * Benchmark: Per-app traffic stats (top 5).
     */
    @Test
    fun benchmarkPerAppStatsTop5() {
        benchmarkRule.measureRepeated {
            runBlocking {
                perAppDataSource.getPerAppStats(5)
            }
        }
    }

    /**
     * Benchmark: Per-app traffic stats (top 10).
     */
    @Test
    fun benchmarkPerAppStatsTop10() {
        benchmarkRule.measureRepeated {
            runBlocking {
                perAppDataSource.getPerAppStats(10)
            }
        }
    }

    // ==================== String Formatting ====================

    /**
     * Benchmark: Native speed formatting.
     */
    @Test
    fun benchmarkNativeFormatSpeed() {
        val bytesPerSec = 1024L * 1024L // 1 MB/s
        benchmarkRule.measureRepeated {
            nativeMetrics.formatSpeed(bytesPerSec, "↓")
        }
    }

    // ==================== Full Cycle ====================

    /**
     * Benchmark: Full monitoring cycle (native).
     * Includes: parsing + type detection
     */
    @Test
    fun benchmarkFullCycleNative() {
        benchmarkRule.measureRepeated {
            nativeMetrics.getNetworkStats()
            networkTypeDetector.getCurrentNetworkType()
        }
    }

    /**
     * Benchmark: Full monitoring cycle (Kotlin).
     * Includes: parsing + type detection
     */
    @Test
    fun benchmarkFullCycleKotlin() {
        benchmarkRule.measureRepeated {
            runBlocking {
                kotlinDataSource.readNetworkStats()
            }
            networkTypeDetector.getCurrentNetworkType()
        }
    }

    /**
     * Benchmark: Full monitoring cycle with per-app stats.
     */
    @Test
    fun benchmarkFullCycleWithPerApp() {
        benchmarkRule.measureRepeated {
            runBlocking {
                nativeMetrics.getNetworkStats()
                networkTypeDetector.getCurrentNetworkType()
                perAppDataSource.getPerAppStats(5)
            }
        }
    }

    // ==================== Memory Allocation ====================

    /**
     * Benchmark: Repeated native calls (memory stability).
     * Runs 100 iterations to check for memory leaks.
     */
    @Test
    fun benchmarkNativeMemoryStability() {
        benchmarkRule.measureRepeated {
            repeat(100) {
                nativeMetrics.getNetworkStats()
            }
        }
    }

    /**
     * Benchmark: Repeated Kotlin calls (memory stability).
     */
    @Test
    fun benchmarkKotlinMemoryStability() {
        benchmarkRule.measureRepeated {
            runBlocking {
                repeat(100) {
                    kotlinDataSource.readNetworkStats()
                }
            }
        }
    }
}
