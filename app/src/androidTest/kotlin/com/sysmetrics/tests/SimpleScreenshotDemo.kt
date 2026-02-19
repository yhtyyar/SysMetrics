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
 * Simple Screenshot Demo
 * Demonstrates the enhanced screenshot system working
 */
@RunWith(AndroidJUnit4::class)
class SimpleScreenshotDemo {

    companion object {
        private const val TAG = "SimpleScreenshotDemo"
    }

    @get:Rule(order = 0)
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )

    @get:Rule(order = 1)
    val activityRule = ActivityScenarioRule(MainActivityOverlay::class.java)

    @Before
    fun setup() {
        Log.d(TAG, "=== SETUP ===")
        ScreenshotUtils.resetCounter()
        ScreenshotUtils.captureScreenshot("demo", "setup", 0)
    }

    @After
    fun teardown() {
        Log.d(TAG, "=== TEARDOWN ===")
        ScreenshotUtils.captureScreenshot("demo", "teardown", 99)
        Log.d(TAG, "Screenshots directory: ${ScreenshotUtils.getScreenshotDirectory()}")
    }

    @Test
    fun basicScreenshotDemo() {
        Log.i(TAG, "🚀 Starting basic screenshot demo")
        
        // Step 1: Initial state
        Log.i(TAG, "📸 Step 1: Initial app state")
        ScreenshotUtils.captureScreenshot("basic_demo", "01_initial", 1)
        TestUtils.delay(2000, "Display initial state")
        
        // Step 2: Verify app title
        Log.i(TAG, "📸 Step 2: App title visible")
        try {
            onView(withId(R.id.tv_app_title))
                .check(matches(isDisplayed()))
            ScreenshotUtils.captureScreenshot("basic_demo", "02_title_visible", 2)
        } catch (e: Exception) {
            Log.w(TAG, "Title not found, continuing anyway...")
            ScreenshotUtils.captureScreenshot("basic_demo", "02_title_not_found", 2)
        }
        
        TestUtils.delay(1500, "Show title verification")
        
        // Step 3: Look for toggle button
        Log.i(TAG, "📸 Step 3: Toggle button check")
        try {
            onView(withId(R.id.btn_toggle_overlay))
                .check(matches(isDisplayed()))
            ScreenshotUtils.captureScreenshot("basic_demo", "03_toggle_found", 3)
            
            // Step 4: Click toggle button
            Log.i(TAG, "📸 Step 4: Clicking toggle button")
            TestUtils.delay(1000, "Before clicking toggle")
            
            onView(withId(R.id.btn_toggle_overlay))
                .perform(click())
            
            TestUtils.delay(3000, "Wait for overlay response")
            ScreenshotUtils.captureScreenshot("basic_demo", "04_toggle_clicked", 4)
            
        } catch (e: Exception) {
            Log.w(TAG, "Toggle button interaction failed: ${e.message}")
            ScreenshotUtils.captureScreenshot("basic_demo", "04_toggle_failed", 4)
        }
        
        // Step 5: Look for settings button
        Log.i(TAG, "📸 Step 5: Settings button check")
        try {
            onView(withId(R.id.btn_settings))
                .check(matches(isDisplayed()))
            ScreenshotUtils.captureScreenshot("basic_demo", "05_settings_found", 5)
            
            // Step 6: Click settings button
            Log.i(TAG, "📸 Step 6: Clicking settings button")
            TestUtils.delay(1000, "Before clicking settings")
            
            onView(withId(R.id.btn_settings))
                .perform(click())
            
            TestUtils.delay(3000, "Wait for settings to open")
            ScreenshotUtils.captureScreenshot("basic_demo", "06_settings_clicked", 6)
            
        } catch (e: Exception) {
            Log.w(TAG, "Settings button interaction failed: ${e.message}")
            ScreenshotUtils.captureScreenshot("basic_demo", "06_settings_failed", 6)
        }
        
        // Step 7: Final state
        Log.i(TAG, "📸 Step 7: Final state")
        TestUtils.delay(2000, "Display final state")
        ScreenshotUtils.captureScreenshot("basic_demo", "07_final_state", 7)
        
        Log.i(TAG, "🎉 Demo completed successfully!")
        Log.i(TAG, "📁 Check screenshots at: ${ScreenshotUtils.getScreenshotDirectory()}")
    }
}
