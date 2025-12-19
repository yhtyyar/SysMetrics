package com.sysmetrics.app.data.source.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.SignalStrength
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import com.sysmetrics.app.data.model.network.NetworkTypeEnum
import com.sysmetrics.app.data.model.network.NetworkTypeInfo
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import timber.log.Timber

/**
 * Detects and monitors network connection type and quality.
 * Uses ConnectivityManager for network type detection and
 * TelephonyManager/WifiManager for signal strength.
 *
 * ## Supported Network Types:
 * - WiFi (with SSID and signal strength)
 * - LTE/4G (with carrier name and signal dBm)
 * - 5G NR (with carrier and signal)
 * - 3G/2G (legacy cellular)
 * - Ethernet
 * - VPN
 *
 * @param context Application context for system services
 */
class NetworkTypeDetector(private val context: Context) {

    companion object {
        private const val TAG = "NET_TYPE_DETECT"
    }

    private val connectivityManager: ConnectivityManager by lazy {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    private val wifiManager: WifiManager? by lazy {
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    }

    private val telephonyManager: TelephonyManager? by lazy {
        context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
    }

    @Volatile
    private var currentSignalStrengthDbm: Int? = null

    /**
     * Gets current network type information synchronously.
     *
     * @return [NetworkTypeInfo] with connection details
     */
    fun getCurrentNetworkType(): NetworkTypeInfo {
        return try {
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)

            if (network == null || capabilities == null) {
                Timber.tag(TAG).d("No active network")
                return NetworkTypeInfo.DISCONNECTED
            }

            detectNetworkType(capabilities)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error detecting network type")
            NetworkTypeInfo.DISCONNECTED
        }
    }

    /**
     * Observes network type changes as a Flow.
     * Emits new values when network type or quality changes.
     *
     * @return Flow of [NetworkTypeInfo]
     */
    fun observeNetworkType(): Flow<NetworkTypeInfo> = callbackFlow {
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Timber.tag(TAG).d("Network available: $network")
                trySend(getCurrentNetworkType())
            }

