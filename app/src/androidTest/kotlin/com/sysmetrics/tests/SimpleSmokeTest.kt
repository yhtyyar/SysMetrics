package com.sysmetrics.tests

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.rule.GrantPermissionRule
import com.sysmetrics.app.R
import com.sysmetrics.app.ui.MainActivityOverlay
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Smoke tests for SysMetrics with screenshot capture.
 *
 * Screenshots are saved to app-specific external storage:
 * /sdcard/Android/data/[package]/files/screenshots/
 *
 * Accessible via: adb pull /sdcard/Android/data/[package]/files/screenshots/
 */
@RunWith(AndroidJUnit4::class)
class SimpleSmokeTest {

    companion object {
        private const val TAG = "SimpleSmokeTest"
    }

    @get:Rule(order = 0)
    val testName: TestName = TestName()

    @get:Rule(order = 1)
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )

    @get:Rule(order = 2)
    val activityRule = ActivityScenarioRule(MainActivityOverlay::class.java)

    private lateinit var context: Context
    private lateinit var screenshotDir: File
    private var screenshotCount = 0

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        screenshotCount = 0
        
        // Get app-specific external files directory
        val externalFilesDir = context.getExternalFilesDir(null)
        screenshotDir = File(externalFilesDir, "screenshots")
        
        val created = screenshotDir.mkdirs()
        val canWrite = screenshotDir.canWrite()
        
        Log.d(TAG, "=== Screenshot Setup ===")
        Log.d(TAG, "SDK: ${Build.VERSION.SDK_INT}")
        Log.d(TAG, "Package: ${context.packageName}")
        Log.d(TAG, "Screenshot dir: ${screenshotDir.absolutePath}")
        Log.d(TAG, "Directory created: $created")
        Log.d(TAG, "Directory exists: ${screenshotDir.exists()}")
        Log.d(TAG, "Directory writable: $canWrite")
        Log.d(TAG, "========================")
    }

    @After
    fun teardown() {
        captureScreenshot("99_teardown")
    }

    @Test
    fun appLaunchesSuccessfully() {
        Log.d(TAG, "TEST: appLaunchesSuccessfully")
        captureScreenshot("01_initial")
        onView(withText("SysMetrics")).check(matches(isDisplayed()))
        captureScreenshot("01_success")
    }

    @Test
    fun toggleButtonIsDisplayed() {
        Log.d(TAG, "TEST: toggleButtonIsDisplayed")
        captureScreenshot("02_initial")
        onView(withId(R.id.btn_toggle_overlay)).check(matches(isDisplayed()))
        captureScreenshot("02_success")
    }

    @Test
    fun settingsButtonIsDisplayed() {
        Log.d(TAG, "TEST: settingsButtonIsDisplayed")
        captureScreenshot("03_initial")
        onView(withId(R.id.btn_settings)).check(matches(isDisplayed()))
        captureScreenshot("03_success")
    }

    @Test
    fun canClickToggleButton() {
        Log.d(TAG, "TEST: canClickToggleButton")
        captureScreenshot("04_initial")
        onView(withId(R.id.btn_toggle_overlay)).perform(click())
        Thread.sleep(1000)
        captureScreenshot("04_after_click")
        onView(withId(R.id.btn_toggle_overlay)).check(matches(isDisplayed()))
        captureScreenshot("04_final")
    }

    private fun captureScreenshot(description: String) {
        screenshotCount++
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val filename = String.format("%02d_%s_%s_%s.png", screenshotCount, testName.methodName, description, timestamp)
        
        Log.d(TAG, "[$screenshotCount] Capturing: $filename")
        
        activityRule.scenario.onActivity { activity ->
            try {
                val decorView = activity.window.decorView
                decorView.isDrawingCacheEnabled = true
                decorView.buildDrawingCache()
                
                val bitmap = Bitmap.createBitmap(decorView.drawingCache)
                decorView.isDrawingCacheEnabled = false
                decorView.destroyDrawingCache()
                
                val file = File(screenshotDir, filename)
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                
                Log.i(TAG, "[$screenshotCount] SAVED: ${file.absolutePath} (${file.length()} bytes)")
            } catch (e: Exception) {
                Log.e(TAG, "[$screenshotCount] FAILED: ${e.message}", e)
            }
        }
    }
}
