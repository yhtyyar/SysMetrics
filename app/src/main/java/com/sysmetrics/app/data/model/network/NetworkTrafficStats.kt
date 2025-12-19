package com.sysmetrics.app.data.model.network

import android.graphics.drawable.Drawable

/**
 * Network type enumeration for connection classification.
 * Supports all common Android network types.
 */
enum class NetworkTypeEnum {
    /** WiFi connection */
    WIFI,
    /** 4G LTE cellular */
    LTE,
    /** 5G cellular (NR) */
    FIVE_G,
    /** 3G cellular */
    THREE_G,
    /** 2G cellular (EDGE/GPRS) */
    TWO_G,
    /** Ethernet connection */
    ETHERNET,
    /** VPN tunnel */
    VPN,
    /** No network connection */
    NONE,
    /** Unknown network type */
    UNKNOWN;

    /**
     * User-friendly display name for the network type.
     */
    val displayName: String
        get() = when (this) {
            WIFI -> "WiFi"
            LTE -> "4G LTE"
            FIVE_G -> "5G"
            THREE_G -> "3G"
            TWO_G -> "2G"
            ETHERNET -> "Ethernet"
            VPN -> "VPN"
            NONE -> "No Connection"
            UNKNOWN -> "Unknown"
        }

    /**
     * Short display name for compact overlay.
     */
    val shortName: String
        get() = when (this) {
            WIFI -> "WiFi"
            LTE -> "LTE"
            FIVE_G -> "5G"
            THREE_G -> "3G"
            TWO_G -> "2G"
            ETHERNET -> "Eth"
            VPN -> "VPN"
            NONE -> "—"
            UNKNOWN -> "?"
        }
}

/**
 * Display mode for network overlay.
 */
enum class NetworkDisplayMode {
    /** Compact: ↓ 2.5M | ↑ 0.8M */
    COMPACT,
    /** Extended with peak values */
    EXTENDED,
    /** Per-app traffic breakdown */
    PER_APP,
    /** Combined with other system metrics */
    COMBINED
}

/**
 * Comprehensive network traffic statistics.
 * Tracks real-time speeds, peak values, and cumulative totals.
 *
 * @property ingressBytesPerSec Current download speed in bytes per second
 * @property egressBytesPerSec Current upload speed in bytes per second
 * @property ingressMbps Download speed formatted as Mbps
 * @property egressMbps Upload speed formatted as Mbps
 * @property peakIngressMbps Peak download speed since monitoring started
 * @property peakEgressMbps Peak upload speed since monitoring started
 * @property peakIngressTimestamp Timestamp when peak download occurred
 * @property peakEgressTimestamp Timestamp when peak upload occurred
 * @property totalIngressBytes Total bytes downloaded since device boot
 * @property totalEgressBytes Total bytes uploaded since device boot
 * @property sessionIngressBytes Bytes downloaded in current session
 * @property sessionEgressBytes Bytes uploaded in current session
 * @property timestamp Timestamp of this measurement
 * @property isAvailable Whether network monitoring is available
 */
