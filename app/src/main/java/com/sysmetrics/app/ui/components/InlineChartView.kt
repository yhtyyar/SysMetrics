package com.sysmetrics.app.ui.components

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.sysmetrics.app.data.model.advanced.ChartData
import com.sysmetrics.app.data.model.advanced.ChartDataPoint
import com.sysmetrics.app.data.model.advanced.MetricType
import com.sysmetrics.app.data.model.advanced.Severity

/**
 * Custom view for rendering inline sparkline charts.
 * Memory-efficient with smooth Bézier curve rendering.
 */
class InlineChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    private var chartData: ChartData = ChartData.empty(MetricType.CPU)
    private var chartHeight = 40 // dp
    
    // Paints
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f * resources.displayMetrics.density
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f
        color = Color.parseColor("#20000000")
    }
    
    // Paths
    private val linePath = Path()
    private val fillPath = Path()
    
    // Colors for different severity levels
    private val colorLow = Color.parseColor("#4CAF50")      // Green
    private val colorMedium = Color.parseColor("#FFC107")   // Yellow
    private val colorHigh = Color.parseColor("#F44336")     // Red
    
    private var gradientShader: Shader? = null
    
    fun setData(data: ChartData) {
        chartData = data
        updateGradient()
        invalidate()
    }
    
    fun setChartHeightDp(heightDp: Int) {
        chartHeight = heightDp
        requestLayout()
    }
    
    private fun updateGradient() {
        if (width > 0 && height > 0) {
            val avgSeverity = chartData.points.map { it.severity }.groupBy { it }
                .maxByOrNull { it.value.size }?.key ?: Severity.LOW
            
            val baseColor = when (avgSeverity) {
                Severity.LOW -> colorLow
                Severity.MEDIUM -> colorMedium
                Severity.HIGH -> colorHigh
            }
            
            gradientShader = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                Color.argb(80, Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor)),
                Color.argb(10, Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor)),
                Shader.TileMode.CLAMP
            )
        }
    }
    
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredHeight = (chartHeight * resources.displayMetrics.density).toInt()
        val height = resolveSize(desiredHeight, heightMeasureSpec)
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY))
    }
    
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateGradient()
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (chartData.isEmpty) {
            drawEmptyState(canvas)
            return
        }
        
        drawGrid(canvas)
        drawChart(canvas)
    }
    
    private fun drawEmptyState(canvas: Canvas) {
        gridPaint.color = Color.parseColor("#30000000")
        val centerY = height / 2f
        canvas.drawLine(0f, centerY, width.toFloat(), centerY, gridPaint)
    }
    
    private fun drawGrid(canvas: Canvas) {
        // Draw horizontal grid lines
        val lineCount = 3
        for (i in 0..lineCount) {
            val y = height * i / lineCount.toFloat()
            canvas.drawLine(0f, y, width.toFloat(), y, gridPaint)
        }
    }
    
    private fun drawChart(canvas: Canvas) {
        val points = chartData.points
        if (points.size < 2) return
        
        linePath.reset()
        fillPath.reset()
        
        val padding = 4f * resources.displayMetrics.density
        val chartWidth = width - 2 * padding
        val chartHeight = height - 2 * padding
        
        val minVal = chartData.minValue
        val maxVal = chartData.maxValue
        val range = (maxVal - minVal).coerceAtLeast(1f)
        
        // Calculate points
        val pointCoords = points.mapIndexed { index, point ->
            val x = padding + (index.toFloat() / (points.size - 1)) * chartWidth
            val normalizedY = (point.value - minVal) / range
            val y = padding + chartHeight * (1 - normalizedY)
            PointF(x, y)
        }
        
        // Draw using Bézier curves for smoothness
        if (pointCoords.isNotEmpty()) {
            linePath.moveTo(pointCoords[0].x, pointCoords[0].y)
            fillPath.moveTo(pointCoords[0].x, height.toFloat())
            fillPath.lineTo(pointCoords[0].x, pointCoords[0].y)
            
            for (i in 1 until pointCoords.size) {
                val prev = pointCoords[i - 1]
                val curr = pointCoords[i]
                
                // Control points for Bézier curve
                val midX = (prev.x + curr.x) / 2
                
                linePath.cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
                fillPath.cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
            }
            
            // Close fill path
            fillPath.lineTo(pointCoords.last().x, height.toFloat())
            fillPath.close()
            
            // Draw fill with gradient
            fillPaint.shader = gradientShader
            canvas.drawPath(fillPath, fillPaint)
            
            // Draw line with color based on latest severity
            val latestSeverity = points.lastOrNull()?.severity ?: Severity.LOW
            linePaint.color = when (latestSeverity) {
                Severity.LOW -> colorLow
                Severity.MEDIUM -> colorMedium
                Severity.HIGH -> colorHigh
            }
            canvas.drawPath(linePath, linePaint)
        }
    }
}
