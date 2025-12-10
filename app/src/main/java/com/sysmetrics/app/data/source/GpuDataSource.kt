package com.sysmetrics.app.data.source

import com.sysmetrics.app.core.di.DispatcherProvider
import com.sysmetrics.app.data.model.GpuInfo
import com.sysmetrics.app.data.model.GpuVendor
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data source for GPU metrics.
 * Supports multiple GPU vendors with fallback mechanisms.
 * 
 * Vendor-specific paths:
 * - Qualcomm Adreno: /sys/class/kgsl/kgsl-3d0/
 * - ARM Mali: /sys/devices/platform/mali/
 * - Generic: /sys/kernel/debug/dri/0/
 */
@Singleton
class GpuDataSource @Inject constructor(
    private val dispatcherProvider: DispatcherProvider
) {
    companion object {
        private const val TAG = "GPU_DATA"
        
        // Qualcomm Adreno paths
        private const val ADRENO_BUSY_PATH = "/sys/class/kgsl/kgsl-3d0/gpubusy"
        private const val ADRENO_FREQ_PATH = "/sys/class/kgsl/kgsl-3d0/gpuclk"
        private const val ADRENO_TEMP_PATH = "/sys/class/kgsl/kgsl-3d0/temp"
        
        // ARM Mali paths
        private const val MALI_UTIL_PATH = "/sys/devices/platform/mali/utilization"
        private const val MALI_FREQ_PATH = "/sys/devices/platform/mali/clock"
        
        // Generic paths
        private const val GENERIC_GPU_PATH = "/sys/kernel/debug/dri/0/gpu_usage"
        
        // Thermal zones that might contain GPU temperature
        private const val GPU_THERMAL_PATH = "/sys/class/thermal/"
        
        // Cache duration
        private const val CACHE_DURATION_MS = 500L
    }

    private var cachedGpuInfo: GpuInfo? = null
    private var cacheTimestamp: Long = 0L
    private var detectedVendor: GpuVendor? = null

    /**
     * Reads GPU usage and temperature.
     * Uses vendor-specific methods with fallbacks.
     */
    suspend fun readGpuInfo(): GpuInfo = withContext(dispatcherProvider.io) {
        val now = System.currentTimeMillis()
        
        // Return cached value if still valid
        cachedGpuInfo?.let { cached ->
            if (now - cacheTimestamp < CACHE_DURATION_MS) {
                return@withContext cached
            }
        }
        
        try {
            // Detect vendor on first call
            if (detectedVendor == null) {
                detectedVendor = detectGpuVendor()
                Timber.tag(TAG).i("Detected GPU vendor: ${detectedVendor?.displayName}")
            }
            
            val gpuInfo = when (detectedVendor) {
                GpuVendor.ADRENO -> readAdrenoGpu()
                GpuVendor.MALI -> readMaliGpu()
                else -> readGenericGpu()
            }
            
            // Update cache
            cachedGpuInfo = gpuInfo
            cacheTimestamp = now
            
            if (gpuInfo.isAvailable) {
                Timber.tag(TAG).v("GPU: %.1f%%, Freq: %d MHz, Temp: %.1f°C",
                    gpuInfo.usagePercent, gpuInfo.frequencyMhz, gpuInfo.temperatureCelsius)
            }
            
            gpuInfo
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error reading GPU info")
            GpuInfo.UNAVAILABLE
        }
    }

    /**
     * Detects GPU vendor based on available paths.
     */
    private fun detectGpuVendor(): GpuVendor {
        return when {
            File(ADRENO_BUSY_PATH).exists() -> GpuVendor.ADRENO
            File(MALI_UTIL_PATH).exists() -> GpuVendor.MALI
            File(GENERIC_GPU_PATH).exists() -> GpuVendor.GENERIC
            else -> {
                Timber.tag(TAG).w("No GPU monitoring paths found")
                GpuVendor.UNKNOWN
            }
        }
    }

    /**
     * Reads Qualcomm Adreno GPU metrics.
     * Format: "busy_time total_time"
     */
    private fun readAdrenoGpu(): GpuInfo {
        try {
            val busyFile = File(ADRENO_BUSY_PATH)
            if (!busyFile.exists() || !busyFile.canRead()) {
                return GpuInfo.UNAVAILABLE
            }

            val busyData = busyFile.readText().trim().split(" ")
            if (busyData.size >= 2) {
                val busyTime = busyData[0].toLongOrNull() ?: 0L
                val totalTime = busyData[1].toLongOrNull() ?: 1L
                
                val usagePercent = if (totalTime > 0) {
                    (busyTime.toFloat() / totalTime.toFloat() * 100f).coerceIn(0f, 100f)
                } else 0f

                // Read frequency
                val frequency = File(ADRENO_FREQ_PATH).takeIf { it.exists() && it.canRead() }
                    ?.readText()?.trim()?.toLongOrNull()?.let { it / 1_000_000 }?.toInt() ?: 0

                // Read temperature
                val temperature = File(ADRENO_TEMP_PATH).takeIf { it.exists() && it.canRead() }
                    ?.readText()?.trim()?.toFloatOrNull() ?: 0f

                return GpuInfo(
                    usagePercent = usagePercent,
                    frequencyMhz = frequency,
                    temperatureCelsius = temperature,
                    vendor = GpuVendor.ADRENO,
                    isAvailable = true
                )
            }
        } catch (e: Exception) {
            Timber.tag(TAG).d(e, "Failed to read Adreno GPU")
        }
        
        return GpuInfo.UNAVAILABLE
    }

    /**
     * Reads ARM Mali GPU metrics.
     * Format: "utilization_percent" (0-100)
     */
    private fun readMaliGpu(): GpuInfo {
        try {
            val utilFile = File(MALI_UTIL_PATH)
            if (!utilFile.exists() || !utilFile.canRead()) {
                return GpuInfo.UNAVAILABLE
            }

            val utilization = utilFile.readText().trim().toFloatOrNull() ?: 0f

            // Read frequency
            val frequency = File(MALI_FREQ_PATH).takeIf { it.exists() && it.canRead() }
                ?.readText()?.trim()?.toLongOrNull()?.let { it / 1_000_000 }?.toInt() ?: 0

            // Try to find GPU temperature from thermal zones
            val temperature = findGpuTemperature()

            return GpuInfo(
                usagePercent = utilization.coerceIn(0f, 100f),
                frequencyMhz = frequency,
                temperatureCelsius = temperature,
                vendor = GpuVendor.MALI,
                isAvailable = true
            )
        } catch (e: Exception) {
            Timber.tag(TAG).d(e, "Failed to read Mali GPU")
        }
        
        return GpuInfo.UNAVAILABLE
    }

    /**
     * Generic GPU reading (fallback).
     * Provides estimated usage if possible.
     */
    private fun readGenericGpu(): GpuInfo {
        try {
            val gpuFile = File(GENERIC_GPU_PATH)
            if (gpuFile.exists() && gpuFile.canRead()) {
                val usage = gpuFile.readText().trim().toFloatOrNull() ?: 0f
                
                return GpuInfo(
                    usagePercent = usage.coerceIn(0f, 100f),
                    frequencyMhz = 0,
                    temperatureCelsius = findGpuTemperature(),
                    vendor = GpuVendor.GENERIC,
                    isAvailable = true
                )
            }
        } catch (e: Exception) {
            Timber.tag(TAG).d(e, "Failed to read generic GPU")
        }
        
        // GPU monitoring not available
        Timber.tag(TAG).d("GPU monitoring not available on this device")
        return GpuInfo.UNAVAILABLE
    }

    /**
     * Attempts to find GPU temperature from thermal zones.
     * Looks for thermal zones with "gpu" in their name or type.
     */
    private fun findGpuTemperature(): Float {
        try {
            val thermalDir = File(GPU_THERMAL_PATH)
            if (!thermalDir.exists() || !thermalDir.canRead()) {
                return 0f
            }

            thermalDir.listFiles()
                ?.filter { it.name.startsWith("thermal_zone") }
                ?.forEach { zone ->
                    // Check if this zone is GPU-related
                    val typeFile = File(zone, "type")
                    if (typeFile.exists() && typeFile.canRead()) {
                        val type = typeFile.readText().trim().lowercase()
                        if (type.contains("gpu")) {
                            val tempFile = File(zone, "temp")
                            if (tempFile.exists() && tempFile.canRead()) {
                                val temp = tempFile.readText().trim().toLongOrNull()
                                if (temp != null) {
                                    // Temperature is in millidegrees Celsius
                                    return (temp / 1000f).coerceIn(0f, 150f)
                                }
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Timber.tag(TAG).d(e, "Failed to find GPU temperature")
        }
        
        return 0f
    }

    /**
     * Clears cached GPU info.
     */
    fun clearCache() {
        cachedGpuInfo = null
        cacheTimestamp = 0L
    }
}
