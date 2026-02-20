package com.sysmetrics.utils

import android.util.Log
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.uiautomator.UiDevice
import androidx.test.platform.app.InstrumentationRegistry
import io.github.kakaocup.kakao.common.actions.KakaoActions
import io.github.kakaocup.kakao.common.assertions.KakaoAssertions

/**
 * Enhanced utilities for Kaspresso testing
 * Provides additional functionality for better test execution
 */
object KaspressoUtils {
    
    private const val TAG = "KaspressoUtils"
    
    /**
     * Enhanced click with visual feedback and logging
     */
    fun KakaoActions.clickWithFeedback(description: String = "Click action") {
        Log.d(TAG, "🔘 $description")
        perform(ViewActions.click())
        delay(500, "After $description")
    }
    
    /**
     * Enhanced assertion with logging
     */
    fun KakaoAssertions.assertWithFeedback(description: String = "Assertion") {
        Log.d(TAG, "✅ $description")
        check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        delay(300, "After $description")
    }
    
    /**
     * Safe view interaction with error handling
     */
    inline fun <T> safeInteraction(
        description: String = "Interaction",
        block: () -> T
    ): T? {
        return try {
            Log.d(TAG, "🔄 Starting: $description")
            val result = block()
            Log.d(TAG, "✅ Completed: $description")
            result
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Failed: $description - ${e.message}")
            null
        }
    }
    
    /**
     * Wait for view to appear with timeout
     */
    fun waitForView(
        viewMatcher: ViewMatcher,
        timeoutMs: Long = 5000,
        description: String = "View"
    ) {
        val startTime = System.currentTimeMillis()
        
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            try {
                onView(viewMatcher).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
                Log.d(TAG, "✅ Found: $description")
                return
            } catch (e: Exception) {
                delay(500, "Waiting for $description")
            }
        }
        
        Log.w(TAG, "⏰ Timeout waiting for: $description")
    }
    
    /**
     * Enhanced delay with logging
     */
    fun delay(ms: Long, description: String = "Delay") {
        Log.d(TAG, "⏱️ $description (${ms}ms)")
        Thread.sleep(ms)
    }
    
    /**
     * Capture screenshot with enhanced naming
     */
    fun captureScreenshot(
        testName: String,
        stepDescription: String,
        stepNumber: Int,
        subStep: Int = 0
    ) {
        val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault())
            .format(java.util.Date())
        
        val fileName = if (subStep > 0) {
            "${testName}_step${stepNumber}_${subStep}_${stepDescription}_${timestamp}.png"
        } else {
            "${testName}_step${stepNumber}_${stepDescription}_${timestamp}.png"
        }
        
        ScreenshotUtils.captureScreenshot(testName, "step${stepNumber}_${stepDescription}", stepNumber)
        Log.d(TAG, "📸 Screenshot captured: $fileName")
    }
    
    /**
     * Get device instance for UI Automator operations
     */
    fun getDevice(): UiDevice {
        return UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }
    
    /**
     * Handle system dialogs gracefully
     */
    fun handleSystemDialogs() {
        try {
            val device = getDevice()
            
            // Handle permission dialogs
            if (device.findObject(androidx.test.uiautomator.By.text("Allow")) != null) {
                device.findObject(androidx.test.uiautomator.By.text("Allow")).click()
                delay(1000, "Handle permission dialog")
            }
            
            // Handle other system dialogs
            if (device.findObject(androidx.test.uiautomator.By.text("OK")) != null) {
                device.findObject(androidx.test.uiautomator.By.text("OK")).click()
                delay(1000, "Handle OK dialog")
            }
            
        } catch (e: Exception) {
            Log.w(TAG, "Could not handle system dialogs: ${e.message}")
        }
    }
    
    /**
     * Verify element exists with safe interaction
     */
    fun verifyElementExists(
        viewMatcher: ViewMatcher,
        description: String = "Element"
    ): Boolean {
        return safeInteraction("Verify $description exists") {
            onView(viewMatcher).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
            true
        } ?: false
    }
    
    /**
     * Scroll to element if needed
     */
    fun scrollToElement(
        viewMatcher: ViewMatcher,
        description: String = "Element"
    ) {
        safeInteraction("Scroll to $description") {
            onView(viewMatcher).perform(ViewActions.scrollTo())
        }
    }
    
    /**
     * Type text with logging
     */
    fun typeText(
        viewMatcher: ViewMatcher,
        text: String,
        description: String = "Type text"
    ) {
        safeInteraction("Type text in $description") {
            onView(viewMatcher).perform(ViewActions.clearText(), ViewActions.typeText(text))
            delay(500, "After typing text")
        }
    }
    
    /**
     * Swipe gesture with logging
     */
    fun swipe(
        startCoordinates: androidx.test.espresso.action.GeneralLocation,
        endCoordinates: androidx.test.espresso.action.GeneralLocation,
        description: String = "Swipe"
    ) {
        safeInteraction("Perform $description") {
            onView(ViewMatchers.isRoot()).perform(
                androidx.test.espresso.action.GeneralSwipeAction(
                    androidx.test.espresso.action.Swipe.SLOW,
                    startCoordinates,
                    endCoordinates,
                    androidx.test.espresso.action.Press.FINGER
                )
            )
            delay(1000, "After $description")
        }
    }
    
    /**
     * Check if element is enabled
     */
    fun isElementEnabled(
        viewMatcher: ViewMatcher,
        description: String = "Element"
    ): Boolean {
        return safeInteraction("Check if $description is enabled") {
            onView(viewMatcher).check(ViewAssertions.matches(ViewMatchers.isEnabled()))
            true
        } ?: false
    }
    
    /**
     * Check if element is selected
     */
    fun isElementSelected(
        viewMatcher: ViewMatcher,
        description: String = "Element"
    ): Boolean {
        return safeInteraction("Check if $description is selected") {
            onView(viewMatcher).check(ViewAssertions.matches(ViewMatchers.isSelected()))
            true
        } ?: false
    }
}
