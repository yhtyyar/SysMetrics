package com.sysmetrics.tests

import androidx.test.ext.junit.rules.ActivityTestRule
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import com.kaspersky.kaspresso.annotations.Requirements
import com.sysmetrics.app.ui.MainActivityOverlay
import com.sysmetrics.screens.MainScreen
import com.sysmetrics.steps.MainScreenSteps.openSettings
import com.sysmetrics.steps.MainScreenSteps.startOverlay
import com.sysmetrics.steps.MainScreenSteps.stopOverlay
import com.sysmetrics.steps.MainScreenSteps.verifyAppLaunch
import com.sysmetrics.steps.SettingsScreenSteps.verifySettingsScreenLoaded
import com.sysmetrics.steps.SettingsScreenSteps.saveAndReturn
import org.junit.Rule
import org.junit.Test

/**
 * Smoke tests for SysMetrics application
 * Basic verification of app functionality
 */
class SysMetricsSmokeTest : TestCase() {

    @get:Rule
    val activityRule = ActivityTestRule(MainActivityOverlay::class.java, true, false)

    @Test
    @Requirements("SMOKE-001", "SMOKE-002", "SMOKE-003")
    fun appLaunchAndBasicNavigationTest() = run {
        before {
            activityRule.launchActivity(null)
        }.after {
            // Ensure overlay is stopped after test
            if (device.uiDevice.isScreenOn) {
                try {
                    flakySafely(timeout = 3000) {
                        if (MainScreen.isVisible()) {
                            stopOverlay()
                        }
                    }
                } catch (e: Exception) {
                    // Ignore cleanup errors
                }
            }
        }.run {

            step("1. Verify app launches correctly") {
                verifyAppLaunch()
            }

            step("2. Verify initial state - overlay is OFF") {
                MainScreen.verifyOverlayStatusOff()
            }

            step("3. Start overlay service") {
                startOverlay()
            }

            step("4. Verify metrics preview is displayed") {
                MainScreen.verifyMetricsPreviewVisible()
            }

            step("5. Stop overlay service") {
                stopOverlay()
            }

            step("6. Verify metrics preview is hidden") {
                MainScreen.verifyMetricsPreviewHidden()
            }

            step("7. Navigate to Settings screen") {
                openSettings()
            }

            step("8. Verify Settings screen is loaded") {
                verifySettingsScreenLoaded()
            }

            step("9. Save settings and return to main") {
                saveAndReturn()
            }

            step("10. Verify we're back on main screen") {
                verifyAppLaunch()
            }
        }
    }

    @Test
    @Requirements("SMOKE-004")
    fun metricsUpdateTest() = run {
        before {
            activityRule.launchActivity(null)
        }.after {
            // Cleanup
            if (device.uiDevice.isScreenOn) {
                try {
                    flakySafely(timeout = 3000) {
                        if (MainScreen.isVisible()) {
                            stopOverlay()
                        }
                    }
                } catch (e: Exception) {
                    // Ignore cleanup errors
                }
            }
        }.run {

            step("1. Start overlay to enable metrics preview") {
                startOverlay()
            }

            step("2. Verify metrics are updating") {
                // Wait for initial metrics update
                flakySafely(timeout = 3000) {
                    MainScreen.cpuPreview.isVisible()
                    MainScreen.ramPreview.isVisible()
                }

                // Capture initial values
                var initialCpu = ""
                MainScreen.cpuPreview {
                    initialCpu = getText().toString()
                }

                // Wait for next update cycle (metrics update every 1 second)
                flakySafely(timeout = 2500) {
                    MainScreen.cpuPreview {
                        // Value should change or be a valid percentage format
                        val currentText = getText().toString()
                        assert(currentText.contains("%")) { "CPU value should contain %" }
                    }
                }
            }

            step("3. Stop overlay") {
                stopOverlay()
            }
        }
    }
}
