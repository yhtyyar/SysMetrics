package com.sysmetrics.app.data.model

/**
 * GPU metrics information.
 * Supports multiple GPU vendors: Qualcomm Adreno, ARM Mali, and generic GPUs.
 */
data class GpuInfo(
    val usagePercent: Float = 0f,
    val frequencyMhz: Int = 0,
    val temperatureCelsius: Float = 0f,
    val vendor: GpuVendor = GpuVendor.UNKNOWN,
    val isAvailable: Boolean = false
) {
    companion object {
        val EMPTY = GpuInfo()
        val UNAVAILABLE = GpuInfo(isAvailable = false)
    }
}

/**
 * GPU vendor types for device-specific monitoring.
 */
enum class GpuVendor(val displayName: String) {
    ADRENO("Adreno"),        // Qualcomm
    MALI("Mali"),            // ARM
    POWERVR("PowerVR"),      // Imagination
    GENERIC("Generic"),      // Fallback
    UNKNOWN("Unknown")
}