            override fun onLost(network: Network) {
                Timber.tag(TAG).d("Network lost: $network")
                trySend(NetworkTypeInfo.DISCONNECTED)
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                Timber.tag(TAG).v("Network capabilities changed")
                trySend(detectNetworkType(networkCapabilities))
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        try {
            connectivityManager.registerNetworkCallback(request, networkCallback)
            Timber.tag(TAG).d("Network callback registered")

            // Emit initial state
            trySend(getCurrentNetworkType())
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error registering network callback")
        }

        awaitClose {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback)
                Timber.tag(TAG).d("Network callback unregistered")
            } catch (e: Exception) {
                Timber.tag(TAG).w(e, "Error unregistering network callback")
            }
        }
    }.distinctUntilChanged()

    /**
     * Detects network type from capabilities.
     */
    private fun detectNetworkType(capabilities: NetworkCapabilities): NetworkTypeInfo {
        val type = when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                NetworkTypeEnum.WIFI
            }
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                detectCellularGeneration()
            }
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
                NetworkTypeEnum.ETHERNET
            }
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> {
                NetworkTypeEnum.VPN
            }
            else -> NetworkTypeEnum.UNKNOWN
        }

        return buildNetworkTypeInfo(type, capabilities)
    }

    /**
     * Detects cellular network generation (2G/3G/4G/5G).
     */
    private fun detectCellularGeneration(): NetworkTypeEnum {
        return try {
            val networkType = telephonyManager?.dataNetworkType ?: return NetworkTypeEnum.UNKNOWN

            when (networkType) {
                // 5G
                TelephonyManager.NETWORK_TYPE_NR -> NetworkTypeEnum.FIVE_G

                // 4G LTE
                TelephonyManager.NETWORK_TYPE_LTE,
                TelephonyManager.NETWORK_TYPE_IWLAN -> NetworkTypeEnum.LTE

                // 3G
                TelephonyManager.NETWORK_TYPE_UMTS,
                TelephonyManager.NETWORK_TYPE_EVDO_0,
                TelephonyManager.NETWORK_TYPE_EVDO_A,
                TelephonyManager.NETWORK_TYPE_HSDPA,
                TelephonyManager.NETWORK_TYPE_HSUPA,
                TelephonyManager.NETWORK_TYPE_HSPA,
                TelephonyManager.NETWORK_TYPE_EVDO_B,
                TelephonyManager.NETWORK_TYPE_EHRPD,
                TelephonyManager.NETWORK_TYPE_HSPAP,
                TelephonyManager.NETWORK_TYPE_TD_SCDMA -> NetworkTypeEnum.THREE_G

                // 2G
                TelephonyManager.NETWORK_TYPE_GPRS,
                TelephonyManager.NETWORK_TYPE_EDGE,
                TelephonyManager.NETWORK_TYPE_CDMA,
                TelephonyManager.NETWORK_TYPE_1xRTT,
                TelephonyManager.NETWORK_TYPE_IDEN,
                TelephonyManager.NETWORK_TYPE_GSM -> NetworkTypeEnum.TWO_G

                else -> NetworkTypeEnum.UNKNOWN
            }
        } catch (e: SecurityException) {
            Timber.tag(TAG).w(e, "No permission to read phone state")
            NetworkTypeEnum.LTE // Default to LTE if permission denied
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error detecting cellular generation")
            NetworkTypeEnum.UNKNOWN
        }
    }

    /**
     * Builds complete NetworkTypeInfo with signal strength and network name.
     */
    private fun buildNetworkTypeInfo(
        type: NetworkTypeEnum,
        capabilities: NetworkCapabilities
    ): NetworkTypeInfo {
        val timestamp = System.currentTimeMillis()

        return when (type) {
            NetworkTypeEnum.WIFI -> buildWifiInfo(capabilities, timestamp)
            NetworkTypeEnum.LTE, NetworkTypeEnum.FIVE_G, 
            NetworkTypeEnum.THREE_G, NetworkTypeEnum.TWO_G -> buildCellularInfo(type, capabilities, timestamp)
            NetworkTypeEnum.ETHERNET -> NetworkTypeInfo(
                type = type,
                networkName = "Ethernet",
                signalLevel = 4, // Assume full signal for wired
                isMetered = false,
                timestamp = timestamp
            )
            NetworkTypeEnum.VPN -> NetworkTypeInfo(
                type = type,
                networkName = "VPN",
                signalLevel = 4,
                timestamp = timestamp
            )
            else -> NetworkTypeInfo(
                type = type,
                timestamp = timestamp
            )
        }
    }

    /**
     * Builds WiFi-specific network info.
     */
    @Suppress("DEPRECATION")
    private fun buildWifiInfo(
        capabilities: NetworkCapabilities,
        timestamp: Long
    ): NetworkTypeInfo {
        var ssid: String? = null
        var signalDbm: Int? = null
        var signalLevel = 0
        var linkSpeed: Int? = null

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ uses NetworkCapabilities for WiFi info
                signalDbm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    capabilities.signalStrength
                } else null
            }

            // Get SSID and link speed from WifiManager
            wifiManager?.connectionInfo?.let { wifiInfo ->
                ssid = wifiInfo.ssid?.removeSurrounding("\"")
                    ?.takeIf { it != WifiManager.UNKNOWN_SSID && it != "<unknown ssid>" }

                if (signalDbm == null) {
                    signalDbm = wifiInfo.rssi
                }
                linkSpeed = wifiInfo.linkSpeed

                signalLevel = WifiManager.calculateSignalLevel(
                    wifiInfo.rssi,
                    5 // 5 levels (0-4)
                )
            }
        } catch (e: SecurityException) {
            Timber.tag(TAG).w(e, "No permission to read WiFi info")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error reading WiFi info")
        }

        val isMetered = !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)

        return NetworkTypeInfo(
            type = NetworkTypeEnum.WIFI,
            networkName = ssid,
            signalStrengthDbm = signalDbm,
            signalLevel = signalLevel,
            linkSpeedMbps = linkSpeed,
            isMetered = isMetered,
            timestamp = timestamp
        )
    }

    /**
     * Builds cellular-specific network info.
     */
    private fun buildCellularInfo(
        type: NetworkTypeEnum,
        capabilities: NetworkCapabilities,
        timestamp: Long
    ): NetworkTypeInfo {
        var carrierName: String? = null
        var signalDbm: Int? = currentSignalStrengthDbm
        var signalLevel = 0
        var isRoaming = false

        try {
            telephonyManager?.let { tm ->
                carrierName = tm.networkOperatorName?.takeIf { it.isNotBlank() }
                isRoaming = tm.isNetworkRoaming

                // Calculate signal level (0-4)
                signalLevel = when {
                    signalDbm == null -> 2 // Default to medium
                    signalDbm!! >= -70 -> 4 // Excellent
                    signalDbm!! >= -85 -> 3 // Good
                    signalDbm!! >= -100 -> 2 // Fair
                    signalDbm!! >= -110 -> 1 // Poor
                    else -> 0 // No signal
                }
            }
        } catch (e: SecurityException) {
            Timber.tag(TAG).w(e, "No permission to read cellular info")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error reading cellular info")
        }

        val isMetered = !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)

        return NetworkTypeInfo(
            type = type,
            networkName = carrierName,
            signalStrengthDbm = signalDbm,
            signalLevel = signalLevel,
            isMetered = isMetered,
            isRoaming = isRoaming,
            timestamp = timestamp
        )
    }

    /**
     * Starts listening for signal strength updates.
     * Call from a lifecycle-aware component.
     */
    @Suppress("DEPRECATION")
    fun startSignalStrengthListener() {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                val listener = object : PhoneStateListener() {
                    @Deprecated("Deprecated in Java")
                    override fun onSignalStrengthsChanged(signalStrength: SignalStrength?) {
                        signalStrength?.let {
                            currentSignalStrengthDbm = extractSignalDbm(it)
                            Timber.tag(TAG).v("Signal strength updated: $currentSignalStrengthDbm dBm")
                        }
                    }
                }
                telephonyManager?.listen(listener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS)
                Timber.tag(TAG).d("Signal strength listener started")
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error starting signal strength listener")
        }
    }

    /**
     * Extracts signal strength in dBm from SignalStrength object.
     */
    @Suppress("DEPRECATION")
    private fun extractSignalDbm(signalStrength: SignalStrength): Int? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Get the strongest signal among all cell info
                signalStrength.cellSignalStrengths
                    .mapNotNull { it.dbm.takeIf { dbm -> dbm != Int.MAX_VALUE } }
                    .maxOrNull()
            } else {
                // Legacy approach
                val gsmSignal = signalStrength.gsmSignalStrength
                if (gsmSignal != 99) {
                    // Convert ASU to dBm: dBm = 2 * ASU - 113
                    2 * gsmSignal - 113
                } else null
            }
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Error extracting signal dBm")
            null
        }
    }

    /**
     * Checks if device is connected to internet.
     */
    fun isConnected(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * Checks if current connection is metered.
     */
    fun isMetered(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
    }

    /**
     * Gets estimated downstream bandwidth in Kbps.
     */
    fun getDownstreamBandwidthKbps(): Int {
        val network = connectivityManager.activeNetwork ?: return 0
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return 0
        return capabilities.linkDownstreamBandwidthKbps
    }

    /**
     * Gets estimated upstream bandwidth in Kbps.
     */
    fun getUpstreamBandwidthKbps(): Int {
        val network = connectivityManager.activeNetwork ?: return 0
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return 0
        return capabilities.linkUpstreamBandwidthKbps
    }
}
