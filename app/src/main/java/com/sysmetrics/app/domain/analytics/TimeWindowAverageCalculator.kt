package com.sysmetrics.app.domain.analytics

import com.sysmetrics.app.data.model.advanced.MetricType
import com.sysmetrics.app.data.model.advanced.TimeWindowStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.LinkedList
import kotlin.math.ceil

/**
 * Calculates time-window averages for metrics.
 * Supports 30s, 1m, 5m windows with min/max/percentile calculations.
 * Uses efficient circular buffer for memory management.
 */
class TimeWindowAverageCalculator(
    private val metricType: MetricType,
    private val maxDurationMs: Long = 5 * 60 * 1000L // 5 minutes
) {
    private data class DataPoint(val value: Float, val timestamp: Long)
    
    private val dataPoints = LinkedList<DataPoint>()
    private val lock = Any()
    
    private val _stats = MutableStateFlow(TimeWindowStats.empty(metricType))
    val stats: StateFlow<TimeWindowStats> = _stats.asStateFlow()
    
    fun addDataPoint(value: Float, timestamp: Long = System.currentTimeMillis()) {
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
        synchronized(lock) {
            dataPoints.clear()
            _stats.value = TimeWindowStats.empty(metricType)
        }
    }
    
    fun getDataPointCount(): Int = synchronized(lock) { dataPoints.size }
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
