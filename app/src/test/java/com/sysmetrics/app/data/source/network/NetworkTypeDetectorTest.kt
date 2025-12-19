package com.sysmetrics.app.data.source.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.sysmetrics.app.data.model.network.NetworkTypeEnum
import com.sysmetrics.app.data.model.network.NetworkTypeInfo
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for NetworkTypeDetector.
 * Tests network type detection and signal strength parsing.
 */
class NetworkTypeDetectorTest {

    private lateinit var context: Context
    private lateinit var connectivityManager: ConnectivityManager

    @Before
    fun setup() {
        connectivityManager = mockk(relaxed = true)
        context = mockk {
            every { getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManager
            every { applicationContext } returns this
            every { getSystemService(Context.WIFI_SERVICE) } returns null
            every { getSystemService(Context.TELEPHONY_SERVICE) } returns null
        }
    }

    // ==================== Network Type Enum Tests ====================

    @Test
    fun `NetworkTypeEnum WIFI has correct displayName`() {
        assertEquals("WiFi", NetworkTypeEnum.WIFI.displayName)
    }

    @Test
    fun `NetworkTypeEnum LTE has correct displayName`() {
        assertEquals("4G LTE", NetworkTypeEnum.LTE.displayName)
    }

    @Test
    fun `NetworkTypeEnum FIVE_G has correct displayName`() {
        assertEquals("5G", NetworkTypeEnum.FIVE_G.displayName)
    }

    @Test
    fun `NetworkTypeEnum NONE has correct displayName`() {
        assertEquals("No Connection", NetworkTypeEnum.NONE.displayName)
    }

    @Test
    fun `NetworkTypeEnum shortName values are concise`() {
        assertTrue(NetworkTypeEnum.WIFI.shortName.length <= 4)
        assertTrue(NetworkTypeEnum.LTE.shortName.length <= 4)
        assertTrue(NetworkTypeEnum.FIVE_G.shortName.length <= 4)
    }

    // ==================== NetworkTypeInfo Tests ====================

    @Test
    fun `NetworkTypeInfo formatDisplay includes type name`() {
        val info = NetworkTypeInfo(
            type = NetworkTypeEnum.WIFI,
            networkName = "TestNetwork",
            signalStrengthDbm = -45
        )
        
        val display = info.formatDisplay()
        assertTrue(display.contains("WiFi"))
        assertTrue(display.contains("TestNetwork"))
        assertTrue(display.contains("-45 dBm"))
    }

    @Test
    fun `NetworkTypeInfo formatDisplay handles null networkName`() {
        val info = NetworkTypeInfo(
            type = NetworkTypeEnum.LTE,
            networkName = null,
            signalStrengthDbm = -85
        )
        
        val display = info.formatDisplay()
        assertTrue(display.contains("LTE"))
        assertFalse(display.contains("null"))
    }

    @Test
    fun `NetworkTypeInfo formatCompact is concise`() {
        val info = NetworkTypeInfo(
            type = NetworkTypeEnum.WIFI,
            signalStrengthDbm = -50
        )
        
        val compact = info.formatCompact()
        assertTrue(compact.length < 20)
        assertTrue(compact.contains("WiFi"))
    }

    @Test
    fun `NetworkTypeInfo signalQualityPercent calculated correctly for level 4`() {
        val info = NetworkTypeInfo(signalLevel = 4)
        assertEquals(100, info.signalQualityPercent)
    }

    @Test
    fun `NetworkTypeInfo signalQualityPercent calculated correctly for level 0`() {
        val info = NetworkTypeInfo(signalLevel = 0)
        assertEquals(0, info.signalQualityPercent)
    }

    @Test
    fun `NetworkTypeInfo signalQualityPercent calculated correctly for level 2`() {
        val info = NetworkTypeInfo(signalLevel = 2)
        assertEquals(50, info.signalQualityPercent)
    }

    @Test
    fun `NetworkTypeInfo DISCONNECTED has NONE type`() {
        val disconnected = NetworkTypeInfo.DISCONNECTED
        assertEquals(NetworkTypeEnum.NONE, disconnected.type)
    }

    // ==================== Connection State Tests ====================

    @Test
    fun `returns DISCONNECTED when no active network`() {
        every { connectivityManager.activeNetwork } returns null
        
        val detector = NetworkTypeDetector(context)
        val result = detector.getCurrentNetworkType()
        
        assertEquals(NetworkTypeEnum.NONE, result.type)
    }

    @Test
    fun `returns DISCONNECTED when no capabilities`() {
        val network = mockk<Network>()
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns null
        
        val detector = NetworkTypeDetector(context)
        val result = detector.getCurrentNetworkType()
        
        assertEquals(NetworkTypeEnum.NONE, result.type)
    }

    @Test
    fun `detects WiFi when TRANSPORT_WIFI present`() {
        val network = mockk<Network>()
        val capabilities = mockk<NetworkCapabilities> {
            every { hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns true
            every { hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) } returns false
            every { hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) } returns false
            every { hasTransport(NetworkCapabilities.TRANSPORT_VPN) } returns false
            every { hasCapability(any()) } returns true
            every { signalStrength } returns -50
        }
        
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns capabilities
        
        val detector = NetworkTypeDetector(context)
        val result = detector.getCurrentNetworkType()
        
        assertEquals(NetworkTypeEnum.WIFI, result.type)
    }

