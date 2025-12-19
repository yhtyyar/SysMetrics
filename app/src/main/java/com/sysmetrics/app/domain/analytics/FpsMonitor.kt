package com.sysmetrics.app.domain.analytics

import android.os.Handler
import android.os.Looper
import android.view.Choreographer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * Real-time FPS monitoring using Choreographer API.
 * Tracks frame rate, detects jank, and provides FPS statistics.
 * 
 * Must be started on the main thread.
 */
class FpsMonitor {
    
    private val choreographer: Choreographer by lazy { Choreographer.getInstance() }
    private val mainHandler = Handler(Looper.getMainLooper())
    
    private var frameCount = 0
    private var lastSecondTime = System.nanoTime()
    private var lastFrameTime = 0L
    
    private val _currentFps = MutableStateFlow(0)
    val currentFps: StateFlow<Int> = _currentFps.asStateFlow()
    
    private val _fpsStats = MutableStateFlow(FpsStats.EMPTY)
    val fpsStats: StateFlow<FpsStats> = _fpsStats.asStateFlow()
    
    private var isMonitoring = false
    private var jankThresholdMs = 16.67f // 60fps threshold
    private var fpsThreshold = 30
    
    private val fpsHistory = mutableListOf<Int>()
    private var jankFrameCount = 0
    private var totalFrameCount = 0
    
    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!isMonitoring) return
            
            frameCount++
            totalFrameCount++
            
            // Detect jank (frame took longer than threshold)
            if (lastFrameTime > 0) {
                val frameTimeMs = (frameTimeNanos - lastFrameTime) / 1_000_000f
                if (frameTimeMs > jankThresholdMs * 2) { // Missed frame
                    jankFrameCount++
                    Timber.tag(TAG).v("Jank detected: frame took %.2fms", frameTimeMs)
                }
            }
            lastFrameTime = frameTimeNanos
            
            val now = System.nanoTime()
            val elapsedNs = now - lastSecondTime
            
            if (elapsedNs >= ONE_SECOND_NS) {
                val fps = frameCount
                _currentFps.value = fps
                
                // Update history
                fpsHistory.add(fps)
                if (fpsHistory.size > MAX_HISTORY_SIZE) {
                    fpsHistory.removeAt(0)
                }
                
                // Update stats
                updateStats(fps)
                
                frameCount = 0
                lastSecondTime = now
            }
            
            choreographer.postFrameCallback(this)
        }
    }
    
    fun start(fpsThreshold: Int = 30, refreshRateHz: Int = 60) {
        if (isMonitoring) return
        
        this.fpsThreshold = fpsThreshold
        this.jankThresholdMs = 1000f / refreshRateHz
        
        mainHandler.post {
            isMonitoring = true
            frameCount = 0
            lastSecondTime = System.nanoTime()
            lastFrameTime = 0L
            fpsHistory.clear()
            jankFrameCount = 0
            totalFrameCount = 0
            
            choreographer.postFrameCallback(frameCallback)
            Timber.tag(TAG).d("FPS monitoring started (threshold: $fpsThreshold, refresh: ${refreshRateHz}Hz)")
        }
    }
    
    fun stop() {
        if (!isMonitoring) return
        
        mainHandler.post {
            isMonitoring = false
            choreographer.removeFrameCallback(frameCallback)
            Timber.tag(TAG).d("FPS monitoring stopped")
        }
    }
    
    private fun updateStats(currentFps: Int) {
        if (fpsHistory.isEmpty()) return
        
        val avg = fpsHistory.average().toFloat()
        val min = fpsHistory.minOrNull() ?: 0
        val max = fpsHistory.maxOrNull() ?: 0
        val dropsBelow30 = fpsHistory.count { it < fpsThreshold }
        val jankPercent = if (totalFrameCount > 0) {
            (jankFrameCount.toFloat() / totalFrameCount * 100)
        } else 0f
        
        _fpsStats.value = FpsStats(
            current = currentFps,
            average = avg,
            min = min,
            max = max,
            frameDrops = dropsBelow30,
            jankPercent = jankPercent,
            status = FpsStatus.fromFps(currentFps, fpsThreshold)
        )
    }
    
    fun getStatus(): FpsStatus = FpsStatus.fromFps(_currentFps.value, fpsThreshold)
    
    fun reset() {
        fpsHistory.clear()
        jankFrameCount = 0
        totalFrameCount = 0
        _fpsStats.value = FpsStats.EMPTY
        _currentFps.value = 0
    }
    
    companion object {
        private const val TAG = "FPS_MONITOR"
        private const val ONE_SECOND_NS = 1_000_000_000L
        private const val MAX_HISTORY_SIZE = 60 // 1 minute of data
    }
}

data class FpsStats(
    val current: Int,
    val average: Float,
    val min: Int,
    val max: Int,
    val frameDrops: Int,
    val jankPercent: Float,
    val status: FpsStatus
) {
    companion object {
        val EMPTY = FpsStats(0, 0f, 0, 0, 0, 0f, FpsStatus.UNKNOWN)
    }
}

enum class FpsStatus(val displayName: String, val emoji: String) {
    SMOOTH("Smooth", "🟢"),
    ACCEPTABLE("Acceptable", "🟡"),
    NEEDS_OPTIMIZATION("Needs optimization", "🟠"),
    LAG_DETECTED("Lag detected", "🔴"),
    UNKNOWN("Unknown", "⚪");
    
    companion object {
        fun fromFps(fps: Int, threshold: Int = 30): FpsStatus = when {
            fps >= 55 -> SMOOTH
            fps >= 45 -> ACCEPTABLE
            fps >= threshold -> NEEDS_OPTIMIZATION
            fps > 0 -> LAG_DETECTED
            else -> UNKNOWN
        }
    }
}
