package com.sysmetrics.app.ui.network

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.snackbar.Snackbar
import com.sysmetrics.app.R
import com.sysmetrics.app.core.di.DefaultDispatcherProvider
import com.sysmetrics.app.core.di.NetworkModule
import com.sysmetrics.app.data.model.network.NetworkDisplayMode
import com.sysmetrics.app.data.model.network.NetworkTrafficStats
import com.sysmetrics.app.data.model.network.NetworkTypeInfo
import com.sysmetrics.app.data.model.network.PerAppTrafficStats
import com.sysmetrics.app.databinding.FragmentNetworkStatsBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Fragment for displaying detailed network traffic statistics.
 * Shows real-time traffic, network type, and per-app breakdown.
 *
 * ## Features:
 * - Real-time ingress/egress speeds
 * - Peak usage tracking
 * - Network type and signal strength
 * - Per-app traffic breakdown (RecyclerView)
 */
class NetworkStatsFragment : Fragment() {

    companion object {
        private const val TAG = "NET_STATS_FRAG"

        fun newInstance(): NetworkStatsFragment = NetworkStatsFragment()
    }

    private var _binding: FragmentNetworkStatsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NetworkStatsViewModel by viewModels {
        NetworkModule.getInstance(
            requireContext().applicationContext,
            DefaultDispatcherProvider()
        ).provideViewModelFactory()
    }

    private val perAppAdapter = PerAppTrafficAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNetworkStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews()
        observeState()
    }

    private fun setupViews() {
        // RecyclerView setup
        binding.recyclerPerApp.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = perAppAdapter
            setHasFixedSize(true)
        }

        // Display mode chips
        binding.chipCompact.setOnClickListener {
            viewModel.setDisplayMode(NetworkDisplayMode.COMPACT)
        }
        binding.chipExtended.setOnClickListener {
            viewModel.setDisplayMode(NetworkDisplayMode.EXTENDED)
        }
        binding.chipPerApp.setOnClickListener {
            viewModel.setDisplayMode(NetworkDisplayMode.PER_APP)
        }

        // Reset baseline button
        binding.btnResetBaseline.setOnClickListener {
            viewModel.resetBaseline()
            Snackbar.make(binding.root, "Baseline reset", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Start monitoring when fragment is visible
                viewModel.startMonitoring()

                launch {
                    viewModel.uiState.collectLatest { state ->
                        updateUi(state)
                    }
                }

                launch {
                    viewModel.alerts.collectLatest { alert ->
                        Snackbar.make(
                            binding.root,
                            alert.message,
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    private fun updateUi(state: NetworkStatsViewModel.NetworkUiState) {
        // Traffic stats
        updateTrafficStats(state.trafficStats)

        // Network type
        updateNetworkType(state.networkType)

        // Per-app stats
        perAppAdapter.submitList(state.perAppStats)

        // Display mode chips
        updateDisplayModeChips(state.displayMode)

        // Availability indicators
        binding.chipNative.isChecked = state.isNativeAvailable
        binding.chipNative.text = if (state.isNativeAvailable) "Native: ON" else "Native: OFF"

        // Error handling
        state.error?.let { error ->
            Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    private fun updateTrafficStats(stats: NetworkTrafficStats) {
        // Current speeds
        binding.tvIngressSpeed.text = stats.formatIngressSpeed()
        binding.tvEgressSpeed.text = stats.formatEgressSpeed()

        // Peak values
        binding.tvPeakIngress.text = "Peak: %.1f Mbps".format(stats.peakIngressMbps)
        binding.tvPeakEgress.text = "Peak: %.1f Mbps".format(stats.peakEgressMbps)

        // Session totals
        binding.tvSessionIngress.text = formatBytes(stats.sessionIngressBytes)
        binding.tvSessionEgress.text = formatBytes(stats.sessionEgressBytes)

        // Progress indicators (scaled to reasonable max)
        val maxMbps = 100f // Scale for visual
        binding.progressIngress.progress = ((stats.ingressMbps / maxMbps) * 100).toInt().coerceIn(0, 100)
        binding.progressEgress.progress = ((stats.egressMbps / maxMbps) * 100).toInt().coerceIn(0, 100)
    }

    private fun updateNetworkType(networkType: NetworkTypeInfo) {
        binding.tvNetworkType.text = networkType.type.displayName
        binding.tvNetworkName.text = networkType.networkName ?: "—"
        binding.tvSignalStrength.text = networkType.signalStrengthDbm?.let { "$it dBm" } ?: "—"
        binding.progressSignal.progress = networkType.signalQualityPercent

        // Connection status indicator
        binding.cardNetworkType.strokeWidth = if (networkType.type != com.sysmetrics.app.data.model.network.NetworkTypeEnum.NONE) 2 else 0
    }

    private fun updateDisplayModeChips(mode: NetworkDisplayMode) {
        binding.chipCompact.isChecked = mode == NetworkDisplayMode.COMPACT
        binding.chipExtended.isChecked = mode == NetworkDisplayMode.EXTENDED
        binding.chipPerApp.isChecked = mode == NetworkDisplayMode.PER_APP
    }

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024f)
            bytes < 1024 * 1024 * 1024 -> "%.1f MB".format(bytes / (1024f * 1024f))
            else -> "%.2f GB".format(bytes / (1024f * 1024f * 1024f))
        }
    }

    override fun onStop() {
        super.onStop()
        viewModel.stopMonitoring()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

/**
 * RecyclerView adapter for per-app traffic stats.
 */
class PerAppTrafficAdapter : ListAdapter<PerAppTrafficStats, PerAppTrafficAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_per_app_traffic, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvAppName: TextView = itemView.findViewById(R.id.tvAppName)
        private val tvTraffic: TextView = itemView.findViewById(R.id.tvTraffic)
        private val progressTraffic: LinearProgressIndicator = itemView.findViewById(R.id.progressTraffic)

        fun bind(stats: PerAppTrafficStats) {
            tvAppName.text = stats.appName
            tvTraffic.text = stats.formatCompact()

            // Calculate progress relative to max in list
            val maxSpeed = 1024 * 1024L // 1 MB/s reference
            val progress = ((stats.totalSpeedBytesPerSec.toFloat() / maxSpeed) * 100).toInt().coerceIn(0, 100)
            progressTraffic.progress = progress

            // Set app icon if available
            stats.appIcon?.let {
                // ivAppIcon.setImageDrawable(it)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<PerAppTrafficStats>() {
        override fun areItemsTheSame(oldItem: PerAppTrafficStats, newItem: PerAppTrafficStats): Boolean {
            return oldItem.uid == newItem.uid
        }

        override fun areContentsTheSame(oldItem: PerAppTrafficStats, newItem: PerAppTrafficStats): Boolean {
            return oldItem == newItem
        }
    }
}