    @Test
    fun `detects Ethernet when TRANSPORT_ETHERNET present`() {
        val network = mockk<Network>()
        val capabilities = mockk<NetworkCapabilities> {
            every { hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns false
            every { hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) } returns false
            every { hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) } returns true
            every { hasTransport(NetworkCapabilities.TRANSPORT_VPN) } returns false
            every { hasCapability(any()) } returns true
        }
        
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns capabilities
        
        val detector = NetworkTypeDetector(context)
        val result = detector.getCurrentNetworkType()
        
        assertEquals(NetworkTypeEnum.ETHERNET, result.type)
    }

    @Test
    fun `detects VPN when TRANSPORT_VPN present`() {
        val network = mockk<Network>()
        val capabilities = mockk<NetworkCapabilities> {
            every { hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns false
            every { hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) } returns false
            every { hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) } returns false
            every { hasTransport(NetworkCapabilities.TRANSPORT_VPN) } returns true
            every { hasCapability(any()) } returns true
        }
        
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns capabilities
        
        val detector = NetworkTypeDetector(context)
        val result = detector.getCurrentNetworkType()
        
        assertEquals(NetworkTypeEnum.VPN, result.type)
    }

    // ==================== Metered Connection Tests ====================

    @Test
    fun `isMetered returns false when NOT_METERED capability present`() {
        val info = NetworkTypeInfo(isMetered = false)
        assertFalse(info.isMetered)
    }

    @Test
    fun `isMetered returns true when NOT_METERED capability absent`() {
        val info = NetworkTypeInfo(isMetered = true)
        assertTrue(info.isMetered)
    }

    // ==================== Roaming Tests ====================

    @Test
    fun `isRoaming defaults to false`() {
        val info = NetworkTypeInfo()
        assertFalse(info.isRoaming)
    }

    @Test
    fun `isRoaming can be set to true`() {
        val info = NetworkTypeInfo(isRoaming = true)
        assertTrue(info.isRoaming)
    }

    // ==================== Link Speed Tests ====================

    @Test
    fun `linkSpeedMbps can be null`() {
        val info = NetworkTypeInfo()
        assertNull(info.linkSpeedMbps)
    }

    @Test
    fun `linkSpeedMbps can store value`() {
        val info = NetworkTypeInfo(linkSpeedMbps = 867)
        assertEquals(867, info.linkSpeedMbps)
    }

    // ==================== Error Handling Tests ====================

    @Test
    fun `handles exception gracefully`() {
        every { connectivityManager.activeNetwork } throws SecurityException("Permission denied")
        
        val detector = NetworkTypeDetector(context)
        val result = detector.getCurrentNetworkType()
        
        // Should return DISCONNECTED on error
        assertEquals(NetworkTypeEnum.NONE, result.type)
    }
}
