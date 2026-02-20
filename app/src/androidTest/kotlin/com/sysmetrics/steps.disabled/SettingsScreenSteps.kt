package com.sysmetrics.steps

import com.kaspersky.components.kaspresso.testcases.core.testcontext.TestContext
import com.sysmetrics.screens.SettingsScreen
import com.sysmetrics.screens.SettingsScreen.OverlayPosition
import com.sysmetrics.screens.SettingsScreen.MetricType
import com.sysmetrics.screens.SettingsScreen.ExportFormat
import timber.log.Timber

/**
 * Common steps for Settings Screen interactions
 */
object SettingsScreenSteps {

    private const val TAG = "SettingsScreenSteps"

    /**
     * Configure overlay position
     */
    fun TestContext<*>.configurePosition(position: OverlayPosition) {
        step("Configure overlay position to ${position.name}") {
            SettingsScreen.selectPosition(position)
            Timber.tag(TAG).d("Position configured to ${position.name}")
        }
    }

    /**
     * Configure which metrics to display
     */
    fun TestContext<*>.configureMetrics(
        showCpu: Boolean = true,
        showRam: Boolean = true,
        showTime: Boolean = true
    ) {
        step("Configure metric display - CPU: $showCpu, RAM: $showRam, Time: $showTime") {
            SettingsScreen.toggleMetric(MetricType.CPU, showCpu)
            SettingsScreen.toggleMetric(MetricType.RAM, showRam)
            SettingsScreen.toggleMetric(MetricType.TIME, showTime)
            Timber.tag(TAG).d("Metrics configured")
        }
    }

    /**
     * Enable or disable background collection
     */
    fun TestContext<*>.setBackgroundCollection(enabled: Boolean) {
        step("Set background collection to $enabled") {
            SettingsScreen.switchBackgroundCollection.setChecked(enabled)
            Timber.tag(TAG).d("Background collection set to $enabled")
        }
    }

    /**
     * Save settings and return to main screen
     */
    fun TestContext<*>.saveAndReturn() {
        step("Save settings and return to main screen") {
            SettingsScreen.saveSettings()
            // Verify we're back on main screen
            flakySafely(timeout = 5000) {
                com.sysmetrics.screens.MainScreen {
                    isVisible()
                }
            }
            Timber.tag(TAG).d("Settings saved and returned to main screen")
        }
    }

    /**
     * Export metrics in specified format
     */
    fun TestContext<*>.exportMetrics(format: ExportFormat) {
        step("Export metrics in ${format.name} format") {
            SettingsScreen.exportMetrics(format)
            Timber.tag(TAG).d("Export initiated in ${format.name} format")
        }
    }

    /**
     * Verify all settings UI elements are visible
     */
    fun TestContext<*>.verifySettingsScreenLoaded() {
        step("Verify Settings screen is loaded") {
            SettingsScreen {
                toolbar.isVisible()
                positionTopLeft.isVisible()
                positionTopRight.isVisible()
                positionBottomLeft.isVisible()
                positionBottomRight.isVisible()
                switchCpu.isVisible()
                switchRam.isVisible()
                switchTime.isVisible()
                exportCsvButton.isVisible()
                exportJsonButton.isVisible()
                switchBackgroundCollection.isVisible()
                saveButton.isVisible()
            }
            Timber.tag(TAG).d("Settings screen loaded verification passed")
        }
    }
}