data class NetworkTrafficStats(
    val ingressBytesPerSec: Long = 0L,
    val egressBytesPerSec: Long = 0L,
    val ingressMbps: Float = 0f,
    val egressMbps: Float = 0f,
    val peakIngressMbps: Float = 0f,
    val peakEgressMbps: Float = 0f,
    val peakIngressTimestamp: Long = 0L,
    val peakEgressTimestamp: Long = 0L,
    val totalIngressBytes: Long = 0L,
    val totalEgressBytes: Long = 0L,
    val sessionIngressBytes: Long = 0L,
    val sessionEgressBytes: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
    val isAvailable: Boolean = true
) {
    /**
     * Formats ingress speed for display.
     * Automatically selects appropriate unit (KB/s, MB/s, Gbps).
     */
    fun formatIngressSpeed(): String = formatSpeed(ingressBytesPerSec, "↓")

    /**
     * Formats egress speed for display.
     * Automatically selects appropriate unit (KB/s, MB/s, Gbps).
     */
    fun formatEgressSpeed(): String = formatSpeed(egressBytesPerSec, "↑")

    /**
     * Compact format: "↓ 2.5M | ↑ 0.8M"
     */
    fun formatCompact(): String = "${formatShortSpeed(ingressBytesPerSec, "↓")} | ${formatShortSpeed(egressBytesPerSec, "↑")}"

    /**
     * Extended format with peak values.
     */
    fun formatExtended(): String = buildString {
        appendLine("↓ Ingress: ${formatMbps(ingressMbps)} | Peak: ${formatMbps(peakIngressMbps)}")
        append("↑ Egress:  ${formatMbps(egressMbps)} | Peak: ${formatMbps(peakEgressMbps)}")
    }

    /**
     * Formats session totals for display.
     */
    fun formatSessionTotals(): String = buildString {
        append("↓ ${formatBytes(sessionIngressBytes)} | ↑ ${formatBytes(sessionEgressBytes)}")
    }

    private fun formatSpeed(bytesPerSec: Long, prefix: String): String {
        return when {
            bytesPerSec < 1024 -> "$prefix ${bytesPerSec} B/s"
            bytesPerSec < 1024 * 1024 -> "$prefix %.1f KB/s".format(bytesPerSec / 1024f)
            bytesPerSec < 1024 * 1024 * 1024 -> "$prefix %.2f MB/s".format(bytesPerSec / (1024f * 1024f))
            else -> "$prefix %.2f GB/s".format(bytesPerSec / (1024f * 1024f * 1024f))
        }
    }

    private fun formatShortSpeed(bytesPerSec: Long, prefix: String): String {
        return when {
            bytesPerSec < 1024 -> "$prefix ${bytesPerSec}B"
            bytesPerSec < 1024 * 1024 -> "$prefix %.0fK".format(bytesPerSec / 1024f)
            bytesPerSec < 1024 * 1024 * 1024 -> "$prefix %.1fM".format(bytesPerSec / (1024f * 1024f))
            else -> "$prefix %.1fG".format(bytesPerSec / (1024f * 1024f * 1024f))
        }
    }

    private fun formatMbps(mbps: Float): String {
        return when {
            mbps < 0.01f -> "0 Mbps"
            mbps < 1f -> "%.2f Mbps".format(mbps)
            mbps < 100f -> "%.1f Mbps".format(mbps)
            mbps < 1000f -> "%.0f Mbps".format(mbps)
            else -> "%.2f Gbps".format(mbps / 1000f)
        }
    }

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024f)
            bytes < 1024 * 1024 * 1024 -> "%.1f MB".format(bytes / (1024f * 1024f))
            else -> "%.2f GB".format(bytes / (1024f * 1024f * 1024f))
        }
    }

    companion object {
        val EMPTY = NetworkTrafficStats(isAvailable = false)

        /** Convert bytes per second to Mbps */
        fun bytesToMbps(bytesPerSec: Long): Float = bytesPerSec * 8f / (1024f * 1024f)

        /** Convert Mbps to bytes per second */
        fun mbpsToBytes(mbps: Float): Long = (mbps * 1024f * 1024f / 8f).toLong()
    }
}

/**
 * Network connection type and quality information.
 *
 * @property type Network type (WiFi, LTE, 5G, etc.)
 * @property networkName Network name (SSID for WiFi, carrier for cellular)
 * @property signalStrengthDbm Signal strength in dBm
 * @property signalLevel Normalized signal level (0-4)
 * @property linkSpeedMbps Link speed in Mbps (for WiFi)
 * @property isMetered Whether connection is metered
 * @property isRoaming Whether device is roaming
 * @property timestamp Measurement timestamp
 */
data class NetworkTypeInfo(
    val type: NetworkTypeEnum = NetworkTypeEnum.NONE,
    val networkName: String? = null,
    val signalStrengthDbm: Int? = null,
    val signalLevel: Int = 0,
    val linkSpeedMbps: Int? = null,
    val isMetered: Boolean = false,
    val isRoaming: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Formats network info for display.
     * Example: "WiFi: LimeHD-Office (-45 dBm)"
     */
    fun formatDisplay(): String = buildString {
        append(type.displayName)
        networkName?.let { append(": $it") }
        signalStrengthDbm?.let { append(" ($it dBm)") }
    }

    /**
     * Compact format for overlay.
     * Example: "WiFi: -45dBm"
     */
    fun formatCompact(): String = buildString {
        append(type.shortName)
        signalStrengthDbm?.let { append(": ${it}dBm") }
    }

    /**
     * Signal quality as percentage (0-100).
     */
    val signalQualityPercent: Int
        get() = (signalLevel * 25).coerceIn(0, 100)

    companion object {
        val DISCONNECTED = NetworkTypeInfo(type = NetworkTypeEnum.NONE)
    }
}

/**
 * Per-application network traffic statistics.
 *
 * @property uid Application UID
 * @property packageName Application package name
 * @property appName User-visible application name
 * @property appIcon Application icon (nullable to avoid memory issues)
 * @property ingressBytesPerSec Current download speed
 * @property egressBytesPerSec Current upload speed
 * @property totalIngressBytes Total bytes downloaded
 * @property totalEgressBytes Total bytes uploaded
 * @property sessionIngressBytes Session download total
 * @property sessionEgressBytes Session upload total
 * @property lastActiveTimestamp Last activity timestamp
 */
