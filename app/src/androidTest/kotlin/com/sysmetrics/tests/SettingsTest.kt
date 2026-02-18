package com.sysmetrics.tests

import androidx.test.ext.junit.rules.ActivityTestRule
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import com.kaspersky.kaspresso.annotations.Requirements
import com.sysmetrics.app.ui.MainActivityOverlay
import com.sysmetrics.screens.MainScreen
import com.sysmetrics.screens.SettingsScreen
import com.sysmetrics.steps.MainScreenSteps.openSettings
import com.sysmetrics.steps.MainScreenSteps.startOverlay
import com.sysmetrics.steps.MainScreenSteps.stopOverlay
import com.sysmetrics.steps.SettingsScreenSteps.configurePosition
import com.sysmetrics.steps.SettingsScreenSteps.configureMetrics
import com.sysmetrics.steps.SettingsScreenSteps.setBackgroundCollection
import com.sysmetrics.steps.SettingsScreenSteps.saveAndReturn
import org.junit.Rule
import org.junit.Test

/**
 * Tests for Settings screen functionality
 * Covers position configuration, metric toggles, and export functionality
 */
class SettingsTest : TestCase() {

    @get:Rule
    val activityRule = ActivityTestRule(MainActivityOverlay::class.java, true, false)

    @Test
    @Requirements("SET-001", "SET-002")
    fun overlayPositionConfigurationTest() = run {
        before {
            activityRule.launchActivity(null)
        }.after {
            // Cleanup
        }.run {

            step("1. Navigate to Settings") {
                openSettings()
            }

            step("2. Verify all position options are available") {
                SettingsScreen {
                    positionTopLeft.isVisible()
                    positionTopRight.isVisible()
                    positionBottomLeft.isVisible()
                    positionBottomRight.isVisible()
                }
            }

            step("3. Select each position and verify selection") {
                // Test each position option
                SettingsScreen.positionTopLeft.click()
                SettingsScreen.positionTopLeft.isChecked()

                SettingsScreen.positionTopRight.click()
                SettingsScreen.positionTopRight.isChecked()

                SettingsScreen.positionBottomLeft.click()
                SettingsScreen.positionBottomLeft.isChecked()

                SettingsScreen.positionBottomRight.click()
                SettingsScreen.positionBottomRight.isChecked()
            }

            step("4. Save settings") {
                saveAndReturn()
            }
        }
    }

    @Test
    @Requirements("SET-003", "SET-004")
    fun metricToggleConfigurationTest() = run {
        before {
            activityRule.launchActivity(null)
        }.after {
            // Cleanup
        }.run {

            step("1. Navigate to Settings") {
                openSettings()
            }

            step("2. Verify all metric switches are present") {
                SettingsScreen {
                    switchCpu.isVisible()
                    switchRam.isVisible()
                    switchTime.isVisible()
                }
            }

            step("3. Toggle CPU metric OFF") {
                SettingsScreen.switchCpu.setChecked(false)
                SettingsScreen.switchCpu.isNotChecked()
            }

            step("4. Toggle RAM metric OFF") {
                SettingsScreen.switchRam.setChecked(false)
                SettingsScreen.switchRam.isNotChecked()
            }

            step("5. Toggle Time metric OFF") {
                SettingsScreen.switchTime.setChecked(false)
                SettingsScreen.switchTime.isNotChecked()
            }

            step("6. Toggle all metrics back ON") {
                SettingsScreen.switchCpu.setChecked(true)
                SettingsScreen.switchRam.setChecked(true)
                SettingsScreen.switchTime.setChecked(true)

                SettingsScreen.switchCpu.isChecked()
                SettingsScreen.switchRam.isChecked()
                SettingsScreen.switchTime.isChecked()
            }

            step("7. Save settings") {
                saveAndReturn()
            }
        }
    }

    @Test
    @Requirements("SET-005")
    fun backgroundCollectionToggleTest() = run {
        before {
            activityRule.launchActivity(null)
        }.after {
            // Disable background collection after test
            try {
                if (SettingsScreen.isVisible()) {
                    SettingsScreen.switchBackgroundCollection.setChecked(false)
                }
            } catch (e: Exception) {
                // Ignore
            }
        }.run {

            step("1. Navigate to Settings") {
                openSettings()
            }

            step("2. Verify background collection switch exists") {
                SettingsScreen.switchBackgroundCollection.isVisible()
            }

            step("3. Enable background collection") {
                SettingsScreen.switchBackgroundCollection.setChecked(true)
                SettingsScreen.switchBackgroundCollection.isChecked()
            }

            step("4. Disable background collection") {
                SettingsScreen.switchBackgroundCollection.setChecked(false)
                SettingsScreen.switchBackgroundCollection.isNotChecked()
            }
        }
    }

    @Test
    @Requirements("SET-006", "SET-007")
    fun exportButtonsVisibilityTest() = run {
        before {
            activityRule.launchActivity(null)
        }.after {
            // Cleanup
        }.run {

            step("1. Navigate to Settings") {
                openSettings()
            }

            step("2. Verify export buttons are visible") {
                SettingsScreen {
                    exportCsvButton.isVisible()
                    exportJsonButton.isVisible()
                }
            }

            step("3. Verify export buttons are enabled") {
                SettingsScreen {
                    exportCsvButton.isEnabled()
                    exportJsonButton.isEnabled()
                }
            }
        }
    }

    @Test
    @Requirements("SET-008")
    fun combinedSettingsConfigurationTest() = run {
        before {
            activityRule.launchActivity(null)
        }.after {
            // Cleanup
        }.run {

            step("1. Navigate to Settings") {
                openSettings()
            }

            step("2. Configure all settings together") {
                configurePosition(SettingsScreen.OverlayPosition.TOP_RIGHT)
                configureMetrics(showCpu = true, showRam = true, showTime = false)
                setBackgroundCollection(false)
            }

            step("3. Verify settings are applied") {
                SettingsScreen {
                    positionTopRight.isChecked()
                    switchCpu.isChecked()
                    switchRam.isChecked()
                    switchTime.isNotChecked()
                    switchBackgroundCollection.isNotChecked()
                }
            }

            step("4. Save and verify return to main screen") {
                saveAndReturn()
            }
        }
    }
}
