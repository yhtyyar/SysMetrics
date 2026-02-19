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
import androidx.test.espresso.matcher.ViewMatchers.withText
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
 * Demo Test with Enhanced Screenshots
 * Shows how the improved screenshot system works
 */
@RunWith(AndroidJUnit4::class)
class DemoTestWithScreenshots {

    companion object {
        private const val TAG = "DemoTest"
    }

    @get:Rule(order = 0)
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )

    @get:Rule(order = 1)
    val activityRule = ActivityScenarioRule(MainActivityOverlay::class.java)

    private var stepCounter = 1

    @Before
    fun setup() {
        Log.d(TAG, "=== TEST SETUP ===")
        ScreenshotUtils.resetCounter()
        ScreenshotUtils.captureScreenshot("demo_setup", "initial_state", 0)
        Log.d(TAG, "Setup complete, starting tests...")
    }

    @After
    fun teardown() {
        Log.d(TAG, "=== TEST TEARDOWN ===")
        ScreenshotUtils.captureScreenshot("demo_teardown", "final_state", 99)
        Log.d(TAG, "All tests completed!")
    }

    @Test
    fun demonstrateAppLaunchAndScreenshots() {
        val testName = "demonstrateAppLaunchAndScreenshots"
        Log.i(TAG, "🚀 Starting: $testName")

        // Step 1: Initial launch screenshot
        Log.i(TAG, "📸 Step 1: Taking initial screenshot")
        ScreenshotUtils.captureScreenshot(testName, "01_initial_launch", stepCounter++)
        
        TestUtils.delay(2000, "Display initial state")

        // Step 2: Verify and screenshot app title
        Log.i(TAG, "📸 Step 2: Verifying app title")
        ScreenshotUtils.captureScreenshot(testName, "02_before_title_check", stepCounter++)
        
        onView(withId(R.id.tv_app_title))
            .check(matches(isDisplayed()))
            .check(matches(withText(R.string.app_name)))
        
        TestUtils.delay(1500, "Show verified title")
        ScreenshotUtils.captureScreenshot(testName, "03_title_verified", stepCounter++)

        // Step 3: Verify and screenshot toggle button
        Log.i(TAG, "📸 Step 3: Verifying toggle button")
        ScreenshotUtils.captureScreenshot(testName, "04_before_toggle_check", stepCounter++)
        
        onView(withId(R.id.btn_toggle_overlay))
            .check(matches(isDisplayed()))
            .check(matches(withText(R.string.start_overlay)))
        
        TestUtils.delay(1500, "Show toggle button")
        ScreenshotUtils.captureScreenshot(testName, "05_toggle_verified", stepCounter++)

        // Step 4: Verify and screenshot settings button
        Log.i(TAG, "📸 Step 4: Verifying settings button")
        ScreenshotUtils.captureScreenshot(testName, "06_before_settings_check", stepCounter++)
        
        onView(withId(R.id.btn_settings))
            .check(matches(isDisplayed()))
        
        TestUtils.delay(1500, "Show settings button")
        ScreenshotUtils.captureScreenshot(testName, "07_settings_verified", stepCounter++)

        // Step 5: Screenshot before clicking toggle
        Log.i(TAG, "📸 Step 5: Before clicking toggle")
        ScreenshotUtils.captureScreenshot(testName, "08_before_toggle_click", stepCounter++)

        // Step 6: Click toggle button with delay
        Log.i(TAG, "📸 Step 6: Clicking toggle button")
        TestUtils.delay(1000, "Preparing to click toggle")
        
        onView(withId(R.id.btn_toggle_overlay))
            .perform(click())
        
        TestUtils.delay(2000, "Wait for overlay to start")
        ScreenshotUtils.captureScreenshot(testName, "09_after_toggle_click", stepCounter++)

        // Step 7: Verify overlay started state
        Log.i(TAG, "📸 Step 7: Verifying overlay started")
        TestUtils.delay(1500, "Wait for state update")
        
        onView(withId(R.id.tv_status))
            .check(matches(withText(R.string.overlay_status_on)))
        
        onView(withId(R.id.btn_toggle_overlay))
            .check(matches(withText(R.string.stop_overlay)))
        
        ScreenshotUtils.captureScreenshot(testName, "10_overlay_active", stepCounter++)

        // Step 8: Wait to show the overlay in action
        Log.i(TAG, "📸 Step 8: Showing overlay in action")
        TestUtils.delay(3000, "Display overlay running")
        ScreenshotUtils.captureScreenshot(testName, "11_overlay_running", stepCounter++)

        // Step 9: Stop the overlay
        Log.i(TAG, "📸 Step 9: Stopping overlay")
        ScreenshotUtils.captureScreenshot(testName, "12_before_stop_click", stepCounter++)
        
        onView(withId(R.id.btn_toggle_overlay))
            .perform(click())
        
        TestUtils.delay(2000, "Wait for overlay to stop")
        ScreenshotUtils.captureScreenshot(testName, "13_after_stop_click", stepCounter++)

        // Step 10: Verify overlay stopped
        Log.i(TAG, "📸 Step 10: Verifying overlay stopped")
        TestUtils.delay(1500, "Wait for final state")
        
        onView(withId(R.id.tv_status))
            .check(matches(withText(R.string.overlay_status_off)))
        
        onView(withId(R.id.btn_toggle_overlay))
            .check(matches(withText(R.string.start_overlay)))
        
        ScreenshotUtils.captureScreenshot(testName, "14_overlay_stopped", stepCounter++)

        // Step 11: Final state
        Log.i(TAG, "📸 Step 11: Final state")
        TestUtils.delay(2000, "Display final state")
        ScreenshotUtils.captureScreenshot(testName, "15_final_state", stepCounter++)

        Log.i(TAG, "🎉 Test completed successfully!")
        Log.i(TAG, "📁 Screenshots saved to: ${ScreenshotUtils.getScreenshotDirectory()}")
        Log.i(TAG, "📱 Debug copies available at: /sdcard/Download/sysmetrics_screenshots/")
    }

    @Test
    fun demonstrateSettingsNavigation() {
        val testName = "demonstrateSettingsNavigation"
        Log.i(TAG, "🚀 Starting: $testName")

        // Reset counter for this test
        stepCounter = 1

        // Step 1: Initial state
        Log.i(TAG, "📸 Step 1: Initial state")
        ScreenshotUtils.captureScreenshot(testName, "01_initial", stepCounter++)
        TestUtils.delay(1500, "Display initial state")

        // Step 2: Before clicking settings
        Log.i(TAG, "📸 Step 2: Before settings click")
        ScreenshotUtils.captureScreenshot(testName, "02_before_settings", stepCounter++)
        
        // Step 3: Click settings button
        Log.i(TAG, "📸 Step 3: Clicking settings")
        TestUtils.delay(1000, "Preparing to click settings")
        
        onView(withId(R.id.btn_settings))
            .perform(click())
        
        TestUtils.delay(2000, "Wait for settings to open")
        ScreenshotUtils.captureScreenshot(testName, "03_settings_opened", stepCounter++)

        // Step 4: Show settings screen
        Log.i(TAG, "📸 Step 4: Settings screen displayed")
        TestUtils.delay(3000, "Display settings screen")
        ScreenshotUtils.captureScreenshot(testName, "04_settings_displayed", stepCounter++)

        Log.i(TAG, "🎉 Settings navigation test completed!")
    }
}
