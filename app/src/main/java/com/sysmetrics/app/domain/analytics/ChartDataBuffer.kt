package com.sysmetrics.app.domain.analytics

import com.sysmetrics.app.data.model.advanced.ChartData
import com.sysmetrics.app.data.model.advanced.ChartDataPoint
import com.sysmetrics.app.data.model.advanced.MetricType
import com.sysmetrics.app.data.model.advanced.Severity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Circular buffer for chart data points.
 * Memory-efficient storage with automatic old data removal.
 */
class ChartDataBuffer(
    private val metricType: MetricType,
    private val maxSize: Int = 60
) {
    private val buffer = ArrayDeque<ChartDataPoint>(maxSize)
    private val lock = Any()
    
    private val _chartData = MutableStateFlow(ChartData.empty(metricType))
    val chartData: StateFlow<ChartData> = _chartData.asStateFlow()
    
    fun add(value: Float, timestamp: Long = System.currentTimeMillis()) {
        synchronized(lock) {
            val severity = when (metricType) {
                MetricType.FPS -> Severity.fromFps(value.toInt())
                else -> Severity.fromValue(value)
            }
            
            val point = ChartDataPoint(timestamp, value, severity)
            
            if (buffer.size >= maxSize) {
                buffer.removeFirst()
            }
            buffer.addLast(point)
            
            _chartData.value = ChartData(
                metricType = metricType,
                points = buffer.toList(),
                maxHistorySize = maxSize
            )
        }
    }
    
    fun getPoints(): List<ChartDataPoint> = synchronized(lock) { buffer.toList() }
    
    fun getLatest(): ChartDataPoint? = synchronized(lock) { buffer.lastOrNull() }
    
    fun clear() {
        synchronized(lock) {
            buffer.clear()
            _chartData.value = ChartData.empty(metricType)
        }
    }
    
    fun size(): Int = synchronized(lock) { buffer.size }
    
    fun isEmpty(): Boolean = synchronized(lock) { buffer.isEmpty() }
    
    fun getMinValue(): Float = synchronized(lock) { 
        buffer.minOfOrNull { it.value } ?: 0f 
    }
    
    fun getMaxValue(): Float = synchronized(lock) { 
        buffer.maxOfOrNull { it.value } ?: 0f 
    }
    
    fun getAverageValue(): Float = synchronized(lock) {
        if (buffer.isEmpty()) 0f else buffer.map { it.value }.average().toFloat()
    }
}

/**
 * Manager for multiple chart buffers.
 */
class ChartBufferManager(private val maxHistorySize: Int = 60) {
    private val buffers = mutableMapOf<MetricType, ChartDataBuffer>()
    
    fun getBuffer(metricType: MetricType): ChartDataBuffer {
        return buffers.getOrPut(metricType) {
            ChartDataBuffer(metricType, maxHistorySize)
        }
    }
    
    fun addDataPoint(metricType: MetricType, value: Float) {
        getBuffer(metricType).add(value)
    }
    
    fun getChartData(metricType: MetricType): ChartData {
        return getBuffer(metricType).chartData.value
    }
    
    fun clearAll() {
        buffers.values.forEach { it.clear() }
    }
    
    fun updateHistorySize(newSize: Int) {
        buffers.clear() // Recreate with new size
    }
}
