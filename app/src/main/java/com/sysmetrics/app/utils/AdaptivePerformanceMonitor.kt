package com.sysmetrics.app.utils

import com.sysmetrics.app.core.common.Constants
import com.sysmetrics.app.data.model.SystemMetrics
import timber.log.Timber

/**
 * Adaptive performance monitor that adjusts update interval based on system load.
 * Helps reduce battery consumption and CPU usage when system is under pressure.
 * 
 * Strategy:
 * - Low load: Fast intervals (500ms) for responsive updates
 * - Normal load: Standard intervals (1000ms)
 * - High load: Slow intervals (2000ms) to reduce overhead
 * - Critical load: Very slow intervals (5000ms) to prevent system overload
 * 
 * IMPROVEMENTS:
 * - Uses constants from central configuration
 * - Singleton for consistent state
 * - Better performance categorization
 */
class AdaptivePerformanceMonitor {
    
    companion object {
        private const val TAG = "ADAPTIVE_PERF"
    }
    
    private var currentInterval: Long = Constants.AdaptiveIntervals.NORMAL
    private var lastCheckTime: Long = 0L
    
    /**
     * Calculate optimal update interval based on system metrics.
     * 
     * @param metrics Current system metrics
     * @param isTvDevice Whether device is Android TV
     * @param preferredInterval User's preferred interval
     * @return Optimal update interval in milliseconds
     */
    fun calculateOptimalInterval(
        metrics: SystemMetrics,
        isTvDevice: Boolean,
        preferredInterval: Long = Constants.AdaptiveIntervals.NORMAL
    ): Long {
        val now = System.currentTimeMillis()
        
        // Don't adjust too frequently
        if (now - lastCheckTime < Constants.AdaptiveIntervals.CHECK_INTERVAL_MS) {
            return currentInterval
        }
        
        lastCheckTime = now
        
        // Determine load level
        val loadLevel = determineLoadLevel(metrics)
        
        val newInterval = when {
            // Critical load - slow down significantly
            loadLevel == LoadLevel.CRITICAL -> {
                Timber.tag(TAG).w("Critical system load detected - using very slow interval")
                Constants.AdaptiveIntervals.VERY_SLOW
            }
            
            // High load - slow down
            loadLevel == LoadLevel.HIGH -> {
                Timber.tag(TAG).i("High system load detected - using slow interval")
                Constants.AdaptiveIntervals.SLOW
            }
            
            // TV device with normal load
            isTvDevice && loadLevel == LoadLevel.NORMAL -> {
                Constants.AdaptiveIntervals.NORMAL
            }
            
            // Mobile device with low load - use fast interval
            !isTvDevice && loadLevel == LoadLevel.LOW -> {
                Constants.AdaptiveIntervals.FAST
            }
            
            // Default
            else -> preferredInterval.coerceIn(Constants.AdaptiveIntervals.FAST, Constants.AdaptiveIntervals.SLOW)
        }
        
        if (newInterval != currentInterval) {
            Timber.tag(TAG).d("Adjusting interval from ${currentInterval}ms to ${newInterval}ms (load: $loadLevel)")
            currentInterval = newInterval
        }
        
        return newInterval
    }
    
    /**
     * Determine current system load level.
     */
    private fun determineLoadLevel(metrics: SystemMetrics): LoadLevel {
        val cpuUsage = metrics.cpuUsage
        val ramUsagePercent = metrics.ramUsagePercent
        val availableRamMb = metrics.ramTotalMb - metrics.ramUsedMb
        
        return when {
            // Critical: Very high CPU or very low memory
            cpuUsage > Constants.PerformanceThresholds.CRITICAL_CPU_THRESHOLD || 
            ramUsagePercent > Constants.PerformanceThresholds.CRITICAL_RAM_THRESHOLD || 
            availableRamMb < Constants.PerformanceThresholds.CRITICAL_MEMORY_THRESHOLD_MB -> {
                LoadLevel.CRITICAL
            }
            
            // High: High CPU or high memory pressure
            cpuUsage > Constants.PerformanceThresholds.HIGH_CPU_THRESHOLD || 
            ramUsagePercent > Constants.PerformanceThresholds.HIGH_RAM_THRESHOLD || 
            availableRamMb < Constants.PerformanceThresholds.LOW_MEMORY_THRESHOLD_MB -> {
                LoadLevel.HIGH
            }
            
            // Low: Low usage
            cpuUsage < 30f && ramUsagePercent < Constants.PerformanceThresholds.RAM_NORMAL_MAX -> {
                LoadLevel.LOW
            }
            
            // Normal: Everything else
            else -> {
                LoadLevel.NORMAL
            }
        }
    }
    
    /**
     * Reset the monitor state.
     */
    fun reset() {
        currentInterval = Constants.AdaptiveIntervals.NORMAL
        lastCheckTime = 0L
        Timber.tag(TAG).d("Adaptive monitor reset")
    }
    
    /**
     * Get current interval without recalculation.
     */
    fun getCurrentInterval(): Long = currentInterval
    
    /**
     * System load levels.
     */
    private enum class LoadLevel {
        LOW,      // < 30% CPU, < 50% RAM
        NORMAL,   // Normal usage
        HIGH,     // > 80% CPU or > 85% RAM
        CRITICAL  // > 90% CPU or > 95% RAM or < 100MB available
    }
}
