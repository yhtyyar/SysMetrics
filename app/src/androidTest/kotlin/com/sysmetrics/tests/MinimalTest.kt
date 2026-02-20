package com.sysmetrics.tests

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sysmetrics.app.ui.MainActivityOverlay
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Minimal test to verify CI/CD compilation
 */
@RunWith(AndroidJUnit4::class)
class MinimalTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivityOverlay::class.java)

    @Test
    fun basicAppLaunchTest() {
        // Minimal test - just launch activity
        // This should compile and run without issues
    }
}
