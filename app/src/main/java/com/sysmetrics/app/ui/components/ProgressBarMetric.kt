package com.sysmetrics.app.ui.components

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.sysmetrics.app.R

/**
 * Custom progress bar with dynamic color based on value
 * Optimized for TV viewing with rounded corners
 */
class ProgressBarMetric @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var progress: Int = 0
    private var animatedProgress: Float = 0f
    private var progressColor: Int = ContextCompat.getColor(context, R.color.metric_success)
    
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.progress_background)
        style = Paint.Style.FILL
    }
    
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = progressColor
        style = Paint.Style.FILL
    }
    
    private val rectF = RectF()
    private val cornerRadius: Float = context.resources.getDimension(R.dimen.progress_bar_corner_radius)
    
    private var animator: ValueAnimator? = null

    init {
        // Set minimum height
        minimumHeight = context.resources.getDimensionPixelSize(R.dimen.progress_bar_height)
    }

    /**
     * Set progress value (0-100)
     */
    fun setProgress(progress: Int) {
        val newProgress = progress.coerceIn(0, 100)
        if (this.progress != newProgress) {
            animateProgress(this.progress, newProgress)
            this.progress = newProgress
        }
    }

    /**
     * Set progress color
     */
    fun setProgressColor(color: Int) {
        this.progressColor = color
        progressPaint.color = color
        invalidate()
    }

    /**
     * Animate progress change
     */
    private fun animateProgress(from: Int, to: Int) {
        animator?.cancel()
        
        animator = ValueAnimator.ofFloat(from.toFloat(), to.toFloat()).apply {
            duration = 200L
            
            addUpdateListener { animation ->
                animatedProgress = animation.animatedValue as Float
                invalidate()
            }
            
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val width = width.toFloat()
        val height = height.toFloat()
        
        // Draw background
        rectF.set(0f, 0f, width, height)
        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, backgroundPaint)
        
        // Draw progress
        if (animatedProgress > 0) {
            val progressWidth = (width * animatedProgress / 100f).coerceAtLeast(cornerRadius * 2)
            rectF.set(0f, 0f, progressWidth, height)
            canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, progressPaint)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val height = context.resources.getDimensionPixelSize(R.dimen.progress_bar_height)
        setMeasuredDimension(
            getDefaultSize(suggestedMinimumWidth, widthMeasureSpec),
            height
        )
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }
}
