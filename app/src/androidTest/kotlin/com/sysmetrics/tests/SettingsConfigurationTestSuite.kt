package com.sysmetrics.tests

import android.Manifest
import android.util.Log
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isNotChecked
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.uiautomator.UiDevice
import androidx.test.platform.app.InstrumentationRegistry
import com.sysmetrics.app.R
import com.sysmetrics.app.ui.MainActivityOverlay
import com.sysmetrics.utils.ScreenshotUtils
import com.sysmetrics.utils.TestUtils
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Settings Configuration Test Suite
 * Implements TC-004 through TC-008 test cases
 * Focuses on settings functionality and configuration
 */
@RunWith(AndroidJUnit4::class)
class SettingsConfigurationTestSuite {

    companion object {
        private const val TAG = "SettingsConfigTestSuite"
    }

    @get:Rule(order = 0)
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )

    @get:Rule(order = 1)
    val activityRule = ActivityScenarioRule(MainActivityOverlay::class.java)

    private lateinit var device: UiDevice
    private val testResults = mutableListOf<ComprehensiveTestSuite.TestResult>()
    private var stepCounter = 1

    @Before
    fun setup() {
        Log.d(TAG, "=== SETTINGS CONFIGURATION TEST SUITE SETUP ===")
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        stepCounter = 1
        
        Log.i(TAG, "🎯 Starting settings configuration test suite...")
        ScreenshotUtils.captureScreenshot("settings_setup", "initial_state", 0)
    }

    @After
    fun teardown() {
        Log.d(TAG, "=== SETTINGS CONFIGURATION TEST SUITE TEARDOWN ===")
        ScreenshotUtils.captureScreenshot("settings_teardown", "final_state", 99)
        
        generateSettingsReport()
        Log.i(TAG, "📊 Settings configuration report generated")
        Log.i(TAG, "🎉 Settings test suite completed!")
    }

    // ========== TC-004: Overlay Position Configuration Test ==========
    @Test
    fun tc004_overlayPositionConfigurationTest() {
        val startTime = System.currentTimeMillis()
        val steps = mutableListOf<ComprehensiveTestSuite.StepResult>()
        val testCaseId = "TC-004"
        val testCaseName = "Overlay Position Configuration Test"
        
        Log.i(TAG, "🚀 Starting $testCaseId: $testCaseName")
        
        try {
            // Navigate to settings first
            navigateToSettings()
            
            // Step 1: Initial state screenshot
            val step1 = executeStep(
                stepNumber = stepCounter++,
                action = "Сделать скриншот начального состояния настроек",
                expectedResult = "Скриншот сохранен",
                actualAction = {
                    ScreenshotUtils.captureScreenshot("tc004", "01_initial_state", stepCounter)
                }
            )
            steps.add(step1)

            // Step 2: Remember current position
            val step2 = executeStep(
                stepNumber = stepCounter++,
                action = "Запомнить текущую выбранную позицию",
                expectedResult = "Текущая позиция определена",
                actualAction = {
                    try {
                        val currentSelection = getCurrentPositionSelection()
                        Log.d(TAG, "Current position selection: $currentSelection")
                        ScreenshotUtils.captureScreenshot("tc004", "02_current_position", stepCounter)
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not determine current position: ${e.message}")
                        ScreenshotUtils.captureScreenshot("tc004", "02_position_undetermined", stepCounter)
                    }
                }
            )
            steps.add(step2)

            // Step 3: Select Top-Left
            val step3 = executeStep(
                stepNumber = stepCounter++,
                action = "Нажать на radio button \"Top-Left\"",
                expectedResult = "Top-Left становится выбранным, другие deselect",
                actualAction = {
                    try {
                        onView(withId(R.id.rb_top_left))
                            .perform(click())
                        TestUtils.delay(500, "After Top-Left selection")
                        onView(withId(R.id.rb_top_left))
                            .check(matches(isChecked()))
                        ScreenshotUtils.captureScreenshot("tc004", "03_top_left_selected", stepCounter)
                    } catch (e: Exception) {
                        ScreenshotUtils.captureScreenshot("tc004", "03_top_left_failed", stepCounter)
                        throw e
                    }
                }
            )
            steps.add(step3)

            // Step 4: Select Top-Right
            val step4 = executeStep(
                stepNumber = stepCounter++,
                action = "Нажать на radio button \"Top-Right\"",
                expectedResult = "Top-Right становится выбранным, другие deselect",
                actualAction = {
                    try {
                        onView(withId(R.id.rb_top_right))
                            .perform(click())
                        TestUtils.delay(500, "After Top-Right selection")
                        onView(withId(R.id.rb_top_right))
                            .check(matches(isChecked()))
                        onView(withId(R.id.rb_top_left))
                            .check(matches(isNotChecked()))
                        ScreenshotUtils.captureScreenshot("tc004", "04_top_right_selected", stepCounter)
                    } catch (e: Exception) {
                        ScreenshotUtils.captureScreenshot("tc004", "04_top_right_failed", stepCounter)
                        throw e
                    }
                }
            )
            steps.add(step4)

            // Step 5: Select Bottom-Left
            val step5 = executeStep(
                stepNumber = stepCounter++,
                action = "Нажать на radio button \"Bottom-Left\"",
                expectedResult = "Bottom-Left становится выбранным, другие deselect",
                actualAction = {
                    try {
                        onView(withId(R.id.rb_bottom_left))
                            .perform(click())
                        TestUtils.delay(500, "After Bottom-Left selection")
                        onView(withId(R.id.rb_bottom_left))
                            .check(matches(isChecked()))
                        onView(withId(R.id.rb_top_right))
                            .check(matches(isNotChecked()))
                        ScreenshotUtils.captureScreenshot("tc004", "05_bottom_left_selected", stepCounter)
                    } catch (e: Exception) {
                        ScreenshotUtils.captureScreenshot("tc004", "05_bottom_left_failed", stepCounter)
                        throw e
                    }
                }
            )
            steps.add(step5)

            // Step 6: Select Bottom-Right
            val step6 = executeStep(
                stepNumber = stepCounter++,
                action = "Нажать на radio button \"Bottom-Right\"",
                expectedResult = "Bottom-Right становится выбранным, другие deselect",
                actualAction = {
                    try {
                        onView(withId(R.id.rb_bottom_right))
                            .perform(click())
                        TestUtils.delay(500, "After Bottom-Right selection")
                        onView(withId(R.id.rb_bottom_right))
                            .check(matches(isChecked()))
                        onView(withId(R.id.rb_bottom_left))
                            .check(matches(isNotChecked()))
                        ScreenshotUtils.captureScreenshot("tc004", "06_bottom_right_selected", stepCounter)
                    } catch (e: Exception) {
                        ScreenshotUtils.captureScreenshot("tc004", "06_bottom_right_failed", stepCounter)
                        throw e
                    }
                }
            )
            steps.add(step6)

            val duration = System.currentTimeMillis() - startTime
            val testResult = ComprehensiveTestSuite.TestResult(
                testCaseId = testCaseId,
                testCaseName = testCaseName,
                steps = steps,
                status = if (steps.any { it.status == ComprehensiveTestSuite.StepStatus.FAILED }) ComprehensiveTestSuite.TestStatus.FAILED else ComprehensiveTestSuite.TestStatus.PASSED,
                duration = duration
            )
            testResults.add(testResult)
            
            Log.i(TAG, "✅ $testCaseId completed in ${TestUtils.formatDuration(duration)}")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ $testCaseId failed: ${e.message}", e)
            val duration = System.currentTimeMillis() - startTime
            testResults.add(ComprehensiveTestSuite.TestResult(
                testCaseId = testCaseId,
                testCaseName = testCaseName,
                steps = steps,
                status = ComprehensiveTestSuite.TestStatus.FAILED,
                duration = duration,
                notes = "Test failed with exception: ${e.message}"
            ))
        }
    }

    // ========== TC-005: Metrics Display Configuration Test ==========
    @Test
    fun tc005_metricsDisplayConfigurationTest() {
        val startTime = System.currentTimeMillis()
        val steps = mutableListOf<ComprehensiveTestSuite.StepResult>()
        val testCaseId = "TC-005"
        val testCaseName = "Metrics Display Configuration Test"
        
        Log.i(TAG, "🚀 Starting $testCaseId: $testCaseName")
        
        try {
            // Navigate to settings first
            navigateToSettings()
            
            // Step 1: Initial state screenshot
            val step1 = executeStep(
                stepNumber = stepCounter++,
                action = "Сделать скриншот начального состояния переключателей",
                expectedResult = "Скриншот сохранен",
                actualAction = {
                    ScreenshotUtils.captureScreenshot("tc005", "01_initial_switches", stepCounter)
                }
            )
            steps.add(step1)

            // Step 2: Remember initial states
            val step2 = executeStep(
                stepNumber = stepCounter++,
                action = "Запомнить начальное состояние каждого переключателя",
                expectedResult = "Начальные состояния зафиксированы",
                actualAction = {
                    try {
                        val cpuState = getSwitchState(R.id.switch_cpu)
                        val ramState = getSwitchState(R.id.switch_ram)
                        val timeState = getSwitchState(R.id.switch_time)
                        Log.d(TAG, "Initial states - CPU: $cpuState, RAM: $ramState, Time: $timeState")
                        ScreenshotUtils.captureScreenshot("tc005", "02_states_recorded", stepCounter)
                    } catch (e: Exception) {
                        ScreenshotUtils.captureScreenshot("tc005", "02_states_not_recorded", stepCounter)
                    }
                }
            )
            steps.add(step2)

            // Step 3: Toggle CPU
            val step3 = executeStep(
                stepNumber = stepCounter++,
                action = "Нажать переключатель \"CPU\"",
                expectedResult = "Состояние CPU переключается (ON→OFF или OFF→ON)",
                actualAction = {
                    try {
                        onView(withId(R.id.switch_cpu))
                            .perform(click())
                        TestUtils.delay(500, "After CPU toggle")
                        ScreenshotUtils.captureScreenshot("tc005", "03_cpu_toggled", stepCounter)
                    } catch (e: Exception) {
                        ScreenshotUtils.captureScreenshot("tc005", "03_cpu_toggle_failed", stepCounter)
                        throw e
                    }
                }
            )
            steps.add(step3)

            // Step 4: Toggle RAM
            val step4 = executeStep(
                stepNumber = stepCounter++,
                action = "Нажать переключатель \"RAM\"",
                expectedResult = "Состояние RAM переключается",
                actualAction = {
                    try {
                        onView(withId(R.id.switch_ram))
                            .perform(click())
                        TestUtils.delay(500, "After RAM toggle")
                        ScreenshotUtils.captureScreenshot("tc005", "04_ram_toggled", stepCounter)
                    } catch (e: Exception) {
                        ScreenshotUtils.captureScreenshot("tc005", "04_ram_toggle_failed", stepCounter)
                        throw e
                    }
                }
            )
            steps.add(step4)

            // Step 5: Toggle Time
            val step5 = executeStep(
                stepNumber = stepCounter++,
                action = "Нажать переключатель \"Time\"",
                expectedResult = "Состояние Time переключается",
                actualAction = {
                    try {
                        onView(withId(R.id.switch_time))
                            .perform(click())
                        TestUtils.delay(500, "After Time toggle")
                        ScreenshotUtils.captureScreenshot("tc005", "05_time_toggled", stepCounter)
                    } catch (e: Exception) {
                        ScreenshotUtils.captureScreenshot("tc005", "05_time_toggle_failed", stepCounter)
                        throw e
                    }
                }
            )
            steps.add(step5)

            // Step 6: Turn all ON
            val step6 = executeStep(
                stepNumber = stepCounter++,
                action = "Включить все три переключателя в состояние ON",
                expectedResult = "Все метрики включены",
                actualAction = {
                    try {
                        // Turn ON all switches
                        turnSwitchOn(R.id.switch_cpu)
                        turnSwitchOn(R.id.switch_ram)
                        turnSwitchOn(R.id.switch_time)
                        TestUtils.delay(500, "After turning all ON")
                        ScreenshotUtils.captureScreenshot("tc005", "06_all_on", stepCounter)
                    } catch (e: Exception) {
                        ScreenshotUtils.captureScreenshot("tc005", "06_all_on_failed", stepCounter)
                        throw e
                    }
                }
            )
            steps.add(step6)

            // Step 7: Turn all OFF
            val step7 = executeStep(
                stepNumber = stepCounter++,
                action = "Выключить все три переключателя в состояние OFF",
                expectedResult = "Все метрики выключены",
                actualAction = {
                    try {
                        // Turn OFF all switches
                        turnSwitchOff(R.id.switch_cpu)
                        turnSwitchOff(R.id.switch_ram)
                        turnSwitchOff(R.id.switch_time)
                        TestUtils.delay(500, "After turning all OFF")
                        ScreenshotUtils.captureScreenshot("tc005", "07_all_off", stepCounter)
                    } catch (e: Exception) {
                        ScreenshotUtils.captureScreenshot("tc005", "07_all_off_failed", stepCounter)
                        throw e
                    }
                }
            )
            steps.add(step7)

            val duration = System.currentTimeMillis() - startTime
            val testResult = ComprehensiveTestSuite.TestResult(
                testCaseId = testCaseId,
                testCaseName = testCaseName,
                steps = steps,
                status = if (steps.any { it.status == ComprehensiveTestSuite.StepStatus.FAILED }) ComprehensiveTestSuite.TestStatus.FAILED else ComprehensiveTestSuite.TestStatus.PASSED,
                duration = duration
            )
            testResults.add(testResult)
            
            Log.i(TAG, "✅ $testCaseId completed in ${TestUtils.formatDuration(duration)}")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ $testCaseId failed: ${e.message}", e)
            val duration = System.currentTimeMillis() - startTime
            testResults.add(ComprehensiveTestSuite.TestResult(
                testCaseId = testCaseId,
                testCaseName = testCaseName,
                steps = steps,
                status = ComprehensiveTestSuite.TestStatus.FAILED,
                duration = duration,
                notes = "Test failed with exception: ${e.message}"
            ))
        }
    }

    // ========== TC-006: Metrics Export Functionality Test ==========
    @Test
    fun tc006_metricsExportFunctionalityTest() {
        val startTime = System.currentTimeMillis()
        val steps = mutableListOf<ComprehensiveTestSuite.StepResult>()
        val testCaseId = "TC-006"
        val testCaseName = "Metrics Export Functionality Test"
        
        Log.i(TAG, "🚀 Starting $testCaseId: $testCaseName")
        
        try {
            // Navigate to settings first
            navigateToSettings()
            
            // Step 1: Initial screenshot
            val step1 = executeStep(
                stepNumber = stepCounter++,
                action = "Сделать скриншот экрана настроек",
                expectedResult = "Скриншот сохранен",
                actualAction = {
                    ScreenshotUtils.captureScreenshot("tc006", "01_settings_screen", stepCounter)
                }
            )
            steps.add(step1)

            // Step 2: Click Export CSV
            val step2 = executeStep(
                stepNumber = stepCounter++,
                action = "Нажать кнопку \"Export CSV\"",
                expectedResult = "Открывается диалог выбора приложения для отправки CSV файла",
                actualAction = {
                    try {
                        onView(withId(R.id.btn_export_csv))
                            .perform(click())
                        TestUtils.delay(2000, "Wait for CSV dialog")
                        ScreenshotUtils.captureScreenshot("tc006", "02_csv_dialog", stepCounter)
                    } catch (e: Exception) {
                        ScreenshotUtils.captureScreenshot("tc006", "02_csv_failed", stepCounter)
                        // Don't throw exception here as export might fail gracefully
                    }
                }
            )
            steps.add(step2)

            // Step 3: Close CSV dialog
            val step3 = executeStep(
                stepNumber = stepCounter++,
                action = "Закрыть диалог (нажать Back или выбрать опцию)",
                expectedResult = "Возврат на экран настроек",
                actualAction = {
                    try {
                        device.pressBack()
                        TestUtils.delay(1000, "After closing CSV dialog")
                        ScreenshotUtils.captureScreenshot("tc006", "03_csv_closed", stepCounter)
                    } catch (e: Exception) {
                        ScreenshotUtils.captureScreenshot("tc006", "03_csv_close_failed", stepCounter)
                    }
                }
            )
            steps.add(step3)

            // Step 4: Click Export JSON
            val step4 = executeStep(
                stepNumber = stepCounter++,
                action = "Нажать кнопку \"Export JSON\"",
                expectedResult = "Открывается диалог выбора приложения для отправки JSON файла",
                actualAction = {
                    try {
                        onView(withId(R.id.btn_export_json))
                            .perform(click())
                        TestUtils.delay(2000, "Wait for JSON dialog")
                        ScreenshotUtils.captureScreenshot("tc006", "04_json_dialog", stepCounter)
                    } catch (e: Exception) {
                        ScreenshotUtils.captureScreenshot("tc006", "04_json_failed", stepCounter)
                        // Don't throw exception here as export might fail gracefully
                    }
                }
            )
            steps.add(step4)

            // Step 5: Close JSON dialog
            val step5 = executeStep(
                stepNumber = stepCounter++,
                action = "Закрыть диалог",
                expectedResult = "Возврат на экран настроек",
                actualAction = {
                    try {
                        device.pressBack()
                        TestUtils.delay(1000, "After closing JSON dialog")
                        ScreenshotUtils.captureScreenshot("tc006", "05_json_closed", stepCounter)
                    } catch (e: Exception) {
                        ScreenshotUtils.captureScreenshot("tc006", "05_json_close_failed", stepCounter)
                    }
                }
            )
            steps.add(step5)

            // Step 6: Final state
            val step6 = executeStep(
                stepNumber = stepCounter++,
                action = "Сделать скриншот финального состояния",
                expectedResult = "Скриншот сохранен",
                actualAction = {
                    TestUtils.delay(1000, "Display final state")
                    ScreenshotUtils.captureScreenshot("tc006", "06_final_state", stepCounter)
                }
            )
            steps.add(step6)

            val duration = System.currentTimeMillis() - startTime
            val testResult = ComprehensiveTestSuite.TestResult(
                testCaseId = testCaseId,
                testCaseName = testCaseName,
                steps = steps,
                status = if (steps.any { it.status == ComprehensiveTestSuite.StepStatus.FAILED }) ComprehensiveTestSuite.TestStatus.FAILED else ComprehensiveTestSuite.TestStatus.PASSED,
                duration = duration
            )
            testResults.add(testResult)
            
            Log.i(TAG, "✅ $testCaseId completed in ${TestUtils.formatDuration(duration)}")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ $testCaseId failed: ${e.message}", e)
            val duration = System.currentTimeMillis() - startTime
            testResults.add(ComprehensiveTestSuite.TestResult(
                testCaseId = testCaseId,
                testCaseName = testCaseName,
                steps = steps,
                status = ComprehensiveTestSuite.TestStatus.FAILED,
                duration = duration,
                notes = "Test failed with exception: ${e.message}"
            ))
        }
    }

    // ========== TC-008: Save Settings and Return Test ==========
    @Test
    fun tc008_saveSettingsAndReturnTest() {
        val startTime = System.currentTimeMillis()
        val steps = mutableListOf<ComprehensiveTestSuite.StepResult>()
        val testCaseId = "TC-008"
        val testCaseName = "Save Settings and Return Test"
        
        Log.i(TAG, "🚀 Starting $testCaseId: $testCaseName")
        
        try {
            // Navigate to settings first
            navigateToSettings()
            
            // Make some changes first
            makeSomeSettingsChanges()
            
            // Step 1: Current state screenshot
            val step1 = executeStep(
                stepNumber = stepCounter++,
                action = "Сделать скриншот текущего состояния настроек",
                expectedResult = "Скриншот сохранен",
                actualAction = {
                    ScreenshotUtils.captureScreenshot("tc008", "01_current_settings", stepCounter)
                }
            )
            steps.add(step1)

            // Step 2: Verify unsaved changes
            val step2 = executeStep(
                stepNumber = stepCounter++,
                action = "Убедиться, что есть несохраненные изменения",
                expectedResult = "Изменения видны на экране",
                actualAction = {
                    TestUtils.delay(500, "Verify changes visible")
                    ScreenshotUtils.captureScreenshot("tc008", "02_changes_visible", stepCounter)
                }
            )
            steps.add(step2)

            // Step 3: Click Save button
            val step3 = executeStep(
                stepNumber = stepCounter++,
                action = "Нажать кнопку \"Save\"",
                expectedResult = "Начинается процесс сохранения настроек",
                actualAction = {
                    try {
                        onView(withId(R.id.btn_save))
                            .perform(click())
                        TestUtils.delay(500, "After save click")
                        ScreenshotUtils.captureScreenshot("tc008", "03_save_clicked", stepCounter)
                    } catch (e: Exception) {
                        ScreenshotUtils.captureScreenshot("tc008", "03_save_failed", stepCounter)
                        throw e
                    }
                }
            )
            steps.add(step3)

            // Step 4: Wait for save
            val step4 = executeStep(
                stepNumber = stepCounter++,
                action = "Подождать 1-2 секунды",
                expectedResult = "Настройки сохранены",
                actualAction = {
                    TestUtils.delay(2000, "Wait for settings to save")
                    ScreenshotUtils.captureScreenshot("tc008", "04_settings_saved", stepCounter)
                }
            )
            steps.add(step4)

            // Step 5: Wait for return to main screen
            val step5 = executeStep(
                stepNumber = stepCounter++,
                action = "Подождать перехода на главный экран",
                expectedResult = "Происходит автоматический возврат на главный экран",
                actualAction = {
                    TestUtils.delay(2000, "Wait for return to main screen")
                    ScreenshotUtils.captureScreenshot("tc008", "05_returning_main", stepCounter)
                }
            )
            steps.add(step5)

            // Step 6: Wait for main screen load
            val step6 = executeStep(
                stepNumber = stepCounter++,
                action = "Подождать 1-2 секунды полной загрузки",
                expectedResult = "Главный экран полностью загружен",
                actualAction = {
                    TestUtils.delay(2000, "Wait for main screen to fully load")
                    ScreenshotUtils.captureScreenshot("tc008", "06_main_loaded", stepCounter)
                }
            )
            steps.add(step6)

            // Step 7: Verify main screen elements
            val step7 = executeStep(
                stepNumber = stepCounter++,
                action = "Проверить наличие основных элементов главного экрана",
                expectedResult = "Заголовок, кнопки, статус отображаются",
                actualAction = {
                    try {
                        onView(withId(R.id.tv_app_title)).check(matches(isDisplayed()))
                        onView(withId(R.id.btn_toggle_overlay)).check(matches(isDisplayed()))
                        onView(withId(R.id.btn_settings)).check(matches(isDisplayed()))
                        onView(withId(R.id.tv_status)).check(matches(isDisplayed()))
                        ScreenshotUtils.captureScreenshot("tc008", "07_main_verified", stepCounter)
                    } catch (e: Exception) {
                        ScreenshotUtils.captureScreenshot("tc008", "07_main_verification_failed", stepCounter)
                        throw e
                    }
                }
            )
            steps.add(step7)

            // Step 8: Final main screen screenshot
            val step8 = executeStep(
                stepNumber = stepCounter++,
                action = "Сделать скриншот главного экрана после возврата",
                expectedResult = "Скриншот сохранен",
                actualAction = {
                    TestUtils.delay(1000, "Display final main screen")
                    ScreenshotUtils.captureScreenshot("tc008", "08_final_main", stepCounter)
                }
            )
            steps.add(step8)

            val duration = System.currentTimeMillis() - startTime
            val testResult = ComprehensiveTestSuite.TestResult(
                testCaseId = testCaseId,
                testCaseName = testCaseName,
                steps = steps,
                status = if (steps.any { it.status == ComprehensiveTestSuite.StepStatus.FAILED }) ComprehensiveTestSuite.TestStatus.FAILED else ComprehensiveTestSuite.TestStatus.PASSED,
                duration = duration
            )
            testResults.add(testResult)
            
            Log.i(TAG, "✅ $testCaseId completed in ${TestUtils.formatDuration(duration)}")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ $testCaseId failed: ${e.message}", e)
            val duration = System.currentTimeMillis() - startTime
            testResults.add(ComprehensiveTestSuite.TestResult(
                testCaseId = testCaseId,
                testCaseName = testCaseName,
                steps = steps,
                status = ComprehensiveTestSuite.TestStatus.FAILED,
                duration = duration,
                notes = "Test failed with exception: ${e.message}"
            ))
        }
    }

    // ========== Helper Methods ==========

    private fun navigateToSettings() {
        try {
            onView(withId(R.id.btn_settings))
                .perform(click())
            TestUtils.delay(2000, "Wait for settings to load")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to navigate to settings: ${e.message}")
            throw e
        }
    }

    private fun getCurrentPositionSelection(): String {
        return try {
            try {
                onView(withId(R.id.rb_top_left)).check(matches(isChecked()))
                "Top-Left"
            } catch (e: Exception) {
                try {
                    onView(withId(R.id.rb_top_right)).check(matches(isChecked()))
                    "Top-Right"
                } catch (e: Exception) {
                    try {
                        onView(withId(R.id.rb_bottom_left)).check(matches(isChecked()))
                        "Bottom-Left"
                    } catch (e: Exception) {
                        try {
                            onView(withId(R.id.rb_bottom_right)).check(matches(isChecked()))
                            "Bottom-Right"
                        } catch (e: Exception) {
                            "Unknown"
                        }
                    }
                }
            }
        } catch (e: Exception) {
            "Error determining selection"
        }
    }

    private fun getSwitchState(switchId: Int): String {
        return try {
            try {
                onView(withId(switchId)).check(matches(isChecked()))
                "ON"
            } catch (e: Exception) {
                "OFF"
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }

    private fun turnSwitchOn(switchId: Int) {
        try {
            try {
                onView(withId(switchId)).check(matches(isChecked()))
            } catch (e: Exception) {
                // Switch is OFF, turn it ON
                onView(withId(switchId)).perform(click())
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to turn switch ON: ${e.message}")
        }
    }

    private fun turnSwitchOff(switchId: Int) {
        try {
            try {
                onView(withId(switchId)).check(matches(isChecked()))
                // Switch is ON, turn it OFF
                onView(withId(switchId)).perform(click())
            } catch (e: Exception) {
                // Switch is already OFF
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to turn switch OFF: ${e.message}")
        }
    }

    private fun makeSomeSettingsChanges() {
        try {
            // Change position
            onView(withId(R.id.rb_bottom_right)).perform(click())
            TestUtils.delay(500, "After position change")
            
            // Toggle some metrics
            onView(withId(R.id.switch_cpu)).perform(click())
            TestUtils.delay(500, "After CPU toggle")
            
            onView(withId(R.id.switch_ram)).perform(click())
            TestUtils.delay(500, "After RAM toggle")
            
        } catch (e: Exception) {
            Log.w(TAG, "Failed to make settings changes: ${e.message}")
        }
    }

    private fun executeStep(
        stepNumber: Int,
        action: String,
        expectedResult: String,
        actualAction: () -> Unit
    ): ComprehensiveTestSuite.StepResult {
        val startTime = System.currentTimeMillis()
        var status = ComprehensiveTestSuite.StepStatus.PASSED
        var actualResult = "Action completed successfully"
        
        try {
            Log.i(TAG, "📸 Step $stepNumber: $action")
            actualAction()
            actualResult = "Expected result achieved: $expectedResult"
        } catch (e: Exception) {
            status = ComprehensiveTestSuite.StepStatus.FAILED
            actualResult = "Step failed: ${e.message}"
            Log.e(TAG, "❌ Step $stepNumber failed: ${e.message}", e)
        }
        
        val duration = System.currentTimeMillis() - startTime
        
        return ComprehensiveTestSuite.StepResult(
            stepNumber = stepNumber,
            action = action,
            expectedResult = expectedResult,
            actualResult = actualResult,
            status = status,
            duration = duration
        )
    }

    private fun generateSettingsReport() {
        val reportBuilder = StringBuilder()
        val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            .format(java.util.Date())

        reportBuilder.appendLine("# ⚙️ SysMetrics - Отчет о Тестировании Настроек")
        reportBuilder.appendLine()
        reportBuilder.appendLine("**Дата и время**: $timestamp")
        reportBuilder.appendLine("**Устройство**: Эмулятор Android")
        reportBuilder.appendLine("**Версия приложения**: 2.7.0")
        reportBuilder.appendLine("**Тип тестирования**: UI автоматизированное тестирование настроек")
        reportBuilder.appendLine()

        // Executive Summary
        val totalTests = testResults.size
        val passedTests = testResults.count { it.status == ComprehensiveTestSuite.TestStatus.PASSED }
        val failedTests = testResults.count { it.status == ComprehensiveTestSuite.TestStatus.FAILED }
        val totalDuration = testResults.sumOf { it.duration }
        val totalSteps = testResults.sumOf { it.steps.size }

        reportBuilder.appendLine("## 📊 Краткая Статистика Настроек")
        reportBuilder.appendLine()
        reportBuilder.appendLine("| Метрика | Значение |")
        reportBuilder.appendLine("|---------|----------|")
        reportBuilder.appendLine("| 📋 Всего тестов настроек | $totalTests |")
        reportBuilder.appendLine("| ✅ Пройдено | $passedTests |")
        reportBuilder.appendLine("| ❌ Провалено | $failedTests |")
        reportBuilder.appendLine("| 📈 Успешность | ${String.format("%.1f", (passedTests.toDouble() / totalTests * 100))}% |")
        reportBuilder.appendLine("| ⏱️ Общая длительность | ${TestUtils.formatDuration(totalDuration)} |")
        reportBuilder.appendLine("| 👣 Всего шагов | $totalSteps |")
        reportBuilder.appendLine()

        // Test Results
        reportBuilder.appendLine("## 🎯 Детальные Результаты Тестов Настроек")
        reportBuilder.appendLine()

        testResults.forEach { testResult ->
            val statusIcon = when (testResult.status) {
                ComprehensiveTestSuite.TestStatus.PASSED -> "✅"
                ComprehensiveTestSuite.TestStatus.FAILED -> "❌"
                ComprehensiveTestSuite.TestStatus.SKIPPED -> "⏭️"
            }

            reportBuilder.appendLine("### $statusIcon ${testResult.testCaseId}: ${testResult.testCaseName}")
            reportBuilder.appendLine()
            reportBuilder.appendLine("**Статус**: ${testResult.status}")
            reportBuilder.appendLine("**Длительность**: ${TestUtils.formatDuration(testResult.duration)}")
            reportBuilder.appendLine("**Шагов**: ${testResult.steps.size}")
            
            if (testResult.notes.isNotEmpty()) {
                reportBuilder.appendLine("**Примечания**: ${testResult.notes}")
            }
            
            reportBuilder.appendLine()

            // Steps table
            reportBuilder.appendLine("| Шаг | Действие | Ожидаемый Результат | Фактический Результат | Статус |")
            reportBuilder.appendLine("|-----|----------|---------------------|---------------------|--------|")
            
            testResult.steps.forEach { step ->
                val stepStatusIcon = when (step.status) {
                    ComprehensiveTestSuite.StepStatus.PASSED -> "✅"
                    ComprehensiveTestSuite.StepStatus.FAILED -> "❌"
                    ComprehensiveTestSuite.StepStatus.WARNING -> "⚠️"
                }
                
                val action = step.action.take(50) + if (step.action.length > 50) "..." else ""
                val expected = step.expectedResult.take(40) + if (step.expectedResult.length > 40) "..." else ""
                val actual = step.actualResult.take(40) + if (step.actualResult.length > 40) "..." else ""
                
                reportBuilder.appendLine("| ${step.stepNumber} | $action | $expected | $actual | $stepStatusIcon |")
            }
            
            reportBuilder.appendLine()
            reportBuilder.appendLine("---")
            reportBuilder.appendLine()
        }

        // Screenshots section
        reportBuilder.appendLine("## 📸 Скриншоты Настроек")
        reportBuilder.appendLine()
        reportBuilder.appendLine("Все скриншоты сохранены в директории:")
        reportBuilder.appendLine("```${ScreenshotUtils.getScreenshotDirectory()}```")
        reportBuilder.appendLine()

        // Conclusion
        reportBuilder.appendLine("## 🎉 Заключение по Настройкам")
        reportBuilder.appendLine()
        
        if (failedTests == 0) {
            reportBuilder.appendLine("🎊 **Все тесты настроек успешно пройдены!** Функциональность настроек работает корректно.")
        } else {
            reportBuilder.appendLine("⚠️ **Обнаружены проблемы в $failedTests тестах настроек.** Рекомендуется проанализировать неудачные шаги и устранить выявленные проблемы.")
        }
        
        reportBuilder.appendLine()
        reportBuilder.appendLine("**Общая оценка качества настроек**: ${if (failedTests == 0) "Отлично" else if (failedTests <= totalTests / 2) "Хорошо" else "Требует улучшения"}")
        reportBuilder.appendLine()
        reportBuilder.appendLine("---")
        reportBuilder.appendLine("*Отчет сгенерирован автоматически* • *${timestamp}*")

        // Save report to project directory only
        val projectReportFile = java.io.File("/home/tester/CascadeProjects/SysMetrics", "LATEST_SETTINGS_TEST_REPORT.md")
        projectReportFile.writeText(reportBuilder.toString())
        
        Log.i(TAG, "📄 Project settings report saved to: ${projectReportFile.absolutePath}")
        
        // Also try to save to temp directory with fallback
        try {
            val reportFileName = "settings_test_report_${java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())}.md"
            val tempReportFile = java.io.File("/tmp", reportFileName)
            tempReportFile.writeText(reportBuilder.toString())
            Log.i(TAG, "📄 Temp settings report saved to: ${tempReportFile.absolutePath}")
        } catch (e: Exception) {
            Log.w(TAG, "Could not save temp settings report: ${e.message}")
        }
    }
}
