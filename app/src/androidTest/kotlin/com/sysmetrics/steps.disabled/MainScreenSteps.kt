package com.sysmetrics.steps

import com.kaspersky.components.kaspresso.testcases.core.testcontext.TestContext
import com.sysmetrics.screens.MainScreen
import com.sysmetrics.utils.TvNavigationUtils.isTvDevice
import com.sysmetrics.utils.pressDPadCenter
import timber.log.Timber

/**
 * Common steps for Main Screen interactions
 */
object MainScreenSteps {

    private const val TAG = "MainScreenSteps"

    /**
     * Start the overlay service
     */
    fun TestContext<*>.startOverlay() {
        step("Start overlay service") {
            MainScreen {
                if (isTvDevice()) {
                    // Use D-Pad for TV
                    pressDPadCenter()
                } else {
                    toggleButton.click()
                }
            }
            // Wait for service to start
            flakySafely(timeout = 5000) {
                MainScreen.verifyOverlayStatusOn()
            }
            Timber.tag(TAG).d("Overlay started successfully")
        }
    }

    /**
     * Stop the overlay service
     */
    fun TestContext<*>.stopOverlay() {
        step("Stop overlay service") {
            MainScreen {
                if (isTvDevice()) {
                    pressDPadCenter()
                } else {
                    toggleButton.click()
                }
            }
            // Wait for service to stop
            flakySafely(timeout = 5000) {
                MainScreen.verifyOverlayStatusOff()
            }
            Timber.tag(TAG).d("Overlay stopped successfully")
        }
    }

    /**
     * Navigate to Settings screen
     */
    fun TestContext<*>.openSettings() {
        step("Navigate to Settings screen") {
            MainScreen {
                settingsButton {
                    isVisible()
                    click()
                }
            }
            Timber.tag(TAG).d("Navigated to Settings")
        }
    }

    /**
     * Verify app launch and initial state
     */
    fun TestContext<*>.verifyAppLaunch() {
        step("Verify app launched correctly") {
            MainScreen {
                appTitle {
                    isVisible()
                }
                statusText {
                    isVisible()
                }
                toggleButton {
                    isVisible()
                    isEnabled()
                }
                settingsButton {
                    isVisible()
                    isEnabled()
                }
            }
            Timber.tag(TAG).d("App launched verification passed")
        }
    }
}
