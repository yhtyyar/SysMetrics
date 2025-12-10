package com.sysmetrics.app.ui

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreferenceCompat
import com.sysmetrics.app.R
import com.sysmetrics.app.core.extensions.hasOverlayPermission
import com.sysmetrics.app.core.extensions.showToast
import com.sysmetrics.app.service.MinimalistOverlayService
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

/**
 * Main activity for SysMetrics SystemOverlay
 * Simple preference-based UI following TvOverlay_cpu pattern
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_simple)
        
        // Load preferences fragment
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings_container, SettingsFragment())
                .commit()
        }

        supportActionBar?.title = getString(R.string.app_name)
    }
    
    /**
     * Settings Fragment with overlay control
     */
    class SettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {
        
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
            
            // Handle overlay enabled toggle
            findPreference<SwitchPreferenceCompat>("overlay_enabled")?.apply {
                setOnPreferenceChangeListener { _, newValue ->
                    val isEnabled = newValue as Boolean
                    handleOverlayToggle(isEnabled)
                    true
                }
            }
        }

        override fun onResume() {
            super.onResume()
            preferenceManager.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onPause() {
            super.onPause()
            preferenceManager.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            // Handle preference changes if needed
        }

        private fun handleOverlayToggle(enabled: Boolean) {
            val context = requireContext()
            
            if (enabled) {
                // Check overlay permission
                if (!context.hasOverlayPermission()) {
                    requestOverlayPermission()
                    // Revert switch
                    findPreference<SwitchPreferenceCompat>("overlay_enabled")?.isChecked = false
                    return
                }
                
                // Start service
                startOverlayService()
            } else {
                // Stop service
                stopOverlayService()
            }
        }

        private fun requestOverlayPermission() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${requireContext().packageName}")
                )
                startActivity(intent)
                requireContext().showToast("Please grant overlay permission and try again")
            }
        }

        private fun startOverlayService() {
            try {
                val context = requireContext()
                val serviceIntent = Intent(context, MinimalistOverlayService::class.java)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }

                Timber.d("Overlay service started")
                context.showToast("Overlay started")
            } catch (e: Exception) {
                Timber.e(e, "Failed to start overlay service")
                requireContext().showToast("Failed to start overlay: ${e.message}")
            }
        }

        private fun stopOverlayService() {
            try {
                val context = requireContext()
                val serviceIntent = Intent(context, MinimalistOverlayService::class.java)
                context.stopService(serviceIntent)

                Timber.d("Overlay service stopped")
                context.showToast("Overlay stopped")
            } catch (e: Exception) {
                Timber.e(e, "Failed to stop overlay service")
            }
        }
    }
}
