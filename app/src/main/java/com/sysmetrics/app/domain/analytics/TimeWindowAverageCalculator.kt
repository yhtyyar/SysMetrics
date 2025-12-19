package com.sysmetrics.app.domain.analytics

import com.sysmetrics.app.data.model.advanced.MetricType
import com.sysmetrics.app.data.model.advanced.TimeWindowStats
import com.sysmetrics.app.native_bridge.NativeAnalytics
import com.sysmetrics.app.native_bridge.NativeTimeWindowStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.util.LinkedList
import kotlin.math.ceil

/**
 * Calculates time-window averages for metrics.
 * Supports 30s, 1m, 5m windows with min/max/percentile calculations.
 * 
 * Uses native C++ backend when available for optimal performance:
 * - Lock-free circular buffers
 * - SIMD-optimized calculations  
 * - O(n) percentile via QuickSelect algorithm
 * 
 * Performance: <1μs for averages, <10μs for percentiles
 */
class TimeWindowAverageCalculator(
    private val metricType: MetricType,
    private val maxDurationMs: Long = 5 * 60 * 1000L // 5 minutes
) {
    private data class DataPoint(val value: Float, val timestamp: Long)
    
    // Native handle (0 = use Kotlin fallback)
    private var nativeHandle: Long = 0L
    private val useNative: Boolean
    
    // Kotlin fallback
    private val dataPoints = LinkedList<DataPoint>()
    private val lock = Any()
    
    private val _stats = MutableStateFlow(TimeWindowStats.empty(metricType))
    val stats: StateFlow<TimeWindowStats> = _stats.asStateFlow()
    
    init {
        useNative = NativeAnalytics.isAvailable()
        if (useNative) {
            nativeHandle = NativeAnalytics.createTimeWindowCalculator(maxDurationMs)
            if (nativeHandle == 0L) {
                Timber.w("Failed to create native calculator, using Kotlin fallback")
            } else {
                Timber.d("Using native TimeWindowCalculator for $metricType")
            }
        }
    }
    
    fun addDataPoint(value: Float, timestamp: Long = System.currentTimeMillis()) {
        if (useNative && nativeHandle != 0L) {
            NativeAnalytics.twcAddPoint(nativeHandle, value, timestamp)
            updateStatsFromNative(value, timestamp)
        } else {
            addDataPointKotlin(value, timestamp)
        }
    }
    
    private fun addDataPointKotlin(value: Float, timestamp: Long) {
        synchronized(lock) {
            dataPoints.add(DataPoint(value, timestamp))
            
            // Remove old points outside max window
            val cutoff = timestamp - maxDurationMs
            while (dataPoints.isNotEmpty() && dataPoints.first.timestamp < cutoff) {
                dataPoints.removeFirst()
            }
            
            // Update stats
            updateStats(value, timestamp)
        }
    }
    
    private fun updateStatsFromNative(current: Float, timestamp: Long) {
        val nativeStats = NativeTimeWindowStats.fromArray(
            NativeAnalytics.twcGetStats(nativeHandle)
        )
        
        _stats.value = TimeWindowStats(
            metricType = metricType,
            current = current,
            avg30s = nativeStats.avg30s,
            avg1m = nativeStats.avg1m,
            avg5m = nativeStats.avg5m,
            min = nativeStats.min,
            max = nativeStats.max,
            p95 = nativeStats.p95,
            p99 = nativeStats.p99,
            timestamp = timestamp
        )
    }
    
    private fun updateStats(current: Float, now: Long) {
        val avg30s = getAverage(30_000L, now)
        val avg1m = getAverage(60_000L, now)
        val avg5m = getAverage(300_000L, now)
        
        val allValues = dataPoints.map { it.value }
        val min = allValues.minOrNull() ?: 0f
        val max = allValues.maxOrNull() ?: 0f
        val p95 = getPercentile(95, 60_000L, now)
        val p99 = getPercentile(99, 60_000L, now)
        
        _stats.value = TimeWindowStats(
            metricType = metricType,
            current = current,
            avg30s = avg30s,
            avg1m = avg1m,
            avg5m = avg5m,
            min = min,
            max = max,
            p95 = p95,
            p99 = p99,
            timestamp = now
        )
    }
    
    fun getAverage(windowMs: Long, now: Long = System.currentTimeMillis()): Float {
        synchronized(lock) {
            val cutoff = now - windowMs
            val values = dataPoints.filter { it.timestamp >= cutoff }.map { it.value }
            return if (values.isEmpty()) 0f else values.average().toFloat()
        }
    }
    
    fun getMin(windowMs: Long, now: Long = System.currentTimeMillis()): Float {
        synchronized(lock) {
            val cutoff = now - windowMs
            return dataPoints.filter { it.timestamp >= cutoff }
                .minOfOrNull { it.value } ?: 0f
        }
    }
    
    fun getMax(windowMs: Long, now: Long = System.currentTimeMillis()): Float {
        synchronized(lock) {
            val cutoff = now - windowMs
            return dataPoints.filter { it.timestamp >= cutoff }
                .maxOfOrNull { it.value } ?: 0f
        }
    }
    
    fun getPercentile(percentile: Int, windowMs: Long, now: Long = System.currentTimeMillis()): Float {
        synchronized(lock) {
            val cutoff = now - windowMs
            val values = dataPoints.filter { it.timestamp >= cutoff }
                .map { it.value }
                .sorted()
            
            if (values.isEmpty()) return 0f
            
            val index = ceil(values.size * percentile / 100.0).toInt() - 1
            return values.getOrElse(index.coerceAtLeast(0)) { values.last() }
        }
    }
    
    fun getDataPointsInWindow(windowMs: Long, now: Long = System.currentTimeMillis()): List<Float> {
        synchronized(lock) {
            val cutoff = now - windowMs
            return dataPoints.filter { it.timestamp >= cutoff }.map { it.value }
        }
    }
    
    fun clear() {
        if (useNative && nativeHandle != 0L) {
            NativeAnalytics.twcClear(nativeHandle)
        }
        synchronized(lock) {
            dataPoints.clear()
            _stats.value = TimeWindowStats.empty(metricType)
        }
    }
    
    fun getDataPointCount(): Int = synchronized(lock) { dataPoints.size }
    
    fun destroy() {
        if (nativeHandle != 0L) {
            NativeAnalytics.destroyTimeWindowCalculator(nativeHandle)
            nativeHandle = 0L
        }
    }
    
    protected fun finalize() {
        destroy()
    }
}

/**
 * Manager for multiple metric calculators.
 */
class MetricsAverageManager {
    private val calculators = mutableMapOf<MetricType, TimeWindowAverageCalculator>()
    
    fun getCalculator(metricType: MetricType): TimeWindowAverageCalculator {
        return calculators.getOrPut(metricType) {
            TimeWindowAverageCalculator(metricType)
        }
    }
    
    fun addDataPoint(metricType: MetricType, value: Float) {
        getCalculator(metricType).addDataPoint(value)
    }
    
    fun getStats(metricType: MetricType): TimeWindowStats {
        return getCalculator(metricType).stats.value
    }
    
    fun clearAll() {
        calculators.values.forEach { it.clear() }
    }
}
