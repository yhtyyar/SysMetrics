package com.sysmetrics.app.domain.analytics

import com.sysmetrics.app.data.model.advanced.ChartData
import com.sysmetrics.app.data.model.advanced.ChartDataPoint
import com.sysmetrics.app.data.model.advanced.MetricType
import com.sysmetrics.app.data.model.advanced.Severity
import com.sysmetrics.app.native_bridge.NativeAnalytics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * Circular buffer for chart data points.
 * Memory-efficient storage with automatic old data removal.
 * 
 * Uses native C++ backend when available:
 * - Pre-computed normalized values for rendering
 * - Cache-aligned memory layout
 * - O(1) push operations
 */
class ChartDataBuffer(
    private val metricType: MetricType,
    private val maxSize: Int = 60
) {
    // Native handle (0 = use Kotlin fallback)
    private var nativeHandle: Long = 0L
    private val useNative: Boolean
    
    // Kotlin fallback
    private val buffer = ArrayDeque<ChartDataPoint>(maxSize)
    private val lock = Any()
    
    private val _chartData = MutableStateFlow(ChartData.empty(metricType))
    val chartData: StateFlow<ChartData> = _chartData.asStateFlow()
    
    init {
        useNative = NativeAnalytics.isAvailable()
        if (useNative) {
            nativeHandle = NativeAnalytics.createChartBuffer(maxSize)
            if (nativeHandle != 0L) {
                Timber.d("Using native ChartBuffer for $metricType")
            }
        }
    }
    
    fun add(value: Float, timestamp: Long = System.currentTimeMillis()) {
        if (useNative && nativeHandle != 0L) {
            NativeAnalytics.chartAddPoint(nativeHandle, value, timestamp)
        }
        
        // Always maintain Kotlin buffer for ChartData emission
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
    
    /**
     * Get pre-computed normalized values (0-1 range) from native buffer.
     * Optimized for chart rendering.
     */
    fun getNormalizedValues(): FloatArray? {
        if (useNative && nativeHandle != 0L) {
            return NativeAnalytics.chartGetNormalized(nativeHandle, maxSize)
        }
        // Kotlin fallback
        synchronized(lock) {
            if (buffer.isEmpty()) return null
            val min = buffer.minOf { it.value }
            val max = buffer.maxOf { it.value }
            val range = (max - min).coerceAtLeast(0.001f)
            return buffer.map { (it.value - min) / range }.toFloatArray()
        }
    }
    
    fun getPoints(): List<ChartDataPoint> = synchronized(lock) { buffer.toList() }
    
    fun getLatest(): ChartDataPoint? = synchronized(lock) { buffer.lastOrNull() }
    
    fun clear() {
        if (useNative && nativeHandle != 0L) {
            NativeAnalytics.chartClear(nativeHandle)
        }
        synchronized(lock) {
            buffer.clear()
            _chartData.value = ChartData.empty(metricType)
        }
    }
    
    fun destroy() {
        if (nativeHandle != 0L) {
            NativeAnalytics.destroyChartBuffer(nativeHandle)
            nativeHandle = 0L
        }
    }
    
    protected fun finalize() {
        destroy()
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
    
    fun getNormalizedValues(metricType: MetricType): FloatArray? {
        return getBuffer(metricType).getNormalizedValues()
    }
    
    fun clearAll() {
        buffers.values.forEach { it.clear() }
    }
    
    fun destroyAll() {
        buffers.values.forEach { it.destroy() }
        buffers.clear()
    }
    
    fun updateHistorySize(newSize: Int) {
        destroyAll()
    }
}
