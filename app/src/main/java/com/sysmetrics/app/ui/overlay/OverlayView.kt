package com.sysmetrics.app.ui.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.TypedValue
import android.view.View
import com.sysmetrics.app.data.model.OverlayConfig
import com.sysmetrics.app.data.model.SystemMetrics

/**
 * Custom View for rendering system metrics overlay.
 * Optimized for minimal allocations during draw operations.
 */
class OverlayView(context: Context) : View(context) {

    private var metrics: SystemMetrics = SystemMetrics.EMPTY
    private var config: OverlayConfig = OverlayConfig.DEFAULT

    // Pre-allocated paint objects
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(200, 0, 0, 0)
        style = Paint.Style.FILL
    }

    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = spToPx(12f)
        isFakeBoldText = true
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#80DEEA")
        textSize = spToPx(11f)
    }

    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = spToPx(13f)
    }

    private val progressBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#33FFFFFF")
        style = Paint.Style.FILL
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4DD0E1")
        style = Paint.Style.FILL
    }

    // Pre-allocated rect for progress bar
    private val progressRect = RectF()
    private val backgroundRect = RectF()

    // Pre-allocated StringBuilder for formatting
    private val stringBuilder = StringBuilder(32)

    /**
     * Updates displayed metrics and triggers redraw.
     */
    fun updateMetrics(newMetrics: SystemMetrics) {
        if (metrics != newMetrics) {
            metrics = newMetrics
            invalidate()
        }
    }

    /**
     * Updates overlay configuration.
     */
    fun updateConfig(newConfig: OverlayConfig) {
        if (config != newConfig) {
            config = newConfig
            val alpha = (newConfig.opacity * 255).toInt()
            backgroundPaint.color = Color.argb(alpha, 0, 0, 0)
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw background
        backgroundRect.set(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawRoundRect(backgroundRect, 12f, 12f, backgroundPaint)

        var yOffset = PADDING + titlePaint.textSize

        // Title
        canvas.drawText("SysMetrics", PADDING, yOffset, titlePaint)
        yOffset += LINE_SPACING

        // CPU
        if (config.showCpu) {
            yOffset = drawMetric(
                canvas, "CPU",
                formatPercent(metrics.cpuUsage),
                metrics.cpuUsage / 100f,
                getCpuColor(metrics.cpuUsage),
                yOffset
            )
        }

        // RAM
        if (config.showRam) {
            yOffset = drawMetric(
                canvas, "RAM",
                formatMemory(metrics.ramUsedMb, metrics.ramTotalMb),
                metrics.ramUsagePercent / 100f,
                getRamColor(metrics.ramUsagePercent),
                yOffset
            )
        }

        // Temperature
        if (config.showTemperature && metrics.temperatureCelsius > 0f) {
            yOffset = drawTemperature(canvas, metrics.temperatureCelsius, yOffset)
        }

        // Cores info
        canvas.drawText(
            "Cores: ${metrics.cpuCores}",
            PADDING, yOffset + labelPaint.textSize,
            labelPaint
        )
    }

    private fun drawMetric(
        canvas: Canvas,
        label: String,
        value: String,
        progress: Float,
        progressColor: Int,
        yOffset: Float
    ): Float {
        var y = yOffset

        // Label
        canvas.drawText(label, PADDING, y, labelPaint)
        y += labelPaint.textSize + 4f

        // Progress bar background
        val progressWidth = width - PADDING * 2
        progressRect.set(PADDING, y, PADDING + progressWidth, y + PROGRESS_HEIGHT)
        canvas.drawRoundRect(progressRect, 4f, 4f, progressBackgroundPaint)

        // Progress bar fill
        progressPaint.color = progressColor
        progressRect.set(PADDING, y, PADDING + progressWidth * progress.coerceIn(0f, 1f), y + PROGRESS_HEIGHT)
        canvas.drawRoundRect(progressRect, 4f, 4f, progressPaint)
        y += PROGRESS_HEIGHT + 4f

        // Value
        canvas.drawText(value, PADDING, y + valuePaint.textSize, valuePaint)
        y += valuePaint.textSize + LINE_SPACING

        return y
    }

    private fun drawTemperature(canvas: Canvas, temp: Float, yOffset: Float): Float {
        var y = yOffset

        canvas.drawText("TEMP", PADDING, y, labelPaint)
        y += labelPaint.textSize + 4f

        val tempStr = formatTemperature(temp)
        valuePaint.color = getTemperatureColor(temp)
        canvas.drawText(tempStr, PADDING, y + valuePaint.textSize, valuePaint)
        valuePaint.color = Color.WHITE
        y += valuePaint.textSize + LINE_SPACING

        return y
    }

    private fun formatPercent(value: Float): String {
        stringBuilder.clear()
        stringBuilder.append(String.format("%.1f", value))
        stringBuilder.append("%")
        return stringBuilder.toString()
    }

    private fun formatMemory(usedMb: Long, totalMb: Long): String {
        stringBuilder.clear()
        stringBuilder.append(usedMb)
        stringBuilder.append(" / ")
        stringBuilder.append(totalMb)
        stringBuilder.append(" MB")
        return stringBuilder.toString()
    }

    private fun formatTemperature(celsius: Float): String {
        stringBuilder.clear()
        stringBuilder.append(String.format("%.0f", celsius))
        stringBuilder.append("°C")
        return stringBuilder.toString()
    }

    private fun getCpuColor(usage: Float): Int = when {
        usage >= 90f -> Color.parseColor("#EF5350")  // Red
        usage >= 70f -> Color.parseColor("#FFA726")  // Orange
        usage >= 50f -> Color.parseColor("#FFEE58")  // Yellow
        else -> Color.parseColor("#66BB6A")          // Green
    }

    private fun getRamColor(usage: Float): Int = when {
        usage >= 90f -> Color.parseColor("#EF5350")
        usage >= 75f -> Color.parseColor("#FFA726")
        else -> Color.parseColor("#42A5F5")          // Blue
    }

    private fun getTemperatureColor(celsius: Float): Int = when {
        celsius >= 80f -> Color.parseColor("#EF5350")
        celsius >= 60f -> Color.parseColor("#FFA726")
        else -> Color.parseColor("#66BB6A")
    }

    private fun spToPx(sp: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            sp,
            resources.displayMetrics
        )
    }

    companion object {
        private const val PADDING = 16f
        private const val LINE_SPACING = 12f
        private const val PROGRESS_HEIGHT = 8f
    }
}
