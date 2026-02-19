package com.sysmetrics.utils

import android.util.Log
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import java.util.concurrent.TimeUnit

/**
 * Utility functions for enhanced test execution with visual feedback
 */
object TestUtils {
    
    private const val TAG = "TestUtils"
    
    /**
     * Perform click with visual feedback and delay
     */
    fun ViewInteraction.clickWithFeedback(
        description: String = "Click action",
        delayBefore: Long = 500,
        delayAfter: Long = 1000
    ): ViewInteraction {
        Log.d(TAG, "→ $description - Starting")
        
        // Delay before action for visual preparation
        Thread.sleep(delayBefore)
        
        Log.d(TAG, "→ $description - Performing click")
        this.perform(ViewActions.click())
        
        // Delay after action to show result
        Thread.sleep(delayAfter)
        Log.d(TAG, "→ $description - Completed")
        
        return this
    }
    
    /**
     * Safe delay with logging
     */
    fun delay(millis: Long, description: String = "Delay") {
        Log.d(TAG, "⏸ $description: ${millis}ms")
        Thread.sleep(millis)
    }
    
    /**
     * Wait for view to be displayed with timeout
     */
    fun waitForView(
        viewInteraction: ViewInteraction,
        timeoutMs: Long = 5000,
        description: String = "View to appear"
    ) {
        Log.d(TAG, "⏳ Waiting for $description")
        val startTime = System.currentTimeMillis()
        
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            try {
                viewInteraction.check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
                Log.d(TAG, "✓ $description is now visible")
                return
            } catch (e: Exception) {
                Thread.sleep(200)
            }
        }
        
        Log.w(TAG, "⚠ Timeout waiting for $description")
    }
    
    /**
     * Log test step start
     */
    fun logStepStart(stepName: String, stepNumber: Int) {
        Log.i(TAG, "🚀 STEP $stepNumber: $stepName")
    }
    
    /**
     * Log test step completion
     */
    fun logStepComplete(stepName: String, stepNumber: Int) {
        Log.i(TAG, "✅ STEP $stepNumber COMPLETE: $stepName")
    }
    
    /**
     * Log test failure with context
     */
    fun logTestFailure(testName: String, error: Throwable) {
        Log.e(TAG, "❌ TEST FAILED: $testName", error)
    }
    
    /**
     * Log test success
     */
    fun logTestSuccess(testName: String) {
        Log.i(TAG, "🎉 TEST PASSED: $testName")
    }
    
    /**
     * Format duration for logging
     */
    fun formatDuration(durationMs: Long): String {
        val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs)
        val millis = durationMs % 1000
        return String.format("%d.%03ds", seconds, millis)
    }
}
