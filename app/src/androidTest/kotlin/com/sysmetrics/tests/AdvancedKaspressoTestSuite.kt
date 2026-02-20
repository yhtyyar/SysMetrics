package com.sysmetrics.tests

import android.Manifest
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.kaspersky.components.kaspresso.testcases.api.testcase.TestCase
import com.sysmetrics.app.R
import com.sysmetrics.app.ui.MainActivityOverlay
import com.sysmetrics.config.KaspressoConfig
import com.sysmetrics.screens.MainScreen
import com.sysmetrics.screens.SettingsScreen
import com.sysmetrics.screens.OverlayPosition
import com.sysmetrics.screens.MetricType
import com.sysmetrics.utils.KaspressoUtils
import com.sysmetrics.utils.ScreenshotUtils
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Advanced Kaspresso Test Suite
 * Uses enhanced configuration and utilities
 * Implements comprehensive test scenarios with better error handling
 */
@RunWith(AndroidJUnit4::class)
class AdvancedKaspressoTestSuite : TestCase(KaspressoConfig.createVisualTestingConfigurator()) {

    @get:Rule(order = 0)
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.SYSTEM_ALERT_WINDOW
    )

    @get:Rule(order = 1)
    val activityRule = ActivityScenarioRule(MainActivityOverlay::class.java)

    @Before
    fun setup() {
        ScreenshotUtils.resetCounter()
        KaspressoUtils.captureScreenshot("setup", "initial_state", 0)
        KaspressoUtils.handleSystemDialogs()
    }

    @After
    fun teardown() {
        KaspressoUtils.captureScreenshot("teardown", "final_state", 99)
    }

    // ========== Enhanced TC-001: App Launch and Basic Elements Verification ==========
    @Test
    fun enhanced_tc001_appLaunchAndBasicElementsVerification() {
        run("Enhanced App Launch and Basic Elements Verification") {
            
            step("Launch app and verify main elements") {
                KaspressoUtils.captureScreenshot("tc001", "01_app_launched", 1)
                KaspressoUtils.delay(2000, "Wait for app to fully load")
                
                MainScreen {
                    appTitle.assertWithFeedback("App title displayed")
                    statusText.assertWithFeedback("Status text displayed")
                    toggleButton.assertWithFeedback("Toggle button displayed")
                    settingsButton.assertWithFeedback("Settings button displayed")
                    
                    // Verify specific text content
                    appTitle.hasText(R.string.app_name)
                }
                
                KaspressoUtils.captureScreenshot("tc001", "02_elements_verified", 2)
            }
            
            step("Verify initial overlay status") {
                KaspressoUtils.captureScreenshot("tc001", "03_status_check", 3)
                
                MainScreen {
                    statusText.hasText(R.string.overlay_status_off)
                    toggleButton.hasText(R.string.start_overlay)
                    
                    // Verify overlay is not active
                    val isActive = isOverlayActive()
                    assert(!isActive) { "Overlay should be inactive initially" }
                }
                
                KaspressoUtils.captureScreenshot("tc001", "04_status_verified", 4)
            }
            
            step("Verify settings button functionality") {
                KaspressoUtils.captureScreenshot("tc001", "05_settings_check", 5)
                
                MainScreen {
                    settingsButton.hasText(R.string.settings)
                    assert(KaspressoUtils.isElementEnabled(androidx.test.espresso.matcher.ViewMatchers.withId(R.id.btn_settings))) {
                        "Settings button should be enabled"
                    }
                }
                
                KaspressoUtils.captureScreenshot("tc001", "06_settings_verified", 6)
            }
            
            step("Verify permission info if present") {
                KaspressoUtils.safeInteraction("Check permission info") {
                    MainScreen.permissionInfo.isDisplayed()
                    KaspressoUtils.captureScreenshot("tc001", "07_permission_info", 7)
                }
            }
        }
    }

    // ========== Enhanced TC-002: Toggle Overlay Functionality Test ==========
    @Test
    fun enhanced_tc002_toggleOverlayFunctionalityTest() {
        run("Enhanced Toggle Overlay Functionality Test") {
            
            step("Verify initial overlay state") {
                KaspressoUtils.captureScreenshot("tc002", "01_initial_state", 1)
                
                MainScreen {
                    statusText.hasText(R.string.overlay_status_off)
                    toggleButton.hasText(R.string.start_overlay)
                    assert(!isOverlayActive()) { "Overlay should be inactive initially" }
                }
                
                KaspressoUtils.delay(1000, "Display initial state")
            }
            
            step("Start overlay with enhanced verification") {
                KaspressoUtils.captureScreenshot("tc002", "02_before_start", 2)
                
                MainScreen {
                    toggleButton.clickWithFeedback("Start overlay")
                }
                
                KaspressoUtils.delay(1000, "Wait for overlay start")
                KaspressoUtils.captureScreenshot("tc002", "03_overlay_starting", 3)
                
                // Verify overlay activation with retry logic
                var overlayActivated = false
                repeat(3) { attempt ->
                    KaspressoUtils.delay(2000, "Attempt ${attempt + 1} to verify overlay activation")
                    
                    MainScreen {
                        try {
                            statusText.hasText(R.string.overlay_status_on)
                            toggleButton.hasText(R.string.stop_overlay)
                            overlayActivated = isOverlayActive()
                            if (overlayActivated) return@repeat
                        } catch (e: Exception) {
                            KaspressoUtils.delay(1000, "Retry overlay activation check")
                        }
                    }
                }
                
                assert(overlayActivated) { "Overlay should be activated after start" }
                KaspressoUtils.captureScreenshot("tc002", "04_overlay_active", 4)
            }
            
            step("Verify metrics display with enhanced checks") {
                KaspressoUtils.captureScreenshot("tc002", "05_metrics_check", 5)
                
                MainScreen {
                    waitForMetricsToAppear()
                    KaspressoUtils.delay(2000, "Wait for metrics to load")
                    
                    // Verify individual metrics if available
                    KaspressoUtils.safeInteraction("Check CPU metric") {
                        cpuPreview.isDisplayed()
                    }
                    KaspressoUtils.safeInteraction("Check RAM metric") {
                        ramPreview.isDisplayed()
                    }
                    KaspressoUtils.safeInteraction("Check Temperature metric") {
                        tempPreview.isDisplayed()
                    }
                    KaspressoUtils.safeInteraction("Check Network metric") {
                        networkPreview.isDisplayed()
                    }
                }
                
                KaspressoUtils.captureScreenshot("tc002", "06_metrics_displayed", 6)
                KaspressoUtils.delay(3000, "Display metrics updating")
            }
            
            step("Stop overlay with enhanced verification") {
                KaspressoUtils.captureScreenshot("tc002", "07_before_stop", 7)
                
                MainScreen {
                    toggleButton.clickWithFeedback("Stop overlay")
                }
                
                KaspressoUtils.delay(1000, "Wait for overlay stop")
                KaspressoUtils.captureScreenshot("tc002", "08_overlay_stopping", 8)
                
                // Verify overlay deactivation with retry logic
                var overlayDeactivated = false
                repeat(3) { attempt ->
                    KaspressoUtils.delay(2000, "Attempt ${attempt + 1} to verify overlay deactivation")
                    
                    MainScreen {
                        try {
                            statusText.hasText(R.string.overlay_status_off)
                            toggleButton.hasText(R.string.start_overlay)
                            overlayDeactivated = !isOverlayActive()
                            if (overlayDeactivated) return@repeat
                        } catch (e: Exception) {
                            KaspressoUtils.delay(1000, "Retry overlay deactivation check")
                        }
                    }
                }
                
                assert(overlayDeactivated) { "Overlay should be deactivated after stop" }
                KaspressoUtils.captureScreenshot("tc002", "09_overlay_stopped", 9)
                KaspressoUtils.delay(2000, "Display stopped overlay")
            }
        }
    }

    // ========== Enhanced TC-003: Settings Navigation and Elements Verification ==========
    @Test
    fun enhanced_tc003_settingsNavigationAndElementsVerification() {
        run("Enhanced Settings Navigation and Elements Verification") {
            
            step("Navigate to settings with enhanced verification") {
                KaspressoUtils.captureScreenshot("tc003", "01_main_screen", 1)
                
                MainScreen {
                    settingsButton.clickWithFeedback("Navigate to settings")
                }
                
                KaspressoUtils.delay(2000, "Wait for settings to load")
                KaspressoUtils.handleSystemDialogs()
                KaspressoUtils.captureScreenshot("tc003", "02_settings_loaded", 2)
                
                // Verify we're in settings
                KaspressoUtils.safeInteraction("Verify settings screen") {
                    SettingsScreen.verifySettingsElementsDisplayed()
                }
            }
            
            step("Verify all settings elements with enhanced checks") {
                KaspressoUtils.captureScreenshot("tc003", "03_elements_check", 3)
                
                SettingsScreen {
                    // Check position radio buttons
                    KaspressoUtils.safeInteraction("Check position buttons") {
                        positionTopLeft.isDisplayed()
                        positionTopRight.isDisplayed()
                        positionBottomLeft.isDisplayed()
                        positionBottomRight.isDisplayed()
                    }
                    
                    // Check metric switches
                    KaspressoUtils.safeInteraction("Check metric switches") {
                        switchCpu.isDisplayed()
                        switchRam.isDisplayed()
                        switchTime.isDisplayed()
                    }
                    
                    // Check export buttons
                    KaspressoUtils.safeInteraction("Check export buttons") {
                        exportCsvButton.isDisplayed()
                        exportJsonButton.isDisplayed()
                    }
                    
                    // Check save button
                    KaspressoUtils.safeInteraction("Check save button") {
                        saveButton.isDisplayed()
                    }
                }
                
                KaspressoUtils.captureScreenshot("tc003", "04_elements_verified", 4)
            }
            
            step("Test element interactions") {
                KaspressoUtils.captureScreenshot("tc003", "05_interaction_test", 5)
                
                SettingsScreen {
                    // Test position selection
                    KaspressoUtils.safeInteraction("Test Top-Left position") {
                        selectPosition(OverlayPosition.TOP_LEFT)
                        KaspressoUtils.delay(500, "Wait for position selection")
                        assert(getSelectedPosition() == OverlayPosition.TOP_LEFT) {
                            "Top-Left position should be selected"
                        }
                    }
                    
                    // Test metric toggle
                    KaspressoUtils.safeInteraction("Test CPU toggle") {
                        val initialState = getMetricState(MetricType.CPU)
                        toggleMetric(MetricType.CPU)
                        KaspressoUtils.delay(500, "Wait for CPU toggle")
                        val newState = getMetricState(MetricType.CPU)
                        assert(initialState != newState) {
                            "CPU state should change after toggle"
                        }
                    }
                }
                
                KaspressoUtils.captureScreenshot("tc003", "06_interactions_tested", 6)
            }
            
            step("Return to main screen with enhanced verification") {
                KaspressoUtils.captureScreenshot("tc003", "07_before_return", 7)
                
                // Try save button first, then back
                SettingsScreen {
                    KaspressoUtils.safeInteraction("Save settings") {
                        saveAndReturn()
                    }
                }
                
                // If save didn't work, use back button
                KaspressoUtils.delay(2000, "Wait for save or back action")
                if (!KaspressoUtils.verifyElementExists(
                    androidx.test.espresso.matcher.ViewMatchers.withId(R.id.tv_app_title),
                    "Main screen title"
                )) {
                    KaspressoUtils.getDevice().pressBack()
                    KaspressoUtils.delay(2000, "Wait for back navigation")
                }
                
                KaspressoUtils.captureScreenshot("tc003", "08_main_screen_returned", 8)
                
                // Verify we're back on main screen
                MainScreen {
                    verifyMainElementsDisplayed()
                }
                
                KaspressoUtils.captureScreenshot("tc003", "09_return_verified", 9)
            }
        }
    }

    // ========== Enhanced TC-004: Comprehensive Settings Test ==========
    @Test
    fun enhanced_tc004_comprehensiveSettingsTest() {
        run("Enhanced Comprehensive Settings Test") {
            
            step("Navigate to settings") {
                MainScreen.openSettings()
                KaspressoUtils.delay(2000, "Wait for settings to load")
                KaspressoUtils.captureScreenshot("tc004", "01_settings_opened", 1)
            }
            
            step("Test all overlay positions") {
                val positions = listOf(
                    OverlayPosition.TOP_LEFT,
                    OverlayPosition.TOP_RIGHT,
                    OverlayPosition.BOTTOM_LEFT,
                    OverlayPosition.BOTTOM_RIGHT
                )
                
                positions.forEachIndexed { index, position ->
                    KaspressoUtils.captureScreenshot("tc004", "02_before_${position.name.lowercase()}", 2 + index)
                    
                    SettingsScreen {
                        selectPosition(position)
                        KaspressoUtils.delay(500, "Wait for ${position.name} selection")
                        
                        val selected = getSelectedPosition()
                        assert(selected == position) {
                            "${position.name} should be selected"
                        }
                    }
                    
                    KaspressoUtils.captureScreenshot("tc004", "03_${position.name.lowercase()}_selected", 6 + index)
                }
            }
            
            step("Test all metric configurations") {
                KaspressoUtils.captureScreenshot("tc004", "10_metrics_test_start", 10)
                
                SettingsScreen {
                    // Enable all metrics
                    setAllMetricsState(true)
                    KaspressoUtils.delay(1000, "Wait for all metrics enable")
                    
                    assert(getMetricState(MetricType.CPU) == true) { "CPU should be enabled" }
                    assert(getMetricState(MetricType.RAM) == true) { "RAM should be enabled" }
                    assert(getMetricState(MetricType.TIME) == true) { "Time should be enabled" }
                    
                    KaspressoUtils.captureScreenshot("tc004", "11_all_enabled", 11)
                    
                    // Disable all metrics
                    setAllMetricsState(false)
                    KaspressoUtils.delay(1000, "Wait for all metrics disable")
                    
                    assert(getMetricState(MetricType.CPU) == false) { "CPU should be disabled" }
                    assert(getMetricState(MetricType.RAM) == false) { "RAM should be disabled" }
                    assert(getMetricState(MetricType.TIME) == false) { "Time should be disabled" }
                    
                    KaspressoUtils.captureScreenshot("tc004", "12_all_disabled", 12)
                }
            }
            
            step("Test background collection") {
                KaspressoUtils.safeInteraction("Test background collection") {
                    SettingsScreen {
                        val initialState = getBackgroundCollectionState()
                        toggleBackgroundCollection()
                        KaspressoUtils.delay(500, "Wait for background toggle")
                        val newState = getBackgroundCollectionState()
                        
                        if (initialState != null && newState != null) {
                            assert(initialState != newState) {
                                "Background collection state should change"
                            }
                        }
                    }
                    
                    KaspressoUtils.captureScreenshot("tc004", "13_background_tested", 13)
                }
            }
            
            step("Test export functionality") {
                KaspressoUtils.safeInteraction("Test CSV export") {
                    SettingsScreen.exportToCsv()
                    KaspressoUtils.delay(2000, "Wait for CSV export")
                    KaspressoUtils.handleSystemDialogs()
                }
                
                KaspressoUtils.safeInteraction("Test JSON export") {
                    SettingsScreen.exportToJson()
                    KaspressoUtils.delay(2000, "Wait for JSON export")
                    KaspressoUtils.handleSystemDialogs()
                }
                
                KaspressoUtils.captureScreenshot("tc004", "14_export_tested", 14)
            }
            
            step("Save and return") {
                SettingsScreen.saveAndReturn()
                KaspressoUtils.delay(2000, "Wait for save and return")
                
                // Verify return to main screen
                MainScreen.verifyMainElementsDisplayed()
                KaspressoUtils.captureScreenshot("tc004", "15_returned_main", 15)
            }
        }
    }
}
