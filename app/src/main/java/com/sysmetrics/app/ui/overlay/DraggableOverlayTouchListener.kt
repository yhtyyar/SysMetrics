package com.sysmetrics.app.ui.overlay

import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import timber.log.Timber

/**
 * Touch listener for dragging overlay on mobile devices.
 * Clean implementation for smooth drag & drop with position saving.
 * 
 * Usage:
 * ```kotlin
 * val dragListener = DraggableOverlayTouchListener(
 *     params = layoutParams,
 *     windowManager = windowManager,
 *     onPositionChanged = { x, y -> savePosition(x, y) }
 * )
 * overlayView.setOnTouchListener(dragListener)
 * ```
 */
class DraggableOverlayTouchListener(
    private val params: WindowManager.LayoutParams,
    private val windowManager: WindowManager,
    private val onPositionChanged: ((x: Int, y: Int) -> Unit)? = null
) : View.OnTouchListener {
    
    companion object {
        private const val TAG = "OVERLAY_DRAG"
        private const val CLICK_THRESHOLD_MS = 200L
        private const val MOVE_THRESHOLD_PX = 10
    }
    
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var touchDownTime = 0L
    private var hasMoved = false
    
    override fun onTouch(view: View, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Save initial position
                initialX = params.x
                initialY = params.y
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                touchDownTime = System.currentTimeMillis()
                hasMoved = false
                
                Timber.tag(TAG).v("Touch down at (%.1f, %.1f)", event.rawX, event.rawY)
                return true
            }
            
            MotionEvent.ACTION_MOVE -> {
                // Calculate delta
                val deltaX = (event.rawX - initialTouchX).toInt()
                val deltaY = (event.rawY - initialTouchY).toInt()
                
                // Check if moved beyond threshold
                if (!hasMoved && (kotlin.math.abs(deltaX) > MOVE_THRESHOLD_PX || 
                                  kotlin.math.abs(deltaY) > MOVE_THRESHOLD_PX)) {
                    hasMoved = true
                }
                
                // Update position
                params.x = initialX + deltaX
                params.y = initialY + deltaY
                
                // Update overlay position
                try {
                    windowManager.updateViewLayout(view, params)
                } catch (e: IllegalArgumentException) {
                    Timber.tag(TAG).w("View not attached during drag")
                } catch (e: Exception) {
                    Timber.tag(TAG).e(e, "Error updating view during drag")
                }
                return true
            }
            
            MotionEvent.ACTION_UP -> {
                val duration = System.currentTimeMillis() - touchDownTime
                
                if (hasMoved) {
                    // Was a drag
                    Timber.tag(TAG).d("Overlay dragged to position (%d, %d)", params.x, params.y)
                    onPositionChanged?.invoke(params.x, params.y)
                } else if (duration < CLICK_THRESHOLD_MS) {
                    // Was a click - perform click action
                    view.performClick()
                }
                
                return true
            }
            
            MotionEvent.ACTION_CANCEL -> {
                Timber.tag(TAG).d("Touch cancelled")
                return true
            }
        }
        return false
    }
}
