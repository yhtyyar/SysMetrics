package com.sysmetrics.app.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.sysmetrics.app.R
import com.sysmetrics.app.core.SysMetricsApplication
import com.sysmetrics.app.data.model.OverlayPosition
import com.sysmetrics.app.data.repository.MetricsHistoryRepository
import com.sysmetrics.app.databinding.ActivitySettingsBinding
import com.sysmetrics.app.domain.usecase.ExportMetricsUseCase
import com.sysmetrics.app.worker.MetricsCollectionWorker
import kotlinx.coroutines.launch

/**
 * Settings activity - with export and background collection
 */
// @AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var viewModel: SettingsViewModel
    private var exportUseCase: ExportMetricsUseCase? = null
    private var isBackgroundCollectionEnabled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize dependencies from AppContainer
        val appContainer = (application as SysMetricsApplication).appContainer
        val factory = SettingsViewModelFactory(appContainer.manageOverlayConfigUseCase)
        viewModel = ViewModelProvider(this, factory)[SettingsViewModel::class.java]

        setupToolbar()
        setupUI()
        setupExport()
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
            switchTime.setOnCheckedChangeListener { _, isChecked ->
                viewModel.updateConfig(showTime = isChecked)
            }

            // Save button
            btnSave.setOnClickListener {
                viewModel.saveConfig()
                finish()
            }
        }
    }

    private fun setupExport() {
        binding.apply {
            // Export CSV button
            btnExportCsv.setOnClickListener {
                exportMetrics(ExportMetricsUseCase.ExportFormat.CSV)
            }
            
            // Export JSON button
            btnExportJson.setOnClickListener {
                exportMetrics(ExportMetricsUseCase.ExportFormat.JSON)
            }
            
            // Background collection toggle
            switchBackgroundCollection.setOnCheckedChangeListener { _, isChecked ->
                isBackgroundCollectionEnabled = isChecked
                if (isChecked) {
                    MetricsCollectionWorker.schedule(this@SettingsActivity)
                    Toast.makeText(this@SettingsActivity, "Background collection enabled", Toast.LENGTH_SHORT).show()
                } else {
                    MetricsCollectionWorker.cancel(this@SettingsActivity)
                    Toast.makeText(this@SettingsActivity, "Background collection disabled", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun exportMetrics(format: ExportMetricsUseCase.ExportFormat) {
        lifecycleScope.launch {
            try {
                val historyRepository = MetricsHistoryRepository(
                    com.sysmetrics.app.data.local.MetricsDatabase.getInstance(this@SettingsActivity).metricsHistoryDao()
                )
                val useCase = ExportMetricsUseCase(this@SettingsActivity, historyRepository)
                
                val result = useCase.export(24, format)
                
                if (result.success && result.shareIntent != null) {
                    startActivity(Intent.createChooser(result.shareIntent, "Share Export"))
                } else {
                    Toast.makeText(
                        this@SettingsActivity,
                        result.errorMessage ?: getString(R.string.export_error),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@SettingsActivity, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
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
                        switchTime.isChecked = config.showTime
                    }
                }
            }
        }
    }
}
