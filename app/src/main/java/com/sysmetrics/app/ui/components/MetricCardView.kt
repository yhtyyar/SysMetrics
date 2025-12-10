package com.sysmetrics.app.ui.components

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.sysmetrics.app.R

/**
 * Custom view for displaying system metrics (CPU, RAM, Temperature)
 * Optimized for Android TV with D-pad navigation support
 * 
 * Features:
 * - Focus state with visual feedback (border, shadow, scale)
 * - Dynamic color based on metric value (green/yellow/red)
 * - Animated progress bar
 * - Large text for 2m viewing distance
 */
class MetricCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    // UI Components
    private val iconView: ImageView
    private val nameTextView: TextView
    private val valueTextView: TextView
    private val progressBar: ProgressBarMetric
    private val percentTextView: TextView

    // State
    private var currentProgress: Int = 0
    private var currentColor: Int = Color.GREEN
    private var animator: ValueAnimator? = null

    init {
        // Inflate layout
        LayoutInflater.from(context).inflate(R.layout.view_metric_card, this, true)
        
        // Find views
        iconView = findViewById(R.id.metric_icon)
        nameTextView = findViewById(R.id.metric_name)
        valueTextView = findViewById(R.id.metric_value)
        progressBar = findViewById(R.id.metric_progress)
        percentTextView = findViewById(R.id.metric_percent)

        // Setup
        isFocusable = true
        isFocusableInTouchMode = true
        isClickable = true
        
        // Apply default style
        setBackgroundResource(R.drawable.bg_metric_card)
        elevation = context.resources.getDimension(R.dimen.card_elevation)
    }

    /**
     * Set metric data
     * @param name Metric name (e.g., "CPU Usage")
     * @param value Main value (e.g., "48.5%")
     * @param progress Progress percentage (0-100)
     * @param iconRes Icon resource ID
     */
    fun setMetric(
        name: String,
        value: String,
        progress: Int,
        iconRes: Int
    ) {
        nameTextView.text = name
        valueTextView.text = value
        iconView.setImageResource(iconRes)
        
        // Update progress with animation
        animateProgress(currentProgress, progress)
        currentProgress = progress
    }

    /**
     * Update only the value (for frequent updates)
     */
    fun updateValue(value: String, progress: Int) {
        valueTextView.text = value
        animateProgress(currentProgress, progress)
        currentProgress = progress
    }

    /**
     * Animate progress bar with color change
     */
    private fun animateProgress(from: Int, to: Int) {
        animator?.cancel()
        
        animator = ValueAnimator.ofInt(from, to).apply {
            duration = 300L
            interpolator = DecelerateInterpolator()
            
            addUpdateListener { animation ->
                val animatedProgress = animation.animatedValue as Int
                progressBar.setProgress(animatedProgress)
                percentTextView.text = "$animatedProgress%"
                
                // Update color based on progress
                val color = getColorForProgress(animatedProgress)
                if (color != currentColor) {
                    currentColor = color
                    progressBar.setProgressColor(color)
                    valueTextView.setTextColor(color)
                }
            }
            
            start()
        }
    }

    /**
     * Get color based on progress value
     * Green: < 50%, Yellow: 50-80%, Red: > 80%
     */
    private fun getColorForProgress(progress: Int): Int {
        return when {
            progress < 50 -> ContextCompat.getColor(context, R.color.metric_success)
            progress < 80 -> ContextCompat.getColor(context, R.color.metric_warning)
            else -> ContextCompat.getColor(context, R.color.metric_error)
        }
    }

    /**
     * Set custom icon color
     */
    fun setIconTint(color: Int) {
        iconView.setColorFilter(color)
    }

    /**
     * Handle focus changes for TV navigation
     */
    override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: android.graphics.Rect?) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
        
        // Animate scale and elevation on focus
        animate()
            .scaleX(if (gainFocus) 1.02f else 1.0f)
            .scaleY(if (gainFocus) 1.02f else 1.0f)
            .setDuration(200)
            .start()
        
        // Update elevation
        elevation = if (gainFocus) {
            context.resources.getDimension(R.dimen.focus_elevation)
        } else {
            context.resources.getDimension(R.dimen.card_elevation)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }
}
