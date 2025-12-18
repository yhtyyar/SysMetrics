package com.sysmetrics.app.utils

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import timber.log.Timber

/**
 * Memory monitoring utility for detecting memory issues.
 * Helps identify memory leaks and excessive memory usage.
 */
class MemoryMonitor(private val context: Context) {
    
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val runtime = Runtime.getRuntime()
    
    companion object {
        private const val TAG = "MEMORY_MONITOR"
        private const val MB = 1024 * 1024L
    }
    
    /**
     * Get current memory info.
     */
    fun getMemoryInfo(): MemoryInfo {
        val javaHeapUsed = (runtime.totalMemory() - runtime.freeMemory()) / MB
        val javaHeapMax = runtime.maxMemory() / MB
        val nativeHeap = Debug.getNativeHeapAllocatedSize() / MB
        
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        
        val availableRam = memInfo.availMem / MB
        val totalRam = memInfo.totalMem / MB
        val lowMemory = memInfo.lowMemory
        
        return MemoryInfo(
            javaHeapUsedMb = javaHeapUsed,
            javaHeapMaxMb = javaHeapMax,
            nativeHeapMb = nativeHeap,
            availableRamMb = availableRam,
            totalRamMb = totalRam,
            isLowMemory = lowMemory
        )
    }
    
    /**
     * Log current memory status.
     */
    fun logMemoryStatus() {
        val info = getMemoryInfo()
        
        Timber.tag(TAG).d("""
            Memory Status:
            - Java Heap: ${info.javaHeapUsedMb}MB / ${info.javaHeapMaxMb}MB (${info.heapUsagePercent}%)
            - Native Heap: ${info.nativeHeapMb}MB
            - Available RAM: ${info.availableRamMb}MB / ${info.totalRamMb}MB
            - Low Memory: ${info.isLowMemory}
        """.trimIndent())
        
        if (info.heapUsagePercent > 80) {
            Timber.tag(TAG).w("⚠️ High heap usage: ${info.heapUsagePercent}%")
        }
        
        if (info.isLowMemory) {
            Timber.tag(TAG).e("🔴 System is running low on memory!")
        }
    }
    
    /**
     * Check if memory usage is critical.
     */
    fun isCriticalMemoryUsage(): Boolean {
        val info = getMemoryInfo()
        return info.heapUsagePercent > 90 || info.isLowMemory
    }
    
    /**
     * Force garbage collection (use sparingly in debug builds only).
     */
    fun forceGC() {
        Timber.tag(TAG).d("Forcing garbage collection...")
        System.gc()
        System.runFinalization()
        System.gc()
    }
    
    /**
     * Memory information data class.
     */
    data class MemoryInfo(
        val javaHeapUsedMb: Long,
        val javaHeapMaxMb: Long,
        val nativeHeapMb: Long,
        val availableRamMb: Long,
        val totalRamMb: Long,
        val isLowMemory: Boolean
    ) {
        val heapUsagePercent: Int
            get() = ((javaHeapUsedMb.toFloat() / javaHeapMaxMb) * 100).toInt()
    }
}
