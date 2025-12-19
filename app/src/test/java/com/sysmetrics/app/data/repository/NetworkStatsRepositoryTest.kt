package com.sysmetrics.app.data.repository

import android.content.Context
import app.cash.turbine.test
import com.sysmetrics.app.data.model.network.NetworkAlertConfig
import com.sysmetrics.app.data.model.network.NetworkTrafficStats
import com.sysmetrics.app.data.model.network.NetworkTypeEnum
import com.sysmetrics.app.data.model.network.NetworkTypeInfo
import com.sysmetrics.app.data.model.network.PerAppTrafficStats
import com.sysmetrics.app.data.source.network.NetworkStatsDataSource
import com.sysmetrics.app.data.source.network.NetworkTypeDetector
import com.sysmetrics.app.data.source.network.PerAppTrafficDataSource
import com.sysmetrics.app.native_bridge.NativeNetworkMetrics
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

/**
 * Unit tests for NetworkStatsRepository.
 * Tests business logic, flow composition, and caching.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NetworkStatsRepositoryTest {

    private lateinit var context: Context
    private lateinit var networkStatsDataSource: NetworkStatsDataSource
    private lateinit var networkTypeDetector: NetworkTypeDetector
    private lateinit var perAppTrafficDataSource: PerAppTrafficDataSource
    private lateinit var nativeNetworkMetrics: NativeNetworkMetrics

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        networkStatsDataSource = mockk(relaxed = true)
        networkTypeDetector = mockk(relaxed = true)
        perAppTrafficDataSource = mockk(relaxed = true)
        nativeNetworkMetrics = mockk(relaxed = true)
    }

    // ==================== getNetworkStats Tests ====================

    @Test
    fun `getNetworkStats uses native when available`() = runTest {
        val expectedStats = NetworkTrafficStats(
            ingressBytesPerSec = 1024,
            egressBytesPerSec = 512,
            isAvailable = true
        )

        every { nativeNetworkMetrics.isNativeAvailable() } returns true
        every { nativeNetworkMetrics.getNetworkStats() } returns expectedStats

        val repository = createRepository()
        val result = repository.getNetworkStats()

        assertEquals(expectedStats, result)
        verify { nativeNetworkMetrics.getNetworkStats() }
    }

    @Test
    fun `getNetworkStats falls back to Kotlin when native unavailable`() = runTest {
        val expectedStats = NetworkTrafficStats(
            ingressBytesPerSec = 2048,
            egressBytesPerSec = 1024,
            isAvailable = true
        )

        every { nativeNetworkMetrics.isNativeAvailable() } returns false
        coEvery { networkStatsDataSource.readNetworkStats() } returns expectedStats

        val repository = createRepository()
        val result = repository.getNetworkStats()

        assertEquals(expectedStats, result)
    }

    @Test
    fun `getNetworkStats returns EMPTY on error`() = runTest {
        every { nativeNetworkMetrics.isNativeAvailable() } returns true
        every { nativeNetworkMetrics.getNetworkStats() } throws RuntimeException("Test error")

        val repository = createRepository()
        val result = repository.getNetworkStats()

        assertEquals(NetworkTrafficStats.EMPTY, result)
    }

    // ==================== getNetworkType Tests ====================

    @Test
    fun `getNetworkType returns detector result`() = runTest {
        val expectedType = NetworkTypeInfo(
            type = NetworkTypeEnum.WIFI,
            networkName = "TestNetwork",
            signalStrengthDbm = -45
        )

        every { networkTypeDetector.getCurrentNetworkType() } returns expectedType

        val repository = createRepository()
        val result = repository.getNetworkType()

        assertEquals(expectedType, result)
    }

    @Test
    fun `getNetworkType returns DISCONNECTED on error`() = runTest {
        every { networkTypeDetector.getCurrentNetworkType() } throws RuntimeException("Test error")

        val repository = createRepository()
        val result = repository.getNetworkType()

        assertEquals(NetworkTypeInfo.DISCONNECTED, result)
    }

    // ==================== getPerAppStats Tests ====================

    @Test
    fun `getPerAppStats returns list from datasource`() = runTest {
        val expectedStats = listOf(
            PerAppTrafficStats(uid = 1000, packageName = "com.test1", appName = "Test1"),
            PerAppTrafficStats(uid = 1001, packageName = "com.test2", appName = "Test2")
        )

        coEvery { perAppTrafficDataSource.getPerAppStats(any()) } returns expectedStats

        val repository = createRepository()
        val result = repository.getPerAppStats(5)

        assertEquals(expectedStats, result)
        assertEquals(2, result.size)
    }

    @Test
    fun `getPerAppStats returns empty list on error`() = runTest {
        coEvery { perAppTrafficDataSource.getPerAppStats(any()) } throws RuntimeException("Test error")

        val repository = createRepository()
        val result = repository.getPerAppStats()

        assertTrue(result.isEmpty())
    }

    // ==================== Peak Values Tests ====================

    @Test
    fun `getPeakValues uses native when available`() {
        val expectedPeaks = Pair(25.5f, 12.3f)
        
        every { nativeNetworkMetrics.isNativeAvailable() } returns true
        every { nativeNetworkMetrics.getPeakValues() } returns expectedPeaks

        val repository = createRepository()
        val result = repository.getPeakValues()

        assertEquals(expectedPeaks, result)
    }

    @Test
    fun `getPeakValues falls back to dataSource when native unavailable`() {
        val expectedPeaks = Pair(30.0f, 15.0f)
        
        every { nativeNetworkMetrics.isNativeAvailable() } returns false
        every { networkStatsDataSource.getPeakValues() } returns expectedPeaks

        val repository = createRepository()
        val result = repository.getPeakValues()

        assertEquals(expectedPeaks, result)
    }

    // ==================== Baseline Reset Tests ====================

    @Test
    fun `resetBaseline resets all sources`() {
        val repository = createRepository()
        repository.resetBaseline()

        verify { nativeNetworkMetrics.resetBaseline() }
        verify { networkStatsDataSource.resetBaseline() }
        verify { perAppTrafficDataSource.resetBaseline() }
    }

    // ==================== Availability Tests ====================

    @Test
    fun `isNativeAvailable returns native status`() {
        every { nativeNetworkMetrics.isNativeAvailable() } returns true

        val repository = createRepository()
        assertTrue(repository.isNativeAvailable())
    }

    @Test
    fun `isMonitoringAvailable returns true when native available`() {
        every { nativeNetworkMetrics.isNativeAvailable() } returns true
        every { networkStatsDataSource.isAvailable() } returns false

        val repository = createRepository()
        assertTrue(repository.isMonitoringAvailable())
    }

    @Test
    fun `isMonitoringAvailable returns true when dataSource available`() {
        every { nativeNetworkMetrics.isNativeAvailable() } returns false
        every { networkStatsDataSource.isAvailable() } returns true

        val repository = createRepository()
        assertTrue(repository.isMonitoringAvailable())
    }

    @Test
    fun `isMonitoringAvailable returns false when nothing available`() {
        every { nativeNetworkMetrics.isNativeAvailable() } returns false
        every { networkStatsDataSource.isAvailable() } returns false

        val repository = createRepository()
        assertFalse(repository.isMonitoringAvailable())
    }

    // ==================== Flow Tests ====================

    @Test
    fun `observeNetworkType emits from detector flow`() = runTest {
        val types = listOf(
            NetworkTypeInfo(type = NetworkTypeEnum.WIFI),
            NetworkTypeInfo(type = NetworkTypeEnum.LTE),
            NetworkTypeInfo(type = NetworkTypeEnum.NONE)
        )

        every { networkTypeDetector.observeNetworkType() } returns flowOf(*types.toTypedArray())

        val repository = createRepository()

        repository.observeNetworkType().test(timeout = 5.seconds) {
            assertEquals(NetworkTypeEnum.WIFI, awaitItem().type)
            assertEquals(NetworkTypeEnum.LTE, awaitItem().type)
            assertEquals(NetworkTypeEnum.NONE, awaitItem().type)
            awaitComplete()
        }
    }

    // ==================== Alert Config Tests ====================

    @Test
    fun `NetworkAlertConfig DEFAULT has expected values`() {
        val config = NetworkAlertConfig.DEFAULT

        assertFalse(config.enabled)
        assertEquals(100f, config.highSpeedThresholdMbps, 0.01f)
        assertEquals(0L, config.dailyQuotaMb)
        assertEquals(80, config.quotaWarningPercent)
        assertFalse(config.anomalyDetectionEnabled)
    }

    @Test
    fun `NetworkAlertConfig can be customized`() {
        val config = NetworkAlertConfig(
            enabled = true,
            highSpeedThresholdMbps = 50f,
            dailyQuotaMb = 1000L,
            quotaWarningPercent = 90,
            anomalyDetectionEnabled = true
        )

        assertTrue(config.enabled)
        assertEquals(50f, config.highSpeedThresholdMbps, 0.01f)
        assertEquals(1000L, config.dailyQuotaMb)
        assertEquals(90, config.quotaWarningPercent)
        assertTrue(config.anomalyDetectionEnabled)
    }

    // ==================== Helper Methods ====================

    private fun createRepository(): NetworkStatsRepository {
        return NetworkStatsRepository(
            context = context,
            networkStatsDataSource = networkStatsDataSource,
            networkTypeDetector = networkTypeDetector,
            perAppTrafficDataSource = perAppTrafficDataSource,
            nativeNetworkMetrics = nativeNetworkMetrics
        )
    }
}