data class PerAppTrafficStats(
    val uid: Int,
    val packageName: String,
    val appName: String,
    val appIcon: Drawable? = null,
    val ingressBytesPerSec: Long = 0L,
    val egressBytesPerSec: Long = 0L,
    val totalIngressBytes: Long = 0L,
    val totalEgressBytes: Long = 0L,
    val sessionIngressBytes: Long = 0L,
    val sessionEgressBytes: Long = 0L,
    val lastActiveTimestamp: Long = System.currentTimeMillis()
) {
    /**
     * Total current speed (ingress + egress).
     */
    val totalSpeedBytesPerSec: Long
        get() = ingressBytesPerSec + egressBytesPerSec

    /**
     * Formats traffic for display.
     * Example: "YouTube - ↓1.5M ↑50K"
     */
    fun formatDisplay(): String = buildString {
        append(appName)
        append(" - ")
        append(formatShortSpeed(ingressBytesPerSec, "↓"))
        append(" ")
        append(formatShortSpeed(egressBytesPerSec, "↑"))
    }

    /**
     * Compact format for overlay.
     */
    fun formatCompact(): String = "${formatShortSpeed(ingressBytesPerSec, "↓")}${formatShortSpeed(egressBytesPerSec, "↑")}"

    private fun formatShortSpeed(bytesPerSec: Long, prefix: String): String {
        return when {
            bytesPerSec < 1024 -> "$prefix${bytesPerSec}B"
            bytesPerSec < 1024 * 1024 -> "$prefix%.0fK".format(bytesPerSec / 1024f)
            else -> "$prefix%.1fM".format(bytesPerSec / (1024f * 1024f))
        }
    }

    companion object {
        val EMPTY = PerAppTrafficStats(
            uid = -1,
            packageName = "",
            appName = "Unknown"
        )
    }
}

/**
 * Network interface statistics from /proc/net/dev.
 * Raw data before delta calculation.
 *
 * @property interfaceName Interface name (e.g., wlan0, eth0)
 * @property rxBytes Received bytes
 * @property rxPackets Received packets
 * @property rxErrors Receive errors
 * @property rxDropped Dropped received packets
 * @property txBytes Transmitted bytes
 * @property txPackets Transmitted packets
 * @property txErrors Transmit errors
 * @property txDropped Dropped transmitted packets
 * @property timestamp Measurement timestamp
 */
data class InterfaceStats(
    val interfaceName: String,
    val rxBytes: Long = 0L,
    val rxPackets: Long = 0L,
    val rxErrors: Long = 0L,
    val rxDropped: Long = 0L,
    val txBytes: Long = 0L,
    val txPackets: Long = 0L,
    val txErrors: Long = 0L,
    val txDropped: Long = 0L,
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Whether this is a loopback interface (should be ignored).
     */
    val isLoopback: Boolean
        get() = interfaceName == "lo"

    /**
     * Whether this interface has any traffic.
     */
    val hasTraffic: Boolean
        get() = rxBytes > 0 || txBytes > 0

    companion object {
        val EMPTY = InterfaceStats(interfaceName = "")
    }
}

/**
 * Aggregated snapshot of all network interfaces.
 * Used for delta calculation.
 *
 * @property interfaces Map of interface name to stats
 * @property totalRxBytes Total received bytes across all interfaces
 * @property totalTxBytes Total transmitted bytes across all interfaces
 * @property timestamp Snapshot timestamp
 */
data class NetworkSnapshot(
    val interfaces: Map<String, InterfaceStats> = emptyMap(),
    val totalRxBytes: Long = 0L,
    val totalTxBytes: Long = 0L,
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Active interfaces (non-loopback with traffic).
     */
    val activeInterfaces: List<InterfaceStats>
        get() = interfaces.values.filter { !it.isLoopback && it.hasTraffic }

    companion object {
        val EMPTY = NetworkSnapshot()
    }
}

/**
 * Network traffic alert configuration.
 *
 * @property enabled Whether alerts are enabled
 * @property highSpeedThresholdMbps Alert when speed exceeds this threshold
 * @property dailyQuotaMb Daily data quota in MB (0 = unlimited)
 * @property quotaWarningPercent Warn at this percentage of quota
 * @property anomalyDetectionEnabled Enable anomaly detection
 */
data class NetworkAlertConfig(
    val enabled: Boolean = false,
    val highSpeedThresholdMbps: Float = 100f,
    val dailyQuotaMb: Long = 0L,
    val quotaWarningPercent: Int = 80,
    val anomalyDetectionEnabled: Boolean = false
) {
    companion object {
        val DEFAULT = NetworkAlertConfig()
    }
}

/**
 * Network traffic alert event.
 *
 * @property type Alert type
 * @property message Human-readable message
 * @property currentValue Current value that triggered the alert
 * @property thresholdValue Threshold that was exceeded
 * @property timestamp Alert timestamp
 */
data class NetworkAlert(
    val type: AlertType,
    val message: String,
    val currentValue: Float,
    val thresholdValue: Float,
    val timestamp: Long = System.currentTimeMillis()
) {
    enum class AlertType {
        HIGH_SPEED,
        QUOTA_WARNING,
        QUOTA_EXCEEDED,
        ANOMALY_DETECTED
    }
}
