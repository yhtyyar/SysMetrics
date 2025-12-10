package com.sysmetrics.app.ui.home

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.sysmetrics.app.R
import com.sysmetrics.app.databinding.FragmentHomeTvBinding
import com.sysmetrics.app.ui.components.MetricCardView
import com.sysmetrics.app.ui.navigation.DpadNavigationHandler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Home Fragment optimized for Android TV
 * Features:
 * - D-pad navigation with visual focus
 * - Real-time metric updates
 * - Minimalist design for 2m viewing distance
 */
@AndroidEntryPoint
class HomeTvFragment : Fragment(), DpadNavigationHandler.NavigationListener {

    private var _binding: FragmentHomeTvBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: HomeTvViewModel by viewModels()
    private lateinit var dpadHandler: DpadNavigationHandler
    
    private val focusableCards = mutableListOf<MetricCardView>()
    private var currentFocusIndex = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeTvBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupDpadNavigation()
        setupMetricCards()
        observeMetrics()
        
        // Request initial focus
        binding.metricCpu.post {
            binding.metricCpu.requestFocus()
        }
    }

    private fun setupDpadNavigation() {
        dpadHandler = DpadNavigationHandler(requireActivity())
        dpadHandler.setNavigationListener(this)
        
        // Setup key listener on root view
        binding.root.isFocusableInTouchMode = true
        binding.root.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                dpadHandler.handleKeyEvent(event)
            } else {
                false
            }
        }
        
        // Setup focusable cards list
        focusableCards.apply {
            add(binding.metricCpu)
            add(binding.metricRam)
            add(binding.metricTemp)
        }
    }

    private fun setupMetricCards() {
        // Setup CPU card
        binding.metricCpu.apply {
            setMetric(
                name = getString(R.string.cpu_usage),
                value = "0.0%",
                progress = 0,
                iconRes = R.drawable.ic_cpu
            )
            setIconTint(resources.getColor(R.color.icon_cpu, null))
        }
        
        // Setup RAM card
        binding.metricRam.apply {
            setMetric(
                name = getString(R.string.ram_usage),
                value = "0 / 0 MB",
                progress = 0,
                iconRes = R.drawable.ic_memory
            )
            setIconTint(resources.getColor(R.color.icon_ram, null))
        }
        
        // Setup Temperature card
        binding.metricTemp.apply {
            setMetric(
                name = getString(R.string.temperature),
                value = getString(R.string.not_available),
                progress = 0,
                iconRes = R.drawable.ic_temperature
            )
            setIconTint(resources.getColor(R.color.icon_temp, null))
        }
        
        // Setup click listeners
        binding.metricCpu.setOnClickListener { 
            // Navigate to CPU details
        }
        binding.metricRam.setOnClickListener { 
            // Navigate to RAM details
        }
        binding.metricTemp.setOnClickListener { 
            // Navigate to Temperature details
        }
    }

    private fun observeMetrics() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe CPU metrics
                launch {
                    viewModel.cpuUsage.collect { cpuUsage ->
                        binding.metricCpu.updateValue(
                            value = String.format("%.1f%%", cpuUsage),
                            progress = cpuUsage.toInt()
                        )
                    }
                }
                
                // Observe RAM metrics
                launch {
                    viewModel.memoryInfo.collect { memoryInfo ->
                        val usedMb = memoryInfo.usedMemory / 1024 / 1024
                        val totalMb = memoryInfo.totalMemory / 1024 / 1024
                        val percent = if (totalMb > 0) {
                            ((usedMb.toFloat() / totalMb.toFloat()) * 100).toInt()
                        } else 0
                        
                        binding.metricRam.updateValue(
                            value = "$usedMb / $totalMb MB",
                            progress = percent
                        )
                    }
                }
                
                // Observe Temperature
                launch {
                    viewModel.temperature.collect { temp ->
                        val value = if (temp > 0) {
                            String.format("%.1f°C", temp)
                        } else {
                            getString(R.string.not_available)
                        }
                        val progress = if (temp > 0) {
                            ((temp / 100f) * 100).toInt().coerceIn(0, 100)
                        } else 0
                        
                        binding.metricTemp.updateValue(value, progress)
                    }
                }
                
                // Observe system info
                launch {
                    viewModel.systemInfo.collect { info ->
                        binding.coresInfo.text = getString(R.string.cores_format, info.cpuCores)
                        binding.uptimeInfo.text = getString(
                            R.string.uptime_format,
                            formatUptime(info.uptimeMillis)
                        )
                    }
                }
            }
        }
    }

    private fun formatUptime(uptimeMillis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(uptimeMillis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(uptimeMillis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(uptimeMillis) % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    // D-pad Navigation Callbacks
    override fun onDpadUp(): Boolean {
        currentFocusIndex = (currentFocusIndex - 1 + focusableCards.size) % focusableCards.size
        focusableCards[currentFocusIndex].requestFocus()
        return true
    }

    override fun onDpadDown(): Boolean {
        currentFocusIndex = (currentFocusIndex + 1) % focusableCards.size
        focusableCards[currentFocusIndex].requestFocus()
        return true
    }

    override fun onDpadCenter(): Boolean {
        // Trigger click on focused card
        focusableCards[currentFocusIndex].performClick()
        return true
    }

    override fun onBackPressed(): Boolean {
        requireActivity().finish()
        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
