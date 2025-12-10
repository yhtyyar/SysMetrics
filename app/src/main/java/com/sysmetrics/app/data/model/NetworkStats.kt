package com.sysmetrics.app.data.model

/**
 * Network traffic statistics.
 * Tracks download/upload speeds and total data transferred.
 */
data class NetworkStats(
    val downloadSpeedKbps: Float = 0f,
    val uploadSpeedKbps: Float = 0f,
    val totalDownloadMb: Float = 0f,
    val totalUploadMb: Float = 0f,
    val isAvailable: Boolean = true
) {
    /**
     * Formats download speed for display.
     */
    fun formatDownloadSpeed(): String {
        return when {
            downloadSpeedKbps < 1024 -> "↓ %.1f KB/s".format(downloadSpeedKbps)
            else -> "↓ %.2f MB/s".format(downloadSpeedKbps / 1024)
        }
    }

    /**
     * Formats upload speed for display.
     */
    fun formatUploadSpeed(): String {
        return when {
            uploadSpeedKbps < 1024 -> "↑ %.1f KB/s".format(uploadSpeedKbps)
            else -> "↑ %.2f MB/s".format(uploadSpeedKbps / 1024)
        }
    }

    companion object {
        val EMPTY = NetworkStats()
    }
}
