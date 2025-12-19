package com.sysmetrics.app.ui.network

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt
import com.sysmetrics.app.data.model.network.NetworkDisplayMode
import com.sysmetrics.app.data.model.network.NetworkTrafficStats
import com.sysmetrics.app.data.model.network.NetworkTypeInfo
import com.sysmetrics.app.data.model.network.PerAppTrafficStats
import timber.log.Timber

/**
 * Custom overlay view for displaying network traffic statistics.
 * Supports multiple display modes and real-time updates.
 *
 * ## Display Modes:
 * - **Compact**: "↓ 2.5M | ↑ 0.8M"
 * - **Extended**: Multi-line with peak values
 * - **Per-App**: Top 3 apps by traffic
 * - **Combined**: With other system metrics
 *
 * ## Performance Optimizations:
 * - Minimal object allocations in onDraw
 * - Pre-computed text bounds
 * - Hardware acceleration compatible
 */
class NetworkOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "NET_OVERLAY_VIEW"

        // Default colors
        private const val DEFAULT_TEXT_COLOR = Color.WHITE
        private const val DEFAULT_INGRESS_COLOR = 0xFF4CAF50.toInt() // Green
        private const val DEFAULT_EGRESS_COLOR = 0xFF2196F3.toInt() // Blue
        private const val DEFAULT_BACKGROUND_COLOR = 0x99000000.toInt() // Semi-transparent black

        // Default sizes (in sp/dp)
        private const val DEFAULT_TEXT_SIZE_SP = 12f
        private const val DEFAULT_PADDING_DP = 8f
        private const val DEFAULT_LINE_SPACING_DP = 4f
    }

    // Paint objects (reused to avoid allocations)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = DEFAULT_TEXT_COLOR
        textSize = DEFAULT_TEXT_SIZE_SP * resources.displayMetrics.scaledDensity
        typeface = Typeface.MONOSPACE
    }

    private val ingressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = DEFAULT_INGRESS_COLOR
        textSize = DEFAULT_TEXT_SIZE_SP * resources.displayMetrics.scaledDensity
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    }

    private val egressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = DEFAULT_EGRESS_COLOR
        textSize = DEFAULT_TEXT_SIZE_SP * resources.displayMetrics.scaledDensity
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    }

    private val backgroundPaint = Paint().apply {
        color = DEFAULT_BACKGROUND_COLOR
        style = Paint.Style.FILL
    }

    // Dimensions
    private val padding = DEFAULT_PADDING_DP * resources.displayMetrics.density
    private val lineSpacing = DEFAULT_LINE_SPACING_DP * resources.displayMetrics.density

    // State
    private var displayMode: NetworkDisplayMode = NetworkDisplayMode.COMPACT
    private var trafficStats: NetworkTrafficStats = NetworkTrafficStats.EMPTY
    private var networkType: NetworkTypeInfo = NetworkTypeInfo.DISCONNECTED
    private var perAppStats: List<PerAppTrafficStats> = emptyList()

    // Cached display strings (updated when data changes)
    private var displayLines: List<DisplayLine> = emptyList()

    // Line height calculated from text size
    private val lineHeight: Float
        get() = textPaint.textSize + lineSpacing

    /**
     * Display line with color information.
     */
    private data class DisplayLine(
        val text: String,
        val paint: Paint
    )

    /**
     * Updates traffic statistics and triggers redraw.
     */
    fun updateTrafficStats(stats: NetworkTrafficStats) {
        trafficStats = stats
        updateDisplayLines()
        invalidate()
    }

    /**
     * Updates network type info and triggers redraw.
     */
    fun updateNetworkType(type: NetworkTypeInfo) {
        networkType = type
        updateDisplayLines()
        invalidate()
    }

    /**
     * Updates per-app statistics and triggers redraw.
     */
    fun updatePerAppStats(stats: List<PerAppTrafficStats>) {
        perAppStats = stats
        updateDisplayLines()
        invalidate()
    }

    /**
     * Updates all data at once (more efficient).
     */
    fun updateAll(
        traffic: NetworkTrafficStats,
        type: NetworkTypeInfo,
        apps: List<PerAppTrafficStats>
    ) {
        trafficStats = traffic
        networkType = type
        perAppStats = apps
        updateDisplayLines()
        invalidate()
    }

    /**
     * Sets display mode.
     */
    fun setDisplayMode(mode: NetworkDisplayMode) {
        if (displayMode != mode) {
            displayMode = mode
            updateDisplayLines()
            requestLayout()
            invalidate()
        }
    }

    /**
     * Gets current display mode.
     */
    fun getDisplayMode(): NetworkDisplayMode = displayMode

    /**
     * Cycles through display modes.
     */
    fun cycleDisplayMode() {
        val modes = NetworkDisplayMode.values()
        val nextIndex = (modes.indexOf(displayMode) + 1) % modes.size
        setDisplayMode(modes[nextIndex])
    }

    /**
     * Sets text color.
     */
    fun setTextColor(@ColorInt color: Int) {
        textPaint.color = color
        invalidate()
    }

    /**
     * Sets ingress (download) highlight color.
     */
    fun setIngressColor(@ColorInt color: Int) {
        ingressPaint.color = color
        invalidate()
    }

    /**
     * Sets egress (upload) highlight color.
     */
    fun setEgressColor(@ColorInt color: Int) {
        egressPaint.color = color
        invalidate()
    }

    /**
     * Sets background color.
     */
    fun setOverlayBackgroundColor(@ColorInt color: Int) {
        backgroundPaint.color = color
        invalidate()
    }

    /**
     * Sets text size in SP.
     */
    fun setTextSizeSp(sizeSp: Float) {
        val sizePixels = sizeSp * resources.displayMetrics.scaledDensity
        textPaint.textSize = sizePixels
        ingressPaint.textSize = sizePixels
        egressPaint.textSize = sizePixels
        updateDisplayLines()
        requestLayout()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = calculateDesiredWidth()
        val desiredHeight = calculateDesiredHeight()

        val width = resolveSize(desiredWidth.toInt(), widthMeasureSpec)
        val height = resolveSize(desiredHeight.toInt(), heightMeasureSpec)

        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw background
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

        // Draw lines
        var y = padding + textPaint.textSize
        for (line in displayLines) {
            canvas.drawText(line.text, padding, y, line.paint)
            y += lineHeight
        }
    }

    /**
     * Updates cached display lines based on current mode and data.
     */
    private fun updateDisplayLines() {
        displayLines = when (displayMode) {
            NetworkDisplayMode.COMPACT -> buildCompactLines()
            NetworkDisplayMode.EXTENDED -> buildExtendedLines()
            NetworkDisplayMode.PER_APP -> buildPerAppLines()
            NetworkDisplayMode.COMBINED -> buildCombinedLines()
        }
    }

    private fun buildCompactLines(): List<DisplayLine> {
        val ingressText = formatShortSpeed(trafficStats.ingressBytesPerSec, "↓")
        val egressText = formatShortSpeed(trafficStats.egressBytesPerSec, "↑")
        return listOf(
            DisplayLine("$ingressText | $egressText", textPaint)
        )
    }

    private fun buildExtendedLines(): List<DisplayLine> {
        return listOf(
            DisplayLine(
                "↓ ${formatSpeed(trafficStats.ingressBytesPerSec)} | Peak: ${formatMbps(trafficStats.peakIngressMbps)}",
                ingressPaint
            ),
            DisplayLine(
                "↑ ${formatSpeed(trafficStats.egressBytesPerSec)} | Peak: ${formatMbps(trafficStats.peakEgressMbps)}",
                egressPaint
            ),
            DisplayLine(
                "${networkType.type.shortName}: ${networkType.signalStrengthDbm ?: "—"}dBm",
                textPaint
            )
        )
    }

    private fun buildPerAppLines(): List<DisplayLine> {
        if (perAppStats.isEmpty()) {
            return listOf(DisplayLine("No app traffic", textPaint))
        }

        return perAppStats.take(3).map { app ->
            val name = app.appName.take(8).padEnd(8)
            val traffic = app.formatCompact()
            DisplayLine("$name $traffic", textPaint)
        }
    }

    private fun buildCombinedLines(): List<DisplayLine> {
        val trafficLine = "${formatShortSpeed(trafficStats.ingressBytesPerSec, "↓")} | ${formatShortSpeed(trafficStats.egressBytesPerSec, "↑")}"
        val networkLine = "${networkType.type.shortName}: ${networkType.signalStrengthDbm ?: "—"}dBm"

        return listOf(
            DisplayLine(trafficLine, textPaint),
            DisplayLine(networkLine, textPaint)
        )
    }

    private fun calculateDesiredWidth(): Float {
        if (displayLines.isEmpty()) return padding * 2

        val maxTextWidth = displayLines.maxOfOrNull { line ->
            line.paint.measureText(line.text)
        } ?: 0f

        return maxTextWidth + padding * 2
    }

    private fun calculateDesiredHeight(): Float {
        val lineCount = displayLines.size.coerceAtLeast(1)
        return (lineCount * lineHeight) + padding * 2
    }

    private fun formatSpeed(bytesPerSec: Long): String {
        return when {
            bytesPerSec < 1024 -> "$bytesPerSec B/s"
            bytesPerSec < 1024 * 1024 -> "%.1f KB/s".format(bytesPerSec / 1024f)
            else -> "%.2f MB/s".format(bytesPerSec / (1024f * 1024f))
        }
    }

    private fun formatShortSpeed(bytesPerSec: Long, prefix: String): String {
        return when {
            bytesPerSec < 1024 -> "$prefix${bytesPerSec}B"
            bytesPerSec < 1024 * 1024 -> "$prefix%.0fK".format(bytesPerSec / 1024f)
            else -> "$prefix%.1fM".format(bytesPerSec / (1024f * 1024f))
        }
    }

    private fun formatMbps(mbps: Float): String {
        return when {
            mbps < 0.1f -> "0"
            mbps < 10f -> "%.1f".format(mbps)
            else -> "%.0f".format(mbps)
        } + " Mbps"
    }

    init {
        updateDisplayLines()
    }
}
