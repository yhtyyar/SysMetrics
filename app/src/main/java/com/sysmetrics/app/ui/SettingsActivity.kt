package com.sysmetrics.app.ui

import android.os.Bundle
import android.widget.SeekBar
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.sysmetrics.app.R
import com.sysmetrics.app.data.model.OverlayPosition
import com.sysmetrics.app.databinding.ActivitySettingsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Settings activity for configuring overlay appearance and behavior.
 */
@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupUI()
        observeState()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setTitle(R.string.settings)
        }
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun setupUI() {
        binding.apply {
            // Position radio group
            rgPosition.setOnCheckedChangeListener { _, checkedId ->
                val position = when (checkedId) {
                    R.id.rb_top_left -> OverlayPosition.TOP_LEFT
                    R.id.rb_top_right -> OverlayPosition.TOP_RIGHT
                    R.id.rb_bottom_left -> OverlayPosition.BOTTOM_LEFT
                    R.id.rb_bottom_right -> OverlayPosition.BOTTOM_RIGHT
                    else -> return@setOnCheckedChangeListener
                }
                viewModel.updateConfig(position = position)
            }

            // Update interval spinner values: 500ms, 1000ms, 2000ms
            val intervals = listOf(500L, 1000L, 2000L)
            rgInterval.setOnCheckedChangeListener { _, checkedId ->
                val interval = when (checkedId) {
                    R.id.rb_interval_500 -> 500L
                    R.id.rb_interval_1000 -> 1000L
                    R.id.rb_interval_2000 -> 2000L
                    else -> return@setOnCheckedChangeListener
                }
                viewModel.updateConfig(updateIntervalMs = interval)
            }

            // Opacity seekbar
            seekbarOpacity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        val opacity = progress / 100f
                        tvOpacityValue.text = getString(R.string.percent_int_format, progress)
                        viewModel.updateConfig(opacity = opacity)
                    }
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })

            // Metric toggles
            switchCpu.setOnCheckedChangeListener { _, isChecked ->
                viewModel.updateConfig(showCpu = isChecked)
            }
            switchRam.setOnCheckedChangeListener { _, isChecked ->
                viewModel.updateConfig(showRam = isChecked)
            }
            switchTemp.setOnCheckedChangeListener { _, isChecked ->
                viewModel.updateConfig(showTemperature = isChecked)
            }

            // Save button
            btnSave.setOnClickListener {
                viewModel.saveConfig()
                finish()
            }
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.overlayConfig.collect { config ->
                    binding.apply {
                        // Position
                        when (config.position) {
                            OverlayPosition.TOP_LEFT -> rbTopLeft.isChecked = true
                            OverlayPosition.TOP_RIGHT -> rbTopRight.isChecked = true
                            OverlayPosition.BOTTOM_LEFT -> rbBottomLeft.isChecked = true
                            OverlayPosition.BOTTOM_RIGHT -> rbBottomRight.isChecked = true
                        }

                        // Interval
                        when (config.updateIntervalMs) {
                            500L -> rbInterval500.isChecked = true
                            1000L -> rbInterval1000.isChecked = true
                            2000L -> rbInterval2000.isChecked = true
                        }

                        // Opacity
                        val opacityPercent = (config.opacity * 100).toInt()
                        seekbarOpacity.progress = opacityPercent
                        tvOpacityValue.text = getString(R.string.percent_int_format, opacityPercent)

                        // Metric toggles
                        switchCpu.isChecked = config.showCpu
                        switchRam.isChecked = config.showRam
                        switchTemp.isChecked = config.showTemperature
                    }
                }
            }
        }
    }
}
