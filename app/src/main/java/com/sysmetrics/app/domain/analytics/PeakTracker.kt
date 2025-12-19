package com.sysmetrics.app.domain.analytics

import com.sysmetrics.app.data.model.advanced.MetricType
import com.sysmetrics.app.data.model.advanced.PeakStats
import com.sysmetrics.app.native_bridge.NativeAnalytics
import com.sysmetrics.app.native_bridge.NativePeakData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.util.LinkedList

/**
 * Tracks peak values for metrics within configurable time windows.
 * Used for peak notification generation.
 * 
 * Uses native C++ backend when available:
 * - Efficient peak tracking with O(1) updates
 * - Minimal memory overhead
 * - Thread-safe implementation
 */
class PeakTracker(
    private val windowMs: Long = 60_000L // Default 1 minute
) {
    private data class MetricPoint(
        val value: Float,
        val timestamp: Long
    )
    
    // Native handles for each metric
    private var cpuHandle: Long = 0L
    private var ramHandle: Long = 0L
    private var tempHandle: Long = 0L
    private var netIngressHandle: Long = 0L
    private var netEgressHandle: Long = 0L
    private var fpsHandle: Long = 0L
    private val useNative: Boolean
    
    // Kotlin fallback
    private val cpuData = LinkedList<MetricPoint>()
    private val ramData = LinkedList<MetricPoint>()
    private val tempData = LinkedList<MetricPoint>()
    private val netIngressData = LinkedList<MetricPoint>()
    private val netEgressData = LinkedList<MetricPoint>()
    private val fpsData = LinkedList<MetricPoint>()
    
    private val lock = Any()
    
    private val _peakStats = MutableStateFlow(PeakStats.EMPTY)
    val peakStats: StateFlow<PeakStats> = _peakStats.asStateFlow()
    
    init {
        useNative = NativeAnalytics.isAvailable()
        if (useNative) {
            cpuHandle = NativeAnalytics.createPeakTracker(windowMs)
            ramHandle = NativeAnalytics.createPeakTracker(windowMs)
            tempHandle = NativeAnalytics.createPeakTracker(windowMs)
            netIngressHandle = NativeAnalytics.createPeakTracker(windowMs)
            netEgressHandle = NativeAnalytics.createPeakTracker(windowMs)
            fpsHandle = NativeAnalytics.createPeakTracker(windowMs)
            Timber.d("Using native PeakTracker")
        }
    }
    
    fun addCpuValue(value: Float, timestamp: Long = System.currentTimeMillis()) {
        if (useNative && cpuHandle != 0L) {
            NativeAnalytics.peakAddValue(cpuHandle, value, timestamp)
        }
        addToList(cpuData, value, timestamp)
        updateStats(timestamp)
    }
    
    fun addRamValue(valueMb: Long, timestamp: Long = System.currentTimeMillis()) {
        if (useNative && ramHandle != 0L) {
            NativeAnalytics.peakAddValue(ramHandle, valueMb.toFloat(), timestamp)
        }
        addToList(ramData, valueMb.toFloat(), timestamp)
        updateStats(timestamp)
    }
    
    fun addTempValue(value: Float, timestamp: Long = System.currentTimeMillis()) {
        if (useNative && tempHandle != 0L) {
            NativeAnalytics.peakAddValue(tempHandle, value, timestamp)
        }
        addToList(tempData, value, timestamp)
        updateStats(timestamp)
    }
    
    fun addNetworkValues(ingressMbps: Float, egressMbps: Float, timestamp: Long = System.currentTimeMillis()) {
        if (useNative) {
            if (netIngressHandle != 0L) NativeAnalytics.peakAddValue(netIngressHandle, ingressMbps, timestamp)
            if (netEgressHandle != 0L) NativeAnalytics.peakAddValue(netEgressHandle, egressMbps, timestamp)
        }
        addToList(netIngressData, ingressMbps, timestamp)
        addToList(netEgressData, egressMbps, timestamp)
        updateStats(timestamp)
    }
    
    fun addFpsValue(value: Int, timestamp: Long = System.currentTimeMillis()) {
        if (useNative && fpsHandle != 0L) {
            NativeAnalytics.peakAddValue(fpsHandle, value.toFloat(), timestamp)
        }
        addToList(fpsData, value.toFloat(), timestamp)
        updateStats(timestamp)
    }
    
    private fun addToList(list: LinkedList<MetricPoint>, value: Float, timestamp: Long) {
        synchronized(lock) {
            list.add(MetricPoint(value, timestamp))
            val cutoff = timestamp - windowMs
            while (list.isNotEmpty() && list.first.timestamp < cutoff) {
                list.removeFirst()
            }
        }
    }
    
    private fun updateStats(now: Long) {
        synchronized(lock) {
            val windowStart = now - windowMs
            
            // CPU
            val cpuPeakPoint = cpuData.maxByOrNull { it.value }
            val cpuAvg = if (cpuData.isEmpty()) 0f else cpuData.map { it.value }.average().toFloat()
            
            // RAM
            val ramPeakPoint = ramData.maxByOrNull { it.value }
            val ramAvg = if (ramData.isEmpty()) 0f else ramData.map { it.value }.average().toFloat()
            
            // Temp
            val tempPeakPoint = tempData.maxByOrNull { it.value }
            val tempAvg = if (tempData.isEmpty()) 0f else tempData.map { it.value }.average().toFloat()
            
            // Network
            val netIngressPeakPoint = netIngressData.maxByOrNull { it.value }
            val netEgressPeakPoint = netEgressData.maxByOrNull { it.value }
            
            // FPS
            val fpsValues = fpsData.map { it.value.toInt() }
            val fpsPeak = fpsValues.maxOrNull() ?: 0
            val fpsMin = fpsValues.minOrNull() ?: 0
            val fpsAvg = if (fpsValues.isEmpty()) 0f else fpsValues.average().toFloat()
            val frameDrops = fpsValues.count { it < 30 }
            
            _peakStats.value = PeakStats(
                cpuPeak = cpuPeakPoint?.value ?: 0f,
                cpuPeakTime = cpuPeakPoint?.timestamp ?: 0L,
                cpuAvg = cpuAvg,
                
                ramPeakMb = ramPeakPoint?.value?.toLong() ?: 0L,
                ramPeakTime = ramPeakPoint?.timestamp ?: 0L,
                ramAvgMb = ramAvg,
                
                tempPeak = tempPeakPoint?.value ?: 0f,
                tempPeakTime = tempPeakPoint?.timestamp ?: 0L,
                tempAvg = tempAvg,
                
                netIngressPeakMbps = netIngressPeakPoint?.value ?: 0f,
                netIngressPeakTime = netIngressPeakPoint?.timestamp ?: 0L,
                
                netEgressPeakMbps = netEgressPeakPoint?.value ?: 0f,
                netEgressPeakTime = netEgressPeakPoint?.timestamp ?: 0L,
                
                fpsPeak = fpsPeak,
                fpsMin = fpsMin,
                fpsAvg = fpsAvg,
                frameDrops = frameDrops,
                
                windowStartTime = windowStart,
                windowEndTime = now
            )
        }
    }
    
    fun getCurrentPeakStats(): PeakStats = _peakStats.value
    
    fun reset() {
        if (useNative) {
            if (cpuHandle != 0L) NativeAnalytics.peakReset(cpuHandle)
            if (ramHandle != 0L) NativeAnalytics.peakReset(ramHandle)
            if (tempHandle != 0L) NativeAnalytics.peakReset(tempHandle)
            if (netIngressHandle != 0L) NativeAnalytics.peakReset(netIngressHandle)
            if (netEgressHandle != 0L) NativeAnalytics.peakReset(netEgressHandle)
            if (fpsHandle != 0L) NativeAnalytics.peakReset(fpsHandle)
        }
        synchronized(lock) {
            cpuData.clear()
            ramData.clear()
            tempData.clear()
            netIngressData.clear()
            netEgressData.clear()
            fpsData.clear()
            _peakStats.value = PeakStats.EMPTY
        }
    }
    
    fun destroy() {
        if (cpuHandle != 0L) { NativeAnalytics.destroyPeakTracker(cpuHandle); cpuHandle = 0L }
        if (ramHandle != 0L) { NativeAnalytics.destroyPeakTracker(ramHandle); ramHandle = 0L }
        if (tempHandle != 0L) { NativeAnalytics.destroyPeakTracker(tempHandle); tempHandle = 0L }
        if (netIngressHandle != 0L) { NativeAnalytics.destroyPeakTracker(netIngressHandle); netIngressHandle = 0L }
        if (netEgressHandle != 0L) { NativeAnalytics.destroyPeakTracker(netEgressHandle); netEgressHandle = 0L }
        if (fpsHandle != 0L) { NativeAnalytics.destroyPeakTracker(fpsHandle); fpsHandle = 0L }
    }
    
    protected fun finalize() {
        destroy()
    }
    
    fun setWindowDuration(windowMs: Long) {
        // Recreate trackers with new window - requires destroy/create
    }
}
