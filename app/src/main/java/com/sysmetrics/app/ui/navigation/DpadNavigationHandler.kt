package com.sysmetrics.app.ui.navigation

import android.app.Activity
import android.view.KeyEvent

/**
 * Handles D-pad navigation for Android TV remote control
 * Provides consistent navigation experience across the app
 */
class DpadNavigationHandler(private val activity: Activity) {

    private var navigationListener: NavigationListener? = null

    interface NavigationListener {
        /**
         * Called when UP button is pressed
         */
        fun onDpadUp(): Boolean = false

        /**
         * Called when DOWN button is pressed
         */
        fun onDpadDown(): Boolean = false

        /**
         * Called when LEFT button is pressed
         */
        fun onDpadLeft(): Boolean = false

        /**
         * Called when RIGHT button is pressed
         */
        fun onDpadRight(): Boolean = false

        /**
         * Called when CENTER/OK button is pressed
         */
        fun onDpadCenter(): Boolean = false

        /**
         * Called when BACK button is pressed
         */
        fun onBackPressed(): Boolean = false

        /**
         * Called when MENU button is pressed
         */
        fun onMenuPressed(): Boolean = false
    }

    /**
     * Set navigation listener
     */
    fun setNavigationListener(listener: NavigationListener) {
        this.navigationListener = listener
    }

    /**
     * Handle key event from remote control
     * @return true if event was handled, false otherwise
     */
    fun handleKeyEvent(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) {
            return false
        }

        val listener = navigationListener ?: return false

        return when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> {
                listener.onDpadUp()
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                listener.onDpadDown()
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                listener.onDpadLeft()
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                listener.onDpadRight()
            }
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                listener.onDpadCenter()
            }
            KeyEvent.KEYCODE_BACK -> {
                listener.onBackPressed()
            }
            KeyEvent.KEYCODE_MENU -> {
                listener.onMenuPressed()
            }
            else -> false
        }
    }

    /**
     * Check if event is navigation key
     */
    fun isNavigationKey(keyCode: Int): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER,
            KeyEvent.KEYCODE_BACK,
            KeyEvent.KEYCODE_MENU -> true
            else -> false
        }
    }
}
