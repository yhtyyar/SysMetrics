package com.sysmetrics.app.native_bridge

import timber.log.Timber

/**
 * JNI Bridge for Native Analytics Engine.
 * 
 * Provides high-performance analytics using C++ backend:
 * - Lock-free circular buffers
 * - SIMD-optimized calculations
 * - Cache-aligned memory layout
 * - O(n) percentile calculations via QuickSelect
 * 
 * Performance targets:
 * - Average calculation: <1μs
 * - Percentile calculation: <10μs
 * - Buffer operations: O(1)
 */
object NativeAnalytics {
    
    private const val TAG = "NATIVE_ANALYTICS"
    
    @Volatile
    private var isLoaded = false
    
    init {
        loadLibrary()
    }
    
    private fun loadLibrary() {
        if (isLoaded) return
        
        try {
            System.loadLibrary("sysmetrics_native")
            isLoaded = true
            Timber.tag(TAG).d("Native analytics library loaded successfully")
        } catch (e: UnsatisfiedLinkError) {
            Timber.tag(TAG).e(e, "Failed to load native analytics library")
            isLoaded = false
        }
    }
    
    fun isAvailable(): Boolean = isLoaded
    
    // ========================================================================
    // Time Window Calculator
    // ========================================================================
    
    /**
     * Create native TimeWindowCalculator.
     * @param maxDurationMs Maximum data retention (default 5 minutes)
     * @return Handle to calculator, or 0 on failure
     */
    @JvmStatic
    external fun createTimeWindowCalculator(maxDurationMs: Long): Long
    
    /**
     * Destroy TimeWindowCalculator and free resources.
     */
    @JvmStatic
    external fun destroyTimeWindowCalculator(handle: Long)
    
    /**
     * Add data point to TimeWindowCalculator.
     */
    @JvmStatic
    external fun twcAddPoint(handle: Long, value: Float, timestamp: Long)
    
    /**
     * Get all statistics from TimeWindowCalculator.
     * @return FloatArray [current, avg30s, avg1m, avg5m, min, max, p50, p95, p99]
     */
    @JvmStatic
    external fun twcGetStats(handle: Long): FloatArray?
    
    /**
     * Clear all data from TimeWindowCalculator.
     */
    @JvmStatic
    external fun twcClear(handle: Long)
    
    // ========================================================================
    // Chart Buffer
    // ========================================================================
    
    /**
     * Create native ChartBuffer.
     * @param capacity Maximum number of data points
     * @return Handle to buffer, or 0 on failure
     */
    @JvmStatic
    external fun createChartBuffer(capacity: Int): Long
    
    /**
     * Destroy ChartBuffer and free resources.
     */
    @JvmStatic
    external fun destroyChartBuffer(handle: Long)
    
    /**
     * Add data point to ChartBuffer.
     */
    @JvmStatic
    external fun chartAddPoint(handle: Long, value: Float, timestamp: Long)
    
    /**
     * Get normalized values (0-1 range) for chart rendering.
     * @return FloatArray of normalized values, or null if empty
     */
    @JvmStatic
    external fun chartGetNormalized(handle: Long, maxCount: Int): FloatArray?
    
    /**
     * Get min/max range of values in buffer.
     * @return FloatArray [min, max]
     */
    @JvmStatic
    external fun chartGetRange(handle: Long): FloatArray?
    
    /**
     * Clear all data from ChartBuffer.
     */
    @JvmStatic
    external fun chartClear(handle: Long)
    
    // ========================================================================
    // Peak Tracker
    // ========================================================================
    
    /**
     * Create native PeakTracker.
     * @param windowMs Time window for peak tracking
     * @return Handle to tracker, or 0 on failure
     */
    @JvmStatic
    external fun createPeakTracker(windowMs: Long): Long
    
    /**
     * Destroy PeakTracker and free resources.
     */
    @JvmStatic
    external fun destroyPeakTracker(handle: Long)
    
    /**
     * Add value to PeakTracker.
     */
    @JvmStatic
    external fun peakAddValue(handle: Long, value: Float, timestamp: Long)
    
    /**
     * Get current peak data.
     * @return FloatArray [peakValue, peakTimestamp, avgValue, sampleCount]
     */
    @JvmStatic
    external fun peakGetData(handle: Long): FloatArray?
    
    /**
     * Reset PeakTracker.
     */
    @JvmStatic
    external fun peakReset(handle: Long)
}

/**
 * Data class for TimeWindow statistics returned from native code.
 */
data class NativeTimeWindowStats(
    val current: Float,
    val avg30s: Float,
    val avg1m: Float,
    val avg5m: Float,
    val min: Float,
    val max: Float,
    val p50: Float,
    val p95: Float,
    val p99: Float
) {
    companion object {
        val EMPTY = NativeTimeWindowStats(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
        
        fun fromArray(arr: FloatArray?): NativeTimeWindowStats {
            if (arr == null || arr.size < 9) return EMPTY
            return NativeTimeWindowStats(
                current = arr[0],
                avg30s = arr[1],
                avg1m = arr[2],
                avg5m = arr[3],
                min = arr[4],
                max = arr[5],
                p50 = arr[6],
                p95 = arr[7],
                p99 = arr[8]
            )
        }
    }
}

/**
 * Data class for Peak data returned from native code.
 */
data class NativePeakData(
    val peakValue: Float,
    val peakTimestamp: Long,
    val avgValue: Float,
    val sampleCount: Int
) {
    companion object {
        val EMPTY = NativePeakData(0f, 0L, 0f, 0)
        
        fun fromArray(arr: FloatArray?): NativePeakData {
            if (arr == null || arr.size < 4) return EMPTY
            return NativePeakData(
                peakValue = arr[0],
                peakTimestamp = arr[1].toLong(),
                avgValue = arr[2],
                sampleCount = arr[3].toInt()
            )
        }
    }
}
