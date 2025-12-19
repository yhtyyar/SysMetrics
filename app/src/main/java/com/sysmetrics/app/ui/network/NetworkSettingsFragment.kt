package com.sysmetrics.app.ui.network

import android.os.Bundle
import android.view.View
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SeekBarPreference
import androidx.preference.SwitchPreferenceCompat
import com.sysmetrics.app.R
import com.sysmetrics.app.core.di.DefaultDispatcherProvider
import com.sysmetrics.app.core.di.NetworkModule
import com.sysmetrics.app.data.model.network.NetworkAlertConfig
import com.sysmetrics.app.data.model.network.NetworkDisplayMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Settings fragment for network traffic monitoring configuration.
 * 
 * ## Settings:
 * - Display mode selection (Compact/Extended/Per-App/Combined)
 * - Update interval (500ms/1s/2s)
 * - Alert configuration (enable/thresholds)
 * - Data quota settings
 * - Native mode toggle
 */
class NetworkSettingsFragment : PreferenceFragmentCompat() {

    companion object {
        private const val TAG = "NET_SETTINGS_FRAG"

        // Preference keys
        const val KEY_DISPLAY_MODE = "network_display_mode"
        const val KEY_UPDATE_INTERVAL = "network_update_interval"
        const val KEY_USE_NATIVE = "network_use_native"
        const val KEY_ALERTS_ENABLED = "network_alerts_enabled"
        const val KEY_HIGH_SPEED_THRESHOLD = "network_high_speed_threshold"
        const val KEY_DAILY_QUOTA = "network_daily_quota"
        const val KEY_QUOTA_WARNING = "network_quota_warning"
        const val KEY_ANOMALY_DETECTION = "network_anomaly_detection"
        const val KEY_RESET_BASELINE = "network_reset_baseline"
        const val KEY_CLEAR_CACHE = "network_clear_cache"

        fun newInstance(): NetworkSettingsFragment = NetworkSettingsFragment()
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val networkModule by lazy {
        NetworkModule.getInstance(
            requireContext().applicationContext,
            DefaultDispatcherProvider()
        )
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.network_preferences, rootKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupPreferences()
        loadCurrentSettings()
    }

    private fun setupPreferences() {
        // Display mode
        findPreference<ListPreference>(KEY_DISPLAY_MODE)?.apply {
            entries = arrayOf("Compact", "Extended", "Per-App", "Combined")
            entryValues = NetworkDisplayMode.values().map { it.name }.toTypedArray()
            setOnPreferenceChangeListener { _, newValue ->
                Timber.tag(TAG).d("Display mode changed to: $newValue")
                true
            }
        }

        // Update interval
        findPreference<ListPreference>(KEY_UPDATE_INTERVAL)?.apply {
            entries = arrayOf("Fast (500ms)", "Normal (1s)", "Slow (2s)")
            entryValues = arrayOf("500", "1000", "2000")
            setOnPreferenceChangeListener { _, newValue ->
                Timber.tag(TAG).d("Update interval changed to: $newValue ms")
                true
            }
        }

        // Native mode toggle
        findPreference<SwitchPreferenceCompat>(KEY_USE_NATIVE)?.apply {
            isEnabled = networkModule.isNativeAvailable()
            if (!networkModule.isNativeAvailable()) {
                summary = "Native library not available on this device"
            }
        }

        // Alerts enabled
        findPreference<SwitchPreferenceCompat>(KEY_ALERTS_ENABLED)?.apply {
            setOnPreferenceChangeListener { _, newValue ->
                updateAlertConfig { it.copy(enabled = newValue as Boolean) }
                true
            }
        }

        // High speed threshold
        findPreference<SeekBarPreference>(KEY_HIGH_SPEED_THRESHOLD)?.apply {
            min = 10
            max = 500
            setOnPreferenceChangeListener { _, newValue ->
                updateAlertConfig { it.copy(highSpeedThresholdMbps = (newValue as Int).toFloat()) }
                true
            }
        }

        // Daily quota
        findPreference<EditTextPreference>(KEY_DAILY_QUOTA)?.apply {
            setOnPreferenceChangeListener { _, newValue ->
                val quotaMb = (newValue as String).toLongOrNull() ?: 0L
                updateAlertConfig { it.copy(dailyQuotaMb = quotaMb) }
                true
            }
        }

        // Quota warning percentage
        findPreference<SeekBarPreference>(KEY_QUOTA_WARNING)?.apply {
            min = 50
            max = 95
            setOnPreferenceChangeListener { _, newValue ->
                updateAlertConfig { it.copy(quotaWarningPercent = newValue as Int) }
                true
            }
        }

        // Anomaly detection
        findPreference<SwitchPreferenceCompat>(KEY_ANOMALY_DETECTION)?.apply {
            setOnPreferenceChangeListener { _, newValue ->
                updateAlertConfig { it.copy(anomalyDetectionEnabled = newValue as Boolean) }
                true
            }
        }

        // Reset baseline action
        findPreference<Preference>(KEY_RESET_BASELINE)?.apply {
            setOnPreferenceClickListener {
                networkModule.resetAll()
                Timber.tag(TAG).d("Baseline reset")
                true
            }
        }

        // Clear cache action
        findPreference<Preference>(KEY_CLEAR_CACHE)?.apply {
            setOnPreferenceClickListener {
                networkModule.clearCaches()
                Timber.tag(TAG).d("Cache cleared")
                true
            }
        }
    }

    private fun loadCurrentSettings() {
        scope.launch {
            try {
                val config = networkModule.provideMonitorNetworkTrafficUseCase().getAlertConfig()

                findPreference<SwitchPreferenceCompat>(KEY_ALERTS_ENABLED)?.isChecked = config.enabled
                findPreference<SeekBarPreference>(KEY_HIGH_SPEED_THRESHOLD)?.value = config.highSpeedThresholdMbps.toInt()
                findPreference<EditTextPreference>(KEY_DAILY_QUOTA)?.text = config.dailyQuotaMb.toString()
                findPreference<SeekBarPreference>(KEY_QUOTA_WARNING)?.value = config.quotaWarningPercent
                findPreference<SwitchPreferenceCompat>(KEY_ANOMALY_DETECTION)?.isChecked = config.anomalyDetectionEnabled

            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error loading settings")
            }
        }
    }

    private fun updateAlertConfig(transform: (NetworkAlertConfig) -> NetworkAlertConfig) {
        scope.launch {
            try {
                val useCase = networkModule.provideMonitorNetworkTrafficUseCase()
                val currentConfig = useCase.getAlertConfig()
                val newConfig = transform(currentConfig)
                useCase.setAlertConfig(newConfig)
                Timber.tag(TAG).d("Alert config updated: $newConfig")
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error updating alert config")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
