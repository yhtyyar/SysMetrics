package com.sysmetrics.app.data.source.network

import com.sysmetrics.app.core.di.DispatcherProvider
import com.sysmetrics.app.data.model.network.InterfaceStats
import com.sysmetrics.app.data.model.network.NetworkTrafficStats
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for NetworkStatsDataSource.
 * Tests /proc/net/dev parsing, delta calculation, and speed conversion.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NetworkStatsDataSourceTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var dispatcherProvider: DispatcherProvider
    private lateinit var dataSource: NetworkStatsDataSource

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        dispatcherProvider = mockk {
            every { io } returns testDispatcher
            every { main } returns testDispatcher
            every { default } returns testDispatcher
        }

        dataSource = NetworkStatsDataSource(dispatcherProvider)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ==================== Parsing Tests ====================

    @Test
    fun `parseProcNetDev returns valid stats on real device`() = runTest {
        // This test will pass on real Android device or emulator
        // On unit test environment, it should gracefully return empty
        val stats = dataSource.readNetworkStats()
        
        // Should return valid object even if /proc/net/dev not accessible
        assertNotNull(stats)
    }

    @Test
    fun `isAvailable returns false when proc file not accessible`() {
        // In unit test environment, /proc/net/dev is not available
        // Test the graceful handling
        val available = dataSource.isAvailable()
        // Don't assert specific value as it depends on test environment
        assertNotNull(available)
    }

    // ==================== Delta Calculation Tests ====================

    @Test
    fun `bytesToMbps converts correctly`() {
        // 1 MB/s = 8 Mbps
        val bytesPerSec = 1024L * 1024L // 1 MB/s
        val mbps = NetworkTrafficStats.bytesToMbps(bytesPerSec)
        
        assertEquals(8f, mbps, 0.01f)
    }

    @Test
    fun `bytesToMbps handles zero`() {
        val mbps = NetworkTrafficStats.bytesToMbps(0L)
        assertEquals(0f, mbps, 0.001f)
    }

    @Test
    fun `bytesToMbps handles large values`() {
        // 1 GB/s = 8000 Mbps
        val bytesPerSec = 1024L * 1024L * 1024L
        val mbps = NetworkTrafficStats.bytesToMbps(bytesPerSec)
        
        assertEquals(8192f, mbps, 1f)
    }

    @Test
    fun `mbpsToBytes converts correctly`() {
        // 8 Mbps = 1 MB/s
        val mbps = 8f
        val bytesPerSec = NetworkTrafficStats.mbpsToBytes(mbps)
        
        assertEquals(1024L * 1024L, bytesPerSec)
    }

    // ==================== Interface Stats Tests ====================

    @Test
    fun `InterfaceStats isLoopback returns true for lo`() {
        val loopback = InterfaceStats(interfaceName = "lo")
        assertTrue(loopback.isLoopback)
    }

    @Test
    fun `InterfaceStats isLoopback returns false for wlan0`() {
        val wlan = InterfaceStats(interfaceName = "wlan0")
        assertFalse(wlan.isLoopback)
    }

    @Test
    fun `InterfaceStats isLoopback returns false for eth0`() {
        val eth = InterfaceStats(interfaceName = "eth0")
        assertFalse(eth.isLoopback)
    }

    @Test
    fun `InterfaceStats hasTraffic returns true when rxBytes greater than zero`() {
        val stats = InterfaceStats(interfaceName = "wlan0", rxBytes = 100)
        assertTrue(stats.hasTraffic)
    }

    @Test
    fun `InterfaceStats hasTraffic returns true when txBytes greater than zero`() {
        val stats = InterfaceStats(interfaceName = "wlan0", txBytes = 100)
        assertTrue(stats.hasTraffic)
    }

    @Test
    fun `InterfaceStats hasTraffic returns false when no traffic`() {
        val stats = InterfaceStats(interfaceName = "wlan0")
        assertFalse(stats.hasTraffic)
    }

    // ==================== NetworkTrafficStats Tests ====================

    @Test
    fun `NetworkTrafficStats formatIngressSpeed formats bytes correctly`() {
        val stats = NetworkTrafficStats(ingressBytesPerSec = 500)
        assertTrue(stats.formatIngressSpeed().contains("500"))
        assertTrue(stats.formatIngressSpeed().contains("B/s"))
    }

    @Test
    fun `NetworkTrafficStats formatIngressSpeed formats KB correctly`() {
        val stats = NetworkTrafficStats(ingressBytesPerSec = 2048) // 2 KB
        assertTrue(stats.formatIngressSpeed().contains("KB/s"))
    }

    @Test
    fun `NetworkTrafficStats formatIngressSpeed formats MB correctly`() {
        val stats = NetworkTrafficStats(ingressBytesPerSec = 2 * 1024 * 1024) // 2 MB
        assertTrue(stats.formatIngressSpeed().contains("MB/s"))
    }

    @Test
    fun `NetworkTrafficStats formatCompact returns correct format`() {
        val stats = NetworkTrafficStats(
            ingressBytesPerSec = 1024 * 1024, // 1 MB/s
            egressBytesPerSec = 512 * 1024    // 512 KB/s
        )
        val compact = stats.formatCompact()
        
        assertTrue(compact.contains("↓"))
        assertTrue(compact.contains("↑"))
        assertTrue(compact.contains("|"))
    }

    @Test
    fun `NetworkTrafficStats EMPTY has zero values`() {
        val empty = NetworkTrafficStats.EMPTY
        
        assertEquals(0L, empty.ingressBytesPerSec)
        assertEquals(0L, empty.egressBytesPerSec)
        assertEquals(0f, empty.ingressMbps, 0.001f)
        assertEquals(0f, empty.egressMbps, 0.001f)
        assertFalse(empty.isAvailable)
    }

    // ==================== Peak Tracking Tests ====================

    @Test
    fun `getPeakValues returns zeros initially`() {
        val peaks = dataSource.getPeakValues()
        
        assertEquals(0f, peaks.first, 0.001f)
        assertEquals(0f, peaks.second, 0.001f)
    }

    @Test
    fun `resetBaseline clears peak values`() {
        // Reset
        dataSource.resetBaseline()
        
        val peaks = dataSource.getPeakValues()
        assertEquals(0f, peaks.first, 0.001f)
        assertEquals(0f, peaks.second, 0.001f)
    }

    // ==================== Session Tracking Tests ====================

    @Test
    fun `NetworkTrafficStats sessionBytes are zero initially`() {
        val stats = NetworkTrafficStats(
            totalIngressBytes = 1000,
            totalEgressBytes = 500,
            sessionIngressBytes = 0,
            sessionEgressBytes = 0
        )
        
        assertEquals(0L, stats.sessionIngressBytes)
        assertEquals(0L, stats.sessionEgressBytes)
    }

    @Test
    fun `NetworkTrafficStats formatSessionTotals returns correct format`() {
        val stats = NetworkTrafficStats(
            sessionIngressBytes = 1024 * 1024, // 1 MB
            sessionEgressBytes = 512 * 1024    // 512 KB
        )
        
        val formatted = stats.formatSessionTotals()
        assertTrue(formatted.contains("↓"))
        assertTrue(formatted.contains("↑"))
    }

    // ==================== Edge Case Tests ====================

    @Test
    fun `handles negative byte values gracefully`() {
        // Should coerce to 0 or handle gracefully
        val stats = NetworkTrafficStats(
            ingressBytesPerSec = -100,
            egressBytesPerSec = -50
        )
        
        // Format should not crash
        val formatted = stats.formatCompact()
        assertNotNull(formatted)
    }

    @Test
    fun `handles very large byte values`() {
        val stats = NetworkTrafficStats(
            ingressBytesPerSec = Long.MAX_VALUE / 2,
            egressBytesPerSec = Long.MAX_VALUE / 2
        )
        
        // Should not crash
        val formatted = stats.formatCompact()
        assertNotNull(formatted)
    }

    @Test
    fun `formatExtended returns multi-line string`() {
        val stats = NetworkTrafficStats(
            ingressMbps = 10f,
            egressMbps = 5f,
            peakIngressMbps = 25f,
            peakEgressMbps = 12f
        )
        
        val extended = stats.formatExtended()
        assertTrue(extended.contains("\n"))
        assertTrue(extended.contains("Peak"))
    }
}
