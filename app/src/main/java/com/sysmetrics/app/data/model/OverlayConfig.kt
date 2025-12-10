package com.sysmetrics.app.data.model

/**
 * Configuration for overlay appearance and behavior.
 */
data class OverlayConfig(
    val positionX: Int = 20,
    val positionY: Int = 20,
    val position: OverlayPosition = OverlayPosition.TOP_LEFT,
    val updateIntervalMs: Long = 1000L,
    val opacity: Float = 0.85f,
    val showCpu: Boolean = true,
    val showRam: Boolean = true,
    val showTemperature: Boolean = true
) {
    companion object {
        val DEFAULT = OverlayConfig()
    }
}

/**
 * Predefined overlay positions.
 */
enum class OverlayPosition {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT
}
