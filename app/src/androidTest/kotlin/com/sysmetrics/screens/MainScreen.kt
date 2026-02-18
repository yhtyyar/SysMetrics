package com.sysmetrics.screens

import com.kaspersky.kaspresso.screens.KScreen
import com.kaspersky.kaspresso.testcases.core.testcontext.TestContext
import io.github.kakaocup.kakao.common.views.KView
import io.github.kakaocup.kakao.text.KTextView
import io.github.kakaocup.kakao.text.KButton
import com.sysmetrics.app.R

/**
 * Page Object for Main Activity Screen
 * Represents the main screen with toggle button and metrics preview
 */
object MainScreen : KScreen<MainScreen>() {

    override val layoutId: Int = R.layout.activity_main_overlay
    override val viewClass: Class<*> = com.sysmetrics.app.ui.MainActivityOverlay::class.java

    // App Title
    val appTitle = KTextView { withId(R.id.tv_app_title) }

    // Status Text
    val statusText = KTextView { withId(R.id.tv_status) }

    // Toggle Overlay Button
    val toggleButton = KButton { withId(R.id.btn_toggle_overlay) }

    // Settings Button
    val settingsButton = KButton { withId(R.id.btn_settings) }

    // Permission Info Text
    val permissionInfo = KTextView { withId(R.id.tv_permission_info) }

    // Metrics Preview Container
    val metricsPreviewLayout = KView { withId(R.id.layout_metrics_preview) }

    // CPU Preview
    val cpuPreview = KTextView { withId(R.id.tv_cpu_preview) }

    // RAM Preview
    val ramPreview = KTextView { withId(R.id.tv_ram_preview) }

    // Temperature Preview
    val tempPreview = KTextView { withId(R.id.tv_temp_preview) }

    // Network Preview
    val networkPreview = KTextView { withId(R.id.tv_network_preview) }

    /**
     * Check if metrics preview is visible
     */
    fun TestContext<*>.verifyMetricsPreviewVisible() {
        step("Verify metrics preview is visible") {
            metricsPreviewLayout {
                isVisible()
            }
            cpuPreview { isVisible() }
            ramPreview { isVisible() }
            tempPreview { isVisible() }
            networkPreview { isVisible() }
        }
    }

    /**
     * Check if metrics preview is hidden
     */
    fun TestContext<*>.verifyMetricsPreviewHidden() {
        step("Verify metrics preview is hidden") {
            metricsPreviewLayout {
                isGone()
            }
        }
    }

    /**
     * Verify status text shows overlay is ON
     */
    fun TestContext<*>.verifyOverlayStatusOn() {
        step("Verify overlay status is ON") {
            statusText {
                isVisible()
                hasText(R.string.overlay_status_on)
            }
            toggleButton {
                hasText(R.string.stop_overlay)
            }
        }
    }

    /**
     * Verify status text shows overlay is OFF
     */
    fun TestContext<*>.verifyOverlayStatusOff() {
        step("Verify overlay status is OFF") {
            statusText {
                isVisible()
                hasText(R.string.overlay_status_off)
            }
            toggleButton {
                hasText(R.string.start_overlay)
            }
        }
    }
}
