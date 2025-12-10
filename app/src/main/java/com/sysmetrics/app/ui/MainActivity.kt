package com.sysmetrics.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.sysmetrics.app.R
import com.sysmetrics.app.core.extensions.collectWhenStarted
import com.sysmetrics.app.core.extensions.hasOverlayPermission
import com.sysmetrics.app.core.extensions.showToast
import com.sysmetrics.app.core.extensions.startForegroundServiceCompat
import com.sysmetrics.app.databinding.ActivityMainBinding
import com.sysmetrics.app.service.OverlayService
import com.sysmetrics.app.ui.home.HomeTvFragment
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

/**
 * Main activity for SysMetrics.
 * Supports both traditional UI and Android TV optimized interface.
 * Use TV interface by default on Android TV devices.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private var binding: ActivityMainBinding? = null
    private val viewModel: MainViewModel by viewModels()
    
    private val useTvInterface = true // Set to true for TV interface

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (hasOverlayPermission()) {
            viewModel.setOverlayEnabled(true)
        } else {
            showToast(R.string.overlay_permission_denied)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Use TV interface if enabled
        if (useTvInterface) {
            setContentView(R.layout.activity_main_tv)
            if (savedInstanceState == null) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, HomeTvFragment())
                    .commit()
            }
        } else {
            // Traditional interface
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding!!.root)
            setupUI()
            observeState()
        }
    }
    
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Handle D-pad navigation for TV interface
        if (useTvInterface) {
            return super.onKeyDown(keyCode, event)
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun setupUI() {
        binding?.apply {
            btnToggleOverlay.setOnClickListener {
                handleOverlayToggle()
            }

            btnSettings.setOnClickListener {
                navigateToSettings()
            }
        }
    }

    private fun observeState() {
        viewModel.overlayEnabled.collectWhenStarted(this) { enabled ->
            updateOverlayButton(enabled)
            handleServiceState(enabled)
        }

        viewModel.systemMetrics.collectWhenStarted(this) { metrics ->
            updateMetricsDisplay(metrics)
        }
    }

    private fun updateMetricsDisplay(metrics: com.sysmetrics.app.data.model.SystemMetrics) {
        binding?.apply {
            tvCpuValue.text = getString(R.string.percent_format, metrics.cpuUsage)
            tvRamValue.text = getString(
                R.string.memory_format,
                metrics.ramUsedMb,
                metrics.ramTotalMb
            )
            tvRamPercent.text = getString(R.string.percent_format, metrics.ramUsagePercent)
            tvTempValue.text = if (metrics.temperatureCelsius > 0f) {
                getString(R.string.temp_format, metrics.temperatureCelsius)
            } else {
                getString(R.string.not_available)
            }
            tvCoresValue.text = metrics.cpuCores.toString()
        }
    }

    private fun handleOverlayToggle() {
        val isEnabled = viewModel.overlayEnabled.value
        when {
            isEnabled -> viewModel.setOverlayEnabled(false)
            hasOverlayPermission() -> viewModel.setOverlayEnabled(true)
            else -> requestOverlayPermission()
        }
    }

    private fun handleServiceState(enabled: Boolean) {
        if (enabled) {
            startOverlayService()
        } else {
            stopOverlayService()
        }
    }

    private fun updateOverlayButton(enabled: Boolean) {
        binding?.btnToggleOverlay?.apply {
            text = getString(if (enabled) R.string.stop_overlay else R.string.start_overlay)
            setBackgroundColor(getColor(if (enabled) R.color.stop_button else R.color.start_button))
        }
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            overlayPermissionLauncher.launch(intent)
        }
    }

    private fun navigateToSettings() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    private fun startOverlayService() {
        Timber.d("Starting overlay service")
        startForegroundServiceCompat(Intent(this, OverlayService::class.java))
    }

    private fun stopOverlayService() {
        Timber.d("Stopping overlay service")
        stopService(Intent(this, OverlayService::class.java))
    }
}
