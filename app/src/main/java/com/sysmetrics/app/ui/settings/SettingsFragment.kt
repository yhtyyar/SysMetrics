package com.sysmetrics.app.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider
import com.google.android.material.switchmaterial.SwitchMaterial
import com.sysmetrics.app.R
import com.sysmetrics.app.core.SysMetricsApplication
import com.sysmetrics.app.data.model.advanced.*
import com.sysmetrics.app.data.repository.SettingsRepository
import com.sysmetrics.app.data.source.AdvancedPreferencesDataSource
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Settings Fragment for advanced monitoring configuration.
 * Provides controls for all 7 advanced features.
 */
class SettingsFragment : Fragment() {
    
    private val viewModel: SettingsViewModel by viewModels {
        val app = requireActivity().application as SysMetricsApplication
        val prefsDataSource = AdvancedPreferencesDataSource(requireContext())
        val repository = SettingsRepository(prefsDataSource)
        SettingsViewModel.Factory(repository)
    }
    
    // UI Elements
    private lateinit var spinnerUpdateInterval: Spinner
    private lateinit var switchCpuChart: SwitchMaterial
    private lateinit var switchRamChart: SwitchMaterial
    private lateinit var switchTempChart: SwitchMaterial
    private lateinit var switchNetworkChart: SwitchMaterial
    private lateinit var switchFpsChart: SwitchMaterial
    private lateinit var spinnerChartHeight: Spinner
    private lateinit var switchPeakNotifications: SwitchMaterial
    private lateinit var spinnerPeakInterval: Spinner
    private lateinit var switchCpuPeak: SwitchMaterial
    private lateinit var switchRamPeak: SwitchMaterial
    private lateinit var switchTempPeak: SwitchMaterial
    private lateinit var switchNetPeak: SwitchMaterial
    private lateinit var sliderToastDuration: Slider
    private lateinit var textToastDuration: TextView
    private lateinit var switchFpsMonitoring: SwitchMaterial
    private lateinit var switchJankDetection: SwitchMaterial
    private lateinit var sliderFpsThreshold: Slider
    private lateinit var textFpsThreshold: TextView
    private lateinit var switch30sAvg: SwitchMaterial
    private lateinit var switch1mAvg: SwitchMaterial
    private lateinit var switch5mAvg: SwitchMaterial
    private lateinit var switchPercentiles: SwitchMaterial
    private lateinit var spinnerExportFormat: Spinner
    private lateinit var spinnerExportRange: Spinner
    private lateinit var btnResetDefaults: Button
    private lateinit var progressBar: ProgressBar
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindViews(view)
        setupSpinners()
        setupListeners()
        observeViewModel()
    }
    
    private fun bindViews(view: View) {
        spinnerUpdateInterval = view.findViewById(R.id.spinner_update_interval)
        switchCpuChart = view.findViewById(R.id.switch_cpu_chart)
        switchRamChart = view.findViewById(R.id.switch_ram_chart)
        switchTempChart = view.findViewById(R.id.switch_temp_chart)
        switchNetworkChart = view.findViewById(R.id.switch_network_chart)
        switchFpsChart = view.findViewById(R.id.switch_fps_chart)
        spinnerChartHeight = view.findViewById(R.id.spinner_chart_height)
        switchPeakNotifications = view.findViewById(R.id.switch_peak_notifications)
        spinnerPeakInterval = view.findViewById(R.id.spinner_peak_interval)
        switchCpuPeak = view.findViewById(R.id.switch_cpu_peak)
        switchRamPeak = view.findViewById(R.id.switch_ram_peak)
        switchTempPeak = view.findViewById(R.id.switch_temp_peak)
        switchNetPeak = view.findViewById(R.id.switch_net_peak)
        sliderToastDuration = view.findViewById(R.id.slider_toast_duration)
        textToastDuration = view.findViewById(R.id.text_toast_duration)
        switchFpsMonitoring = view.findViewById(R.id.switch_fps_monitoring)
        switchJankDetection = view.findViewById(R.id.switch_jank_detection)
        sliderFpsThreshold = view.findViewById(R.id.slider_fps_threshold)
        textFpsThreshold = view.findViewById(R.id.text_fps_threshold)
        switch30sAvg = view.findViewById(R.id.switch_30s_avg)
        switch1mAvg = view.findViewById(R.id.switch_1m_avg)
        switch5mAvg = view.findViewById(R.id.switch_5m_avg)
        switchPercentiles = view.findViewById(R.id.switch_percentiles)
        spinnerExportFormat = view.findViewById(R.id.spinner_export_format)
        spinnerExportRange = view.findViewById(R.id.spinner_export_range)
        btnResetDefaults = view.findViewById(R.id.btn_reset_defaults)
        progressBar = view.findViewById(R.id.progress_bar)
    }
    
    private fun setupSpinners() {
        // Update Interval Spinner
        val intervalAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            UpdateInterval.entries.map { "${it.icon} ${it.displayName}" }
        ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        spinnerUpdateInterval.adapter = intervalAdapter
        
        // Chart Height Spinner
        val heightAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            ChartHeight.entries.map { it.displayName }
        ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        spinnerChartHeight.adapter = heightAdapter
        
        // Peak Interval Spinner
        val peakIntervalAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            PeakNotificationInterval.entries.map { it.displayName }
        ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        spinnerPeakInterval.adapter = peakIntervalAdapter
        
        // Export Format Spinner
        val formatAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            listOf("CSV", "TXT", "JSON")
        ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        spinnerExportFormat.adapter = formatAdapter
        
        // Export Range Spinner
        val rangeAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            TimeRange.entries.map { it.displayName }
        ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        spinnerExportRange.adapter = rangeAdapter
    }
    
    private fun setupListeners() {
        // Update Interval
        spinnerUpdateInterval.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                viewModel.updateUpdateInterval(UpdateInterval.entries[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        
        // Chart Toggles
        switchCpuChart.setOnCheckedChangeListener { _, isChecked -> 
            viewModel.updateChartSettings(showCpu = isChecked) 
        }
        switchRamChart.setOnCheckedChangeListener { _, isChecked -> 
            viewModel.updateChartSettings(showRam = isChecked) 
        }
        switchTempChart.setOnCheckedChangeListener { _, isChecked -> 
            viewModel.updateChartSettings(showTemp = isChecked) 
        }
        switchNetworkChart.setOnCheckedChangeListener { _, isChecked -> 
            viewModel.updateChartSettings(showNetwork = isChecked) 
        }
        switchFpsChart.setOnCheckedChangeListener { _, isChecked -> 
            viewModel.updateChartSettings(showFps = isChecked) 
        }
        
        spinnerChartHeight.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                viewModel.updateChartSettings(height = ChartHeight.entries[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        
        // Peak Notifications
        switchPeakNotifications.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updatePeakNotificationSettings(enabled = isChecked)
        }
        spinnerPeakInterval.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                viewModel.updatePeakNotificationSettings(interval = PeakNotificationInterval.entries[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        switchCpuPeak.setOnCheckedChangeListener { _, isChecked -> 
            viewModel.updatePeakNotificationSettings(showCpu = isChecked) 
        }
        switchRamPeak.setOnCheckedChangeListener { _, isChecked -> 
            viewModel.updatePeakNotificationSettings(showRam = isChecked) 
        }
        switchTempPeak.setOnCheckedChangeListener { _, isChecked -> 
            viewModel.updatePeakNotificationSettings(showTemp = isChecked) 
        }
        switchNetPeak.setOnCheckedChangeListener { _, isChecked -> 
            viewModel.updatePeakNotificationSettings(showNet = isChecked) 
        }
        
        sliderToastDuration.addOnChangeListener { _, value, _ ->
            textToastDuration.text = "${value.toInt()}s"
            viewModel.updatePeakNotificationSettings(toastDuration = (value * 1000).toInt())
        }
        
        // FPS Monitoring
        switchFpsMonitoring.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateFpsSettings(enabled = isChecked)
        }
        switchJankDetection.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateFpsSettings(jankDetection = isChecked)
        }
        sliderFpsThreshold.addOnChangeListener { _, value, _ ->
            textFpsThreshold.text = "${value.toInt()} fps"
            viewModel.updateFpsSettings(threshold = value.toInt())
        }
        
        // Analytics
        switch30sAvg.setOnCheckedChangeListener { _, isChecked -> 
            viewModel.updateAnalyticsSettings(show30s = isChecked) 
        }
        switch1mAvg.setOnCheckedChangeListener { _, isChecked -> 
            viewModel.updateAnalyticsSettings(show1m = isChecked) 
        }
        switch5mAvg.setOnCheckedChangeListener { _, isChecked -> 
            viewModel.updateAnalyticsSettings(show5m = isChecked) 
        }
        switchPercentiles.setOnCheckedChangeListener { _, isChecked -> 
            viewModel.updateAnalyticsSettings(showPercentiles = isChecked) 
        }
        
        // Export
        spinnerExportFormat.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val format = when (position) {
                    0 -> ExportFormat.CSV
                    1 -> ExportFormat.TXT
                    else -> ExportFormat.JSON
                }
                viewModel.updateExportSettings(format = format)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        spinnerExportRange.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                viewModel.updateExportSettings(range = TimeRange.entries[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        
        // Reset Defaults
        btnResetDefaults.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Reset Settings")
                .setMessage("Reset all settings to default values?")
                .setPositiveButton("Reset") { _, _ -> viewModel.resetToDefaults() }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
    
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collectLatest { state ->
                        updateUI(state)
                    }
                }
                launch {
                    viewModel.events.collectLatest { event ->
                        handleEvent(event)
                    }
                }
            }
        }
    }
    
    private fun updateUI(state: SettingsUiState) {
        progressBar.visibility = if (state.isLoading || state.isSaving) View.VISIBLE else View.GONE
        
        val settings = state.settings
        
        // Update Interval
        val intervalIndex = UpdateInterval.entries.indexOfFirst { it.intervalMs == settings.updateIntervalMs }
        if (intervalIndex >= 0) spinnerUpdateInterval.setSelection(intervalIndex, false)
        
        // Charts
        switchCpuChart.isChecked = settings.showCpuChart
        switchRamChart.isChecked = settings.showRamChart
        switchTempChart.isChecked = settings.showTempChart
        switchNetworkChart.isChecked = settings.showNetworkChart
        switchFpsChart.isChecked = settings.showFpsChart
        spinnerChartHeight.setSelection(settings.chartHeight.ordinal, false)
        
        // Peak Notifications
        switchPeakNotifications.isChecked = settings.peakNotificationsEnabled
        val peakIntervalIndex = PeakNotificationInterval.entries.indexOfFirst { 
            it.intervalMs == settings.peakNotificationIntervalMs 
        }
        if (peakIntervalIndex >= 0) spinnerPeakInterval.setSelection(peakIntervalIndex, false)
        switchCpuPeak.isChecked = settings.showCpuPeak
        switchRamPeak.isChecked = settings.showRamPeak
        switchTempPeak.isChecked = settings.showTempPeak
        switchNetPeak.isChecked = settings.showNetPeak
        sliderToastDuration.value = (settings.toastDurationMs / 1000f)
        textToastDuration.text = "${settings.toastDurationMs / 1000}s"
        
        // FPS
        switchFpsMonitoring.isChecked = settings.fpsMonitoringEnabled
        switchJankDetection.isChecked = settings.jankDetectionEnabled
        sliderFpsThreshold.value = settings.fpsThreshold.toFloat()
        textFpsThreshold.text = "${settings.fpsThreshold} fps"
        
        // Analytics
        switch30sAvg.isChecked = settings.show30sAverage
        switch1mAvg.isChecked = settings.show1mAverage
        switch5mAvg.isChecked = settings.show5mAverage
        switchPercentiles.isChecked = settings.showPercentiles
        
        // Export
        val formatIndex = when (settings.defaultExportFormat) {
            is ExportFormat.CSV -> 0
            is ExportFormat.TXT -> 1
            is ExportFormat.JSON -> 2
        }
        spinnerExportFormat.setSelection(formatIndex, false)
        spinnerExportRange.setSelection(settings.defaultExportRange.ordinal, false)
    }
    
    private fun handleEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.SettingsSaved -> {
                Toast.makeText(context, "Settings saved", Toast.LENGTH_SHORT).show()
            }
            is SettingsEvent.SettingsReset -> {
                Toast.makeText(context, "Settings reset to defaults", Toast.LENGTH_SHORT).show()
            }
            is SettingsEvent.Error -> {
                Toast.makeText(context, "Error: ${event.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    companion object {
        fun newInstance() = SettingsFragment()
    }
}
