package com.sysmetrics.app.native_bridge

import com.sysmetrics.app.domain.collector.ICpuMetricsCollector
import com.sysmetrics.app.domain.formatter.IStringFormatter

/**
 * Factory for creating metrics collectors with automatic fallback.
 * Provides native implementations when available, fallback otherwise.
 */
class MetricsCollectorFactory(
    private val nativeCpuCollector: NativeCpuMetricsCollector,
    private val fallbackCpuCollector: FallbackCpuMetricsCollector,
    private val nativeFormatter: NativeStringFormatter,
    private val fallbackFormatter: FallbackStringFormatter
) {

    /**
     * Get the best available CPU metrics collector.
     * @return native collector if available, fallback otherwise
     */
    fun createCpuCollector(): ICpuMetricsCollector {
        return if (nativeCpuCollector.isNativeAvailable()) {
            nativeCpuCollector
        } else {
            fallbackCpuCollector
        }
    }

    /**
     * Get the best available string formatter.
     * @return native formatter if available, fallback otherwise
     */
    fun createStringFormatter(): IStringFormatter {
        return if (nativeFormatter.isNativeAvailable()) {
            nativeFormatter
        } else {
            fallbackFormatter
        }
    }
}
