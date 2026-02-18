package com.sysmetrics.utils

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import androidx.test.platform.app.InstrumentationRegistry
import com.kaspersky.kaspresso.testcases.core.testcontext.TestContext
import timber.log.Timber

/**
 * Utility class for Android TV specific operations
 */
object TvNavigationUtils {

    private const val TAG = "TvNavigationUtils"

    /**
     * Check if the device is an Android TV
     */
    fun isTvDevice(): Boolean {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        val uiMode = uiModeManager?.currentModeType ?: Configuration.UI_MODE_TYPE_NORMAL
        return uiMode == Configuration.UI_MODE_TYPE_TELEVISION
    }

    /**
     * Navigate using D-Pad in test context
     */
    fun TestContext<*>.pressDPadUp() {
        step("Press D-Pad UP") {
            device.uiDevice.pressDPadUp()
            Timber.tag(TAG).d("D-Pad UP pressed")
        }
    }

    fun TestContext<*>.pressDPadDown() {
        step("Press D-Pad DOWN") {
            device.uiDevice.pressDPadDown()
            Timber.tag(TAG).d("D-Pad DOWN pressed")
        }
    }

    fun TestContext<*>.pressDPadLeft() {
        step("Press D-Pad LEFT") {
            device.uiDevice.pressDPadLeft()
            Timber.tag(TAG).d("D-Pad LEFT pressed")
        }
    }

    fun TestContext<*>.pressDPadRight() {
        step("Press D-Pad RIGHT") {
            device.uiDevice.pressDPadRight()
            Timber.tag(TAG).d("D-Pad RIGHT pressed")
        }
    }

    fun TestContext<*>.pressDPadCenter() {
        step("Press D-Pad CENTER/OK") {
            device.uiDevice.pressDPadCenter()
            Timber.tag(TAG).d("D-Pad CENTER pressed")
        }
    }

    fun TestContext<*>.pressBack() {
        step("Press BACK button") {
            device.uiDevice.pressBack()
            Timber.tag(TAG).d("BACK button pressed")
        }
    }

    fun TestContext<*>.pressHome() {
        step("Press HOME button") {
            device.uiDevice.pressHome()
            Timber.tag(TAG).d("HOME button pressed")
        }
    }

    fun TestContext<*>.pressMenu() {
        step("Press MENU button") {
            device.uiDevice.pressMenu()
            Timber.tag(TAG).d("MENU button pressed")
        }
    }
}
