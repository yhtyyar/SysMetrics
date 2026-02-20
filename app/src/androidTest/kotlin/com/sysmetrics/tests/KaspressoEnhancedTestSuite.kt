package com.sysmetrics.tests

import android.Manifest
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.kaspersky.components.kaspresso.Kaspresso
import com.kaspersky.components.kaspresso.configurator.KaspressoConfigurator
import com.kaspersky.components.kaspresso.testcases.api.testcase.TestCase
import com.sysmetrics.app.R
import com.sysmetrics.app.ui.MainActivityOverlay
import com.sysmetrics.screens.MainScreen
import com.sysmetrics.screens.SettingsScreen
import com.sysmetrics.screens.OverlayPosition
import com.sysmetrics.screens.MetricType
import com.sysmetrics.utils.ScreenshotUtils
import com.sysmetrics.utils.TestUtils
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Enhanced Kaspresso Test Suite
 * Uses proper Page Objects and Kaspresso DSL
 * Implements all test cases with improved structure
 */
@RunWith(AndroidJUnit4::class)
class KaspressoEnhancedTestSuite : TestCase(KaspressoConfigurator.defaultConfig) {

    @get:Rule(order = 0)
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )

    @get:Rule(order = 1)
    val activityRule = ActivityScenarioRule(MainActivityOverlay::class.java)

    @Before
    fun setup() {
        ScreenshotUtils.resetCounter()
        ScreenshotUtils.captureScreenshot("kaspresso_setup", "initial_state", 0)
    }

    @After
    fun teardown() {
        ScreenshotUtils.captureScreenshot("kaspresso_teardown", "final_state", 99)
    }

    // ========== TC-001: App Launch and Basic Elements Verification ==========
    @Test
    fun tc001_appLaunchAndBasicElementsVerification() {
        run("App Launch and Basic Elements Verification") {
            
            step("Launch app and verify main elements") {
                ScreenshotUtils.captureScreenshot("tc001", "01_app_launched", 1)
                TestUtils.delay(2000, "Wait for app to fully load")
                
                MainScreen {
                    verifyMainElementsDisplayed()
                    appTitle.hasText(R.string.app_name)
                }
                
                ScreenshotUtils.captureScreenshot("tc001", "02_elements_verified", 2)
                TestUtils.delay(1500, "Display verified elements")
            }
            
            step("Verify initial overlay status") {
                ScreenshotUtils.captureScreenshot("tc001", "03_status_check", 3)
                
                MainScreen {
                    statusText.hasText(R.string.overlay_status_off)
                    toggleButton.hasText(R.string.start_overlay)
                }
                
                ScreenshotUtils.captureScreenshot("tc001", "04_status_verified", 4)
                TestUtils.delay(1000, "Display status verification")
            }
            
            step("Verify settings button functionality") {
                ScreenshotUtils.captureScreenshot("tc001", "05_settings_check", 5)
                
                MainScreen {
                    settingsButton.isDisplayed()
                    settingsButton.hasText(R.string.settings)
                }
                
                ScreenshotUtils.captureScreenshot("tc001", "06_settings_verified", 6)
                TestUtils.delay(1000, "Display settings verification")
            }
        }
    }

    // ========== TC-002: Toggle Overlay Functionality Test ==========
    @Test
    fun tc002_toggleOverlayFunctionalityTest() {
        run("Toggle Overlay Functionality Test") {
            
            step("Verify initial overlay state") {
                ScreenshotUtils.captureScreenshot("tc002", "01_initial_state", 1)
                
                MainScreen {
                    statusText.hasText(R.string.overlay_status_off)
                    toggleButton.hasText(R.string.start_overlay)
                    isOverlayActive() shouldBe false
                }
                
                TestUtils.delay(1000, "Display initial state")
            }
            
            step("Start overlay") {
                ScreenshotUtils.captureScreenshot("tc002", "02_before_start", 2)
                
                MainScreen {
                    toggleOverlay()
                }
                
                TestUtils.delay(1000, "Wait for overlay start")
                ScreenshotUtils.captureScreenshot("tc002", "03_overlay_starting", 3)
                
                MainScreen {
                    TestUtils.delay(2000, "Wait for overlay to activate")
                    statusText.hasText(R.string.overlay_status_on)
                    toggleButton.hasText(R.string.stop_overlay)
                    isOverlayActive() shouldBe true
                }
                
                ScreenshotUtils.captureScreenshot("tc002", "04_overlay_active", 4)
                TestUtils.delay(2000, "Display active overlay")
            }
            
            step("Verify metrics display") {
                ScreenshotUtils.captureScreenshot("tc002", "05_metrics_check", 5)
                
                MainScreen {
                    waitForMetricsToAppear()
                    TestUtils.delay(1000, "Wait for metrics to load")
                }
                
                ScreenshotUtils.captureScreenshot("tc002", "06_metrics_displayed", 6)
                TestUtils.delay(3000, "Display metrics updating")
            }
            
            step("Stop overlay") {
                ScreenshotUtils.captureScreenshot("tc002", "07_before_stop", 7)
                
                MainScreen {
                    toggleOverlay()
                }
                
                TestUtils.delay(1000, "Wait for overlay stop")
                ScreenshotUtils.captureScreenshot("tc002", "08_overlay_stopping", 8)
                
                MainScreen {
                    TestUtils.delay(2000, "Wait for overlay to deactivate")
                    statusText.hasText(R.string.overlay_status_off)
                    toggleButton.hasText(R.string.start_overlay)
                    isOverlayActive() shouldBe false
                }
                
                ScreenshotUtils.captureScreenshot("tc002", "09_overlay_stopped", 9)
                TestUtils.delay(2000, "Display stopped overlay")
            }
        }
    }

    // ========== TC-003: Settings Navigation and Elements Verification ==========
    @Test
    fun tc003_settingsNavigationAndElementsVerification() {
        run("Settings Navigation and Elements Verification") {
            
            step("Navigate to settings") {
                ScreenshotUtils.captureScreenshot("tc003", "01_main_screen", 1)
                
                MainScreen {
                    openSettings()
                }
                
                TestUtils.delay(2000, "Wait for settings to load")
                ScreenshotUtils.captureScreenshot("tc003", "02_settings_loaded", 2)
            }
            
            step("Verify settings elements") {
                ScreenshotUtils.captureScreenshot("tc003", "03_elements_check", 3)
                
                SettingsScreen {
                    verifySettingsElementsDisplayed()
                }
                
                ScreenshotUtils.captureScreenshot("tc003", "04_elements_verified", 4)
                TestUtils.delay(1500, "Display verified elements")
            }
            
            step("Return to main screen") {
                ScreenshotUtils.captureScreenshot("tc003", "05_before_return", 6)
                
                // Use back button to return
                device.pressBack()
                
                TestUtils.delay(2000, "Wait for return to main")
                ScreenshotUtils.captureScreenshot("tc003", "06_main_screen_returned", 7)
                
                MainScreen {
                    verifyMainElementsDisplayed()
                }
                
                TestUtils.delay(1000, "Display returned main screen")
            }
        }
    }

    // ========== TC-004: Overlay Position Configuration Test ==========
    @Test
    fun tc004_overlayPositionConfigurationTest() {
        run("Overlay Position Configuration Test") {
            
            step("Navigate to settings") {
                MainScreen.openSettings()
                TestUtils.delay(2000, "Wait for settings to load")
                ScreenshotUtils.captureScreenshot("tc004", "01_settings_opened", 1)
            }
            
            step("Test Top-Left position") {
                ScreenshotUtils.captureScreenshot("tc004", "02_before_top_left", 2)
                
                SettingsScreen {
                    selectPosition(OverlayPosition.TOP_LEFT)
                    TestUtils.delay(500, "Wait for selection")
                    
                    getSelectedPosition() shouldBe OverlayPosition.TOP_LEFT
                }
                
                ScreenshotUtils.captureScreenshot("tc004", "03_top_left_selected", 3)
            }
            
            step("Test Top-Right position") {
                ScreenshotUtils.captureScreenshot("tc004", "04_before_top_right", 4)
                
                SettingsScreen {
                    selectPosition(OverlayPosition.TOP_RIGHT)
                    TestUtils.delay(500, "Wait for selection")
                    
                    getSelectedPosition() shouldBe OverlayPosition.TOP_RIGHT
                }
                
                ScreenshotUtils.captureScreenshot("tc004", "05_top_right_selected", 5)
            }
            
            step("Test Bottom-Left position") {
                ScreenshotUtils.captureScreenshot("tc004", "06_before_bottom_left", 6)
                
                SettingsScreen {
                    selectPosition(OverlayPosition.BOTTOM_LEFT)
                    TestUtils.delay(500, "Wait for selection")
                    
                    getSelectedPosition() shouldBe OverlayPosition.BOTTOM_LEFT
                }
                
                ScreenshotUtils.captureScreenshot("tc004", "07_bottom_left_selected", 7)
            }
            
            step("Test Bottom-Right position") {
                ScreenshotUtils.captureScreenshot("tc004", "08_before_bottom_right", 8)
                
                SettingsScreen {
                    selectPosition(OverlayPosition.BOTTOM_RIGHT)
                    TestUtils.delay(500, "Wait for selection")
                    
                    getSelectedPosition() shouldBe OverlayPosition.BOTTOM_RIGHT
                }
                
                ScreenshotUtils.captureScreenshot("tc004", "09_bottom_right_selected", 9)
            }
            
            step("Return to main screen") {
                SettingsScreen.saveAndReturn()
                TestUtils.delay(2000, "Wait for return to main")
                ScreenshotUtils.captureScreenshot("tc004", "10_returned_main", 10)
            }
        }
    }

    // ========== TC-005: Metrics Display Configuration Test ==========
    @Test
    fun tc005_metricsDisplayConfigurationTest() {
        run("Metrics Display Configuration Test") {
            
            step("Navigate to settings") {
                MainScreen.openSettings()
                TestUtils.delay(2000, "Wait for settings to load")
                ScreenshotUtils.captureScreenshot("tc005", "01_settings_opened", 1)
            }
            
            step("Toggle individual metrics") {
                ScreenshotUtils.captureScreenshot("tc005", "02_initial_metrics", 2)
                
                SettingsScreen {
                    // Toggle CPU
                    toggleMetric(MetricType.CPU)
                    TestUtils.delay(500, "Wait for CPU toggle")
                    ScreenshotUtils.captureScreenshot("tc005", "03_cpu_toggled", 3)
                    
                    // Toggle RAM
                    toggleMetric(MetricType.RAM)
                    TestUtils.delay(500, "Wait for RAM toggle")
                    ScreenshotUtils.captureScreenshot("tc005", "04_ram_toggled", 4)
                    
                    // Toggle Time
                    toggleMetric(MetricType.TIME)
                    TestUtils.delay(500, "Wait for Time toggle")
                    ScreenshotUtils.captureScreenshot("tc005", "05_time_toggled", 5)
                }
            }
            
            step("Enable all metrics") {
                SettingsScreen {
                    setAllMetricsState(true)
                    TestUtils.delay(500, "Wait for all metrics enable")
                    
                    getMetricState(MetricType.CPU) shouldBe true
                    getMetricState(MetricType.RAM) shouldBe true
                    getMetricState(MetricType.TIME) shouldBe true
                }
                
                ScreenshotUtils.captureScreenshot("tc005", "06_all_enabled", 6)
            }
            
            step("Disable all metrics") {
                SettingsScreen {
                    setAllMetricsState(false)
                    TestUtils.delay(500, "Wait for all metrics disable")
                    
                    getMetricState(MetricType.CPU) shouldBe false
                    getMetricState(MetricType.RAM) shouldBe false
                    getMetricState(MetricType.TIME) shouldBe false
                }
                
                ScreenshotUtils.captureScreenshot("tc005", "07_all_disabled", 7)
            }
            
            step("Return to main screen") {
                SettingsScreen.saveAndReturn()
                TestUtils.delay(2000, "Wait for return to main")
                ScreenshotUtils.captureScreenshot("tc005", "08_returned_main", 8)
            }
        }
    }

    // ========== TC-006: Metrics Export Functionality Test ==========
    @Test
    fun tc006_metricsExportFunctionalityTest() {
        run("Metrics Export Functionality Test") {
            
            step("Navigate to settings") {
                MainScreen.openSettings()
                TestUtils.delay(2000, "Wait for settings to load")
                ScreenshotUtils.captureScreenshot("tc006", "01_settings_opened", 1)
            }
            
            step("Test CSV export") {
                ScreenshotUtils.captureScreenshot("tc006", "02_before_csv_export", 2)
                
                SettingsScreen {
                    exportToCsv()
                }
                
                TestUtils.delay(2000, "Wait for CSV export dialog")
                ScreenshotUtils.captureScreenshot("tc006", "03_csv_export_dialog", 3)
                
                // Close export dialog
                device.pressBack()
                TestUtils.delay(1000, "Wait for dialog close")
            }
            
            step("Test JSON export") {
                ScreenshotUtils.captureScreenshot("tc006", "04_before_json_export", 4)
                
                SettingsScreen {
                    exportToJson()
                }
                
                TestUtils.delay(2000, "Wait for JSON export dialog")
                ScreenshotUtils.captureScreenshot("tc006", "05_json_export_dialog", 5)
                
                // Close export dialog
                device.pressBack()
                TestUtils.delay(1000, "Wait for dialog close")
            }
            
            step("Return to main screen") {
                SettingsScreen.saveAndReturn()
                TestUtils.delay(2000, "Wait for return to main")
                ScreenshotUtils.captureScreenshot("tc006", "06_returned_main", 6)
            }
        }
    }

    // ========== TC-008: Save Settings and Return Test ==========
    @Test
    fun tc008_saveSettingsAndReturnTest() {
        run("Save Settings and Return Test") {
            
            step("Navigate to settings and make changes") {
                MainScreen.openSettings()
                TestUtils.delay(2000, "Wait for settings to load")
                ScreenshotUtils.captureScreenshot("tc008", "01_settings_opened", 1)
                
                SettingsScreen {
                    // Make some changes
                    selectPosition(OverlayPosition.BOTTOM_RIGHT)
                    TestUtils.delay(500, "Wait for position change")
                    
                    toggleMetric(MetricType.CPU)
                    TestUtils.delay(500, "Wait for CPU toggle")
                    
                    toggleMetric(MetricType.RAM)
                    TestUtils.delay(500, "Wait for RAM toggle")
                }
                
                ScreenshotUtils.captureScreenshot("tc008", "02_changes_made", 2)
            }
            
            step("Save settings") {
                ScreenshotUtils.captureScreenshot("tc008", "03_before_save", 3)
                
                SettingsScreen {
                    saveAndReturn()
                }
                
                TestUtils.delay(2000, "Wait for save and return")
                ScreenshotUtils.captureScreenshot("tc008", "04_settings_saved", 4)
            }
            
            step("Verify return to main screen") {
                TestUtils.delay(2000, "Wait for main screen load")
                ScreenshotUtils.captureScreenshot("tc008", "05_main_screen_loaded", 5)
                
                MainScreen {
                    verifyMainElementsDisplayed()
                }
                
                ScreenshotUtils.captureScreenshot("tc008", "06_main_verified", 6)
            }
        }
    }
}

/**
 * Extension function for device access
 */
private val TestCase.device get() = KaspressoConfigurator.deviceContext?.let { 
    androidx.test.uiautomator.UiDevice.getInstance(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation())
}
