package com.sysmetrics.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.sysmetrics.app.R
import com.sysmetrics.app.databinding.ActivityMainBinding
import com.sysmetrics.app.service.OverlayService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Main activity for SysMetrics.
 * Provides controls for starting/stopping the overlay service
 * and displays a preview of system metrics.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (hasOverlayPermission()) {
            startOverlayService()
        } else {
            Toast.makeText(
                this,
                R.string.overlay_permission_denied,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        observeState()
    }

    private fun setupUI() {
        binding.apply {
            btnToggleOverlay.setOnClickListener {
                handleOverlayToggle()
            }

            btnSettings.setOnClickListener {
                startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
            }
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.overlayEnabled.collect { enabled ->
                        updateOverlayButton(enabled)
                        if (enabled) {
                            startOverlayService()
                        } else {
                            stopOverlayService()
                        }
                    }
                }

                launch {
                    viewModel.systemMetrics.collect { metrics ->
                        binding.apply {
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
                }
            }
        }
    }

    private fun handleOverlayToggle() {
        if (!viewModel.overlayEnabled.value) {
            if (hasOverlayPermission()) {
                viewModel.setOverlayEnabled(true)
            } else {
                requestOverlayPermission()
            }
        } else {
            viewModel.setOverlayEnabled(false)
        }
    }

    private fun updateOverlayButton(enabled: Boolean) {
        binding.btnToggleOverlay.apply {
            text = getString(if (enabled) R.string.stop_overlay else R.string.start_overlay)
            setBackgroundColor(
                getColor(if (enabled) R.color.stop_button else R.color.start_button)
            )
        }
    }

    private fun hasOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
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

    private fun startOverlayService() {
        Timber.d("Starting overlay service")
        val intent = Intent(this, OverlayService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun stopOverlayService() {
        Timber.d("Stopping overlay service")
        stopService(Intent(this, OverlayService::class.java))
    }
}
