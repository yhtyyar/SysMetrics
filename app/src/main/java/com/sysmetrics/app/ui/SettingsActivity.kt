package com.sysmetrics.app.ui

import android.os.Bundle
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
 * Settings activity - simplified for TV usage
 * Only position and metric toggles
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

            // Metric toggles
            switchCpu.setOnCheckedChangeListener { _, isChecked ->
                viewModel.updateConfig(showCpu = isChecked)
            }
            switchRam.setOnCheckedChangeListener { _, isChecked ->
                viewModel.updateConfig(showRam = isChecked)
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

                        // Metric toggles
                        switchCpu.isChecked = config.showCpu
                        switchRam.isChecked = config.showRam
                    }
                }
            }
        }
    }
}
