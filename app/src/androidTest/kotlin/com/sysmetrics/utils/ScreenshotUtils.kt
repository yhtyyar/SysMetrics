package com.sysmetrics.utils

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Enhanced screenshot utility for Kaspresso tests
 * Provides automatic screenshot capture with proper naming and organization
 */
object ScreenshotUtils {
    
    private const val TAG = "ScreenshotUtils"
    private const val SCREENSHOT_DIR = "test-screenshots"
    private var screenshotCounter = 1
    
    private lateinit var screenshotDir: File
    private lateinit var context: Context
    
    init {
        setup()
    }
    
    private fun setup() {
        context = ApplicationProvider.getApplicationContext()
        
        // Use app-specific external storage for better compatibility
        val externalFilesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        screenshotDir = File(externalFilesDir, SCREENSHOT_DIR)
        
        if (!screenshotDir.exists()) {
            val created = screenshotDir.mkdirs()
            Log.d(TAG, "Screenshot directory created: $created, path: ${screenshotDir.absolutePath}")
        }
        
        Log.d(TAG, "Screenshot setup complete - Directory: ${screenshotDir.absolutePath}")
        Log.d(TAG, "Directory exists: ${screenshotDir.exists()}, Writable: ${screenshotDir.canWrite()}")
    }
    
    /**
     * Capture screenshot with enhanced naming and metadata
     */
    fun captureScreenshot(
        testName: String,
        description: String,
        stepNumber: Int = 0
    ): String? {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
            val filename = if (stepNumber > 0) {
                String.format("%02d_%s_%s_%s.png", stepNumber, testName, description, timestamp)
            } else {
                String.format("%s_%s_%s.png", testName, description, timestamp)
            }
            
            val file = File(screenshotDir, filename)
            
            // Capture screenshot using UI Automator
            val screenshot = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
            
            FileOutputStream(file).use { out ->
                screenshot.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            
            val filePath = file.absolutePath
            Log.i(TAG, "✓ Screenshot saved: $filePath (${file.length()} bytes)")
            
            // Also copy to a more accessible location for debugging
            copyToDebugLocation(file, testName, description)
            
            filePath
        } catch (e: Exception) {
            Log.e(TAG, "✗ Failed to capture screenshot: ${e.message}", e)
            null
        }
    }
    
    /**
     * Copy screenshot to debug location for easier access during development
     */
    private fun copyToDebugLocation(originalFile: File, testName: String, description: String) {
        try {
            val debugDir = File("/sdcard/Download/sysmetrics_screenshots")
            if (!debugDir.exists()) {
                debugDir.mkdirs()
            }
            
            val debugFile = File(debugDir, originalFile.name)
            originalFile.copyTo(debugFile, overwrite = true)
            
            Log.d(TAG, "Debug copy saved: ${debugFile.absolutePath}")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to create debug copy: ${e.message}")
        }
    }
    
    /**
     * Get screenshot directory path for test reports
     */
    fun getScreenshotDirectory(): String {
        return screenshotDir.absolutePath
    }
    
    /**
     * Clean up old screenshots (keep only last 50)
     */
    fun cleanupOldScreenshots() {
        try {
            val files = screenshotDir.listFiles()?.sortedByDescending { it.lastModified() }
            files?.drop(50)?.forEach { file ->
                if (file.delete()) {
                    Log.d(TAG, "Deleted old screenshot: ${file.name}")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to cleanup old screenshots: ${e.message}")
        }
    }
    
    /**
     * Reset screenshot counter for new test
     */
    fun resetCounter() {
        screenshotCounter = 1
    }
    
    /**
     * Get next screenshot number
     */
    fun getNextScreenshotNumber(): Int {
        return screenshotCounter++
    }
}
