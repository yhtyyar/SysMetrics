package com.sysmetrics.app.utils

import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import timber.log.Timber

/**
 * Touch listener for draggable overlay on mobile devices.
 * Allows user to move the overlay anywhere on the screen.
 */
class DraggableOverlayTouchListener(
    private val params: WindowManager.LayoutParams,
    private val windowManager: WindowManager,
    private val onPositionChanged: (x: Int, y: Int) -> Unit
) : View.OnTouchListener {

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false
    
    companion object {
        private const val TAG = "DRAG_OVERLAY"
        private const val DRAG_THRESHOLD = 10 // pixels
    }

    override fun onTouch(view: View, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Remember initial position
                initialX = params.x
                initialY = params.y
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                isDragging = false
                return true
            }
            
            MotionEvent.ACTION_MOVE -> {
                val deltaX = event.rawX - initialTouchX
                val deltaY = event.rawY - initialTouchY
                
                // Check if movement exceeds threshold
                if (!isDragging && (Math.abs(deltaX) > DRAG_THRESHOLD || Math.abs(deltaY) > DRAG_THRESHOLD)) {
                    isDragging = true
                }
                
                if (isDragging) {
                    // Update overlay position
                    params.x = (initialX + deltaX).toInt()
                    params.y = (initialY + deltaY).toInt()
                    
                    try {
                        windowManager.updateViewLayout(view, params)
                    } catch (e: Exception) {
                        Timber.tag(TAG).e(e, "Failed to update overlay position")
                    }
                }
                return true
            }
            
            MotionEvent.ACTION_UP -> {
                if (isDragging) {
                    // Save final position
                    Timber.tag(TAG).d("Overlay moved to position: (${params.x}, ${params.y})")
                    onPositionChanged(params.x, params.y)
                    isDragging = false
                    return true
                }
                return false
            }
            
            MotionEvent.ACTION_CANCEL -> {
                isDragging = false
                return false
            }
        }
        return false
    }
}
