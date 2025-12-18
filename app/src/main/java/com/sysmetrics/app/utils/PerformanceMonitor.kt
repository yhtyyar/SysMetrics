package com.sysmetrics.app.utils

import android.os.SystemClock
import timber.log.Timber

/**
 * Performance monitoring utility for debugging and optimization.
 * Helps identify performance bottlenecks in production code.
 */
object PerformanceMonitor {
    
    @PublishedApi
    internal const val TAG = "PERFORMANCE"
    
    @PublishedApi
    internal val measurements = mutableMapOf<String, MutableList<Long>>()
    
    /**
     * Measure execution time of a code block.
     * 
     * @param label Label for this measurement
     * @param block Code block to measure
     * @return Result of the block
     */
    inline fun <T> measure(label: String, block: () -> T): T {
        val startTime = SystemClock.elapsedRealtime()
        
        return try {
            block()
        } finally {
            val duration = SystemClock.elapsedRealtime() - startTime
            recordMeasurement(label, duration)
            
            if (duration > 100) {
                Timber.tag(TAG).w("⚠️ Slow operation: $label took ${duration}ms")
            } else {
                Timber.tag(TAG).v("✅ $label: ${duration}ms")
            }
        }
    }
    
    /**
     * Measure execution time of a suspend function.
     */
    inline fun <T> measureSuspend(label: String, block: () -> T): T {
        return measure(label, block)
    }
    
    /**
     * Record a measurement for later analysis.
     */
    @PublishedApi
    internal fun recordMeasurement(label: String, duration: Long) {
        measurements.getOrPut(label) { mutableListOf() }.add(duration)
    }
    
    /**
     * Get statistics for a specific measurement.
     */
    fun getStats(label: String): Stats? {
        val values = measurements[label] ?: return null
        
        if (values.isEmpty()) return null
        
        val sorted = values.sorted()
        return Stats(
            count = values.size,
            min = sorted.first(),
            max = sorted.last(),
            avg = values.average(),
            median = sorted[sorted.size / 2]
        )
    }
    
    /**
     * Print all statistics to log.
     */
    fun printAllStats() {
        Timber.tag(TAG).i("📊 Performance Statistics:")
        measurements.keys.sorted().forEach { label ->
            val stats = getStats(label)
            if (stats != null) {
                Timber.tag(TAG).i("  $label: avg=${stats.avg.toInt()}ms, min=${stats.min}ms, max=${stats.max}ms (n=${stats.count})")
            }
        }
    }
    
    /**
     * Clear all measurements.
     */
    fun clear() {
        measurements.clear()
    }
    
    /**
     * Statistics data class.
     */
    data class Stats(
        val count: Int,
        val min: Long,
        val max: Long,
        val avg: Double,
        val median: Long
    )
}
