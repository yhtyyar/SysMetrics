package com.sysmetrics.app.domain.analytics

import com.sysmetrics.app.data.model.advanced.MetricType
import com.sysmetrics.app.data.model.advanced.PeakStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.LinkedList

/**
 * Tracks peak values for metrics within configurable time windows.
 * Used for peak notification generation.
 */
class PeakTracker(
    private val windowMs: Long = 60_000L // Default 1 minute
) {
    private data class MetricPoint(
        val value: Float,
        val timestamp: Long
    )
    
    private val cpuData = LinkedList<MetricPoint>()
    private val ramData = LinkedList<MetricPoint>()
    private val tempData = LinkedList<MetricPoint>()
    private val netIngressData = LinkedList<MetricPoint>()
    private val netEgressData = LinkedList<MetricPoint>()
    private val fpsData = LinkedList<MetricPoint>()
    
    private val lock = Any()
    
    private val _peakStats = MutableStateFlow(PeakStats.EMPTY)
    val peakStats: StateFlow<PeakStats> = _peakStats.asStateFlow()
    
    fun addCpuValue(value: Float, timestamp: Long = System.currentTimeMillis()) {
        addToList(cpuData, value, timestamp)
        updateStats(timestamp)
    }
    
    fun addRamValue(valueMb: Long, timestamp: Long = System.currentTimeMillis()) {
        addToList(ramData, valueMb.toFloat(), timestamp)
        updateStats(timestamp)
    }
    
    fun addTempValue(value: Float, timestamp: Long = System.currentTimeMillis()) {
        addToList(tempData, value, timestamp)
        updateStats(timestamp)
    }
    
    fun addNetworkValues(ingressMbps: Float, egressMbps: Float, timestamp: Long = System.currentTimeMillis()) {
        addToList(netIngressData, ingressMbps, timestamp)
        addToList(netEgressData, egressMbps, timestamp)
        updateStats(timestamp)
    }
    
    fun addFpsValue(value: Int, timestamp: Long = System.currentTimeMillis()) {
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
    
    fun setWindowDuration(windowMs: Long) {
        // Will take effect on next data point
    }
}
