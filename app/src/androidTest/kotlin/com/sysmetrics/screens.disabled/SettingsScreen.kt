package com.sysmetrics.screens

import com.kaspersky.components.kaspresso.screens.KScreen
import com.kaspersky.components.kaspresso.testcases.core.testcontext.TestContext
import io.github.kakaocup.kakao.common.views.KView
import io.github.kakaocup.kakao.text.KTextView
import io.github.kakaocup.kakao.text.KButton
import io.github.kakaocup.kakao.material.KSwitch
import io.github.kakaocup.kakao.material.KRadioButton
import io.github.kakaocup.kakao.text.KTextView as KakaoTextView
import com.sysmetrics.app.R

/**
 * Page Object for Settings Activity Screen
 * Represents the settings screen with configuration options
 */
object SettingsScreen : KScreen<SettingsScreen>() {

    override val layoutId: Int = R.layout.activity_settings
    override val viewClass: Class<*> = com.sysmetrics.app.ui.SettingsActivity::class.java

    // Toolbar
    val toolbar = KView { withId(R.id.toolbar) }

    // Position Radio Group
    val positionTopLeft = KRadioButton { withId(R.id.rb_top_left) }
    val positionTopRight = KRadioButton { withId(R.id.rb_top_right) }
    val positionBottomLeft = KRadioButton { withId(R.id.rb_bottom_left) }
    val positionBottomRight = KRadioButton { withId(R.id.rb_bottom_right) }

    // Metric Switches
    val switchCpu = KSwitch { withId(R.id.switch_cpu) }
    val switchRam = KSwitch { withId(R.id.switch_ram) }
    val switchTime = KSwitch { withId(R.id.switch_time) }

    // Background Collection Switch
    val switchBackgroundCollection = KSwitch { withId(R.id.switch_background_collection) }

    // Export Buttons
    val exportCsvButton = KButton { withId(R.id.btn_export_csv) }
    val exportJsonButton = KButton { withId(R.id.btn_export_json) }

    // Save Button
    val saveButton = KButton { withId(R.id.btn_save) }

    /**
     * Select overlay position
     */
    fun TestContext<*>.selectPosition(position: OverlayPosition) {
        step("Select overlay position: ${position.name}") {
            when (position) {
                OverlayPosition.TOP_LEFT -> positionTopLeft.click()
                OverlayPosition.TOP_RIGHT -> positionTopRight.click()
                OverlayPosition.BOTTOM_LEFT -> positionBottomLeft.click()
                OverlayPosition.BOTTOM_RIGHT -> positionBottomRight.click()
            }
        }
    }

    /**
     * Toggle metric display settings
     */
    fun TestContext<*>.toggleMetric(metric: MetricType, enabled: Boolean) {
        step("Set ${metric.name} metric display to $enabled") {
            when (metric) {
                MetricType.CPU -> switchCpu.setChecked(enabled)
                MetricType.RAM -> switchRam.setChecked(enabled)
                MetricType.TIME -> switchTime.setChecked(enabled)
            }
        }
    }

    /**
     * Verify all metric switches are checked
     */
    fun TestContext<*>.verifyAllMetricsEnabled() {
        step("Verify all metric switches are enabled") {
            switchCpu.isChecked()
            switchRam.isChecked()
            switchTime.isChecked()
        }
    }

    /**
     * Click save button and verify navigation back
     */
    fun TestContext<*>.saveSettings() {
        step("Save settings") {
            saveButton.click()
        }
    }

    /**
     * Export metrics in specified format
     */
    fun TestContext<*>.exportMetrics(format: ExportFormat) {
        step("Export metrics as ${format.name}") {
            when (format) {
                ExportFormat.CSV -> exportCsvButton.click()
                ExportFormat.JSON -> exportJsonButton.click()
            }
        }
    }

    enum class OverlayPosition {
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
    }

    enum class MetricType {
        CPU, RAM, TIME
    }

    enum class ExportFormat {
        CSV, JSON
    }
}
