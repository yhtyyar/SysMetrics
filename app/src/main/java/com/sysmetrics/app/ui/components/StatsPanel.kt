package com.sysmetrics.app.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import com.sysmetrics.app.R
import com.sysmetrics.app.data.model.advanced.TimeWindowStats
import java.util.Locale

/**
 * Panel displaying aggregated statistics for a metric.
 * Shows current value, averages, min/max, and percentiles.
 */
class StatsPanel @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    
    private val tvCurrent: TextView
    private val tvAvg30s: TextView
    private val tvAvg1m: TextView
    private val tvAvg5m: TextView
    private val tvMin: TextView
    private val tvMax: TextView
    private val tvP95: TextView
    private val tvP99: TextView
    private val tvTitle: TextView
    
    private var show30s = true
    private var show1m = true
    private var show5m = true
    private var showPercentiles = false
    
    init {
        orientation = VERTICAL
        LayoutInflater.from(context).inflate(R.layout.view_stats_panel, this, true)
        
        tvTitle = findViewById(R.id.tv_stats_title)
        tvCurrent = findViewById(R.id.tv_current)
        tvAvg30s = findViewById(R.id.tv_avg_30s)
        tvAvg1m = findViewById(R.id.tv_avg_1m)
        tvAvg5m = findViewById(R.id.tv_avg_5m)
        tvMin = findViewById(R.id.tv_min)
        tvMax = findViewById(R.id.tv_max)
        tvP95 = findViewById(R.id.tv_p95)
        tvP99 = findViewById(R.id.tv_p99)
    }
    
    fun setStats(stats: TimeWindowStats) {
        tvTitle.text = stats.metricType.displayName
        val unit = stats.metricType.unit
        
        tvCurrent.text = formatValue(stats.current, unit)
        tvAvg30s.text = formatValue(stats.avg30s, unit)
        tvAvg1m.text = formatValue(stats.avg1m, unit)
        tvAvg5m.text = formatValue(stats.avg5m, unit)
        tvMin.text = formatValue(stats.min, unit)
        tvMax.text = formatValue(stats.max, unit)
        tvP95.text = formatValue(stats.p95, unit)
        tvP99.text = formatValue(stats.p99, unit)
        
        updateVisibility()
    }
    
    fun setVisibilityOptions(show30s: Boolean, show1m: Boolean, show5m: Boolean, showPercentiles: Boolean) {
        this.show30s = show30s
        this.show1m = show1m
        this.show5m = show5m
        this.showPercentiles = showPercentiles
        updateVisibility()
    }
    
    private fun updateVisibility() {
        tvAvg30s.visibility = if (show30s) VISIBLE else GONE
        tvAvg1m.visibility = if (show1m) VISIBLE else GONE
        tvAvg5m.visibility = if (show5m) VISIBLE else GONE
        tvP95.visibility = if (showPercentiles) VISIBLE else GONE
        tvP99.visibility = if (showPercentiles) VISIBLE else GONE
    }
    
    private fun formatValue(value: Float, unit: String): String {
        return String.format(Locale.getDefault(), "%.1f%s", value, unit)
    }
}
