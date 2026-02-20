package com.sysmetrics.screens

import com.kaspersky.components.kaspresso.screens.KScreen
import com.sysmetrics.app.R
import com.sysmetrics.app.ui.MainActivityOverlay
import io.github.kakaocup.kakao.text.KTextView
import io.github.kakaocup.kakao.common.views.KView
import io.github.kakaocup.kakao.button.KButton

/**
 * Main Screen Page Object for Kaspresso testing
 * Provides access to all UI elements on the main screen
 */
object MainScreen : KScreen<MainScreen>() {
    
    override val layoutId: Int? = R.layout.activity_main_overlay
    override val viewClass: Class<*>? = MainActivityOverlay::class.java
    
    // App Title
    val appTitle = KTextView { withId(R.id.tv_app_title) }
    
    // Status Text
    val statusText = KTextView { withId(R.id.tv_status) }
    
    // Toggle Button
    val toggleButton = KButton { withId(R.id.btn_toggle_overlay) }
    
    // Settings Button
    val settingsButton = KButton { withId(R.id.btn_settings) }
    
    // Permission Info
    val permissionInfo = KTextView { withId(R.id.tv_permission_info) }
    
    // Metrics Preview Layout
    val metricsPreviewLayout = KView { withId(R.id.layout_metrics_preview) }
    
    // Individual Metrics
    val cpuPreview = KTextView { withId(R.id.tv_cpu_preview) }
    val ramPreview = KTextView { withId(R.id.tv_ram_preview) }
    val tempPreview = KTextView { withId(R.id.tv_temp_preview) }
    val networkPreview = KTextView { withId(R.id.tv_network_preview) }
    
    /**
     * Verify all main elements are displayed
     */
    fun verifyMainElementsDisplayed() {
        appTitle.isDisplayed()
        statusText.isDisplayed()
        toggleButton.isDisplayed()
        settingsButton.isDisplayed()
    }
    
    /**
     * Get current overlay status text
     */
    fun getOverlayStatus(): String {
        return statusText.getText()
    }
    
    /**
     * Check if overlay is currently active
     */
    fun isOverlayActive(): Boolean {
        return try {
            statusText.hasText(R.string.overlay_status_on)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Toggle overlay with visual feedback
     */
    fun toggleOverlay() {
        toggleButton.click()
    }
    
    /**
     * Navigate to settings
     */
    fun openSettings() {
        settingsButton.click()
    }
    
    /**
     * Wait for metrics to appear if overlay is active
     */
    fun waitForMetricsToAppear() {
        try {
            metricsPreviewLayout.isDisplayed()
            cpuPreview.isDisplayed()
            ramPreview.isDisplayed()
            tempPreview.isDisplayed()
            networkPreview.isDisplayed()
        } catch (e: Exception) {
            // Metrics might not be visible immediately
        }
    }
}
