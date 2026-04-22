// ui/fragment/AnalyticsFragment.kt

package com.example.subtrack.ui.fragment

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.example.subtrack.SubTrackApplication
import com.example.subtrack.databinding.FragmentAnalyticsBinding
import com.example.subtrack.ui.viewmodel.AnalyticsData
import com.example.subtrack.ui.viewmodel.AnalyticsViewModel
import com.example.subtrack.util.CurrencyUtils
import kotlinx.coroutines.launch

class AnalyticsFragment : Fragment() {

    private var _binding: FragmentAnalyticsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AnalyticsViewModel by viewModels {
        AnalyticsViewModel.Factory(
            requireActivity().application,
            (requireActivity().application as SubTrackApplication).repository
        )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAnalyticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupPieChartStyle()
        setupBarChartStyle()
        observeAnalytics()
    }

    private fun observeAnalytics() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.analyticsData.collect { data ->
                updateSummaryCards(data)
                updatePieChart(data)
                updateBarChart(data)
            }
        }
    }

    private fun updateSummaryCards(data: AnalyticsData) {
        val preferredCurrency = CurrencyUtils.getPreferredCurrency(requireContext())
        binding.tvAnnualBurnRate.text = CurrencyUtils.format(data.annualBurnRate, preferredCurrency) + " / year"
        binding.tvDailyAverage.text = CurrencyUtils.format(data.dailyAverage, preferredCurrency) + " / day on average"
    }

    private fun setupPieChartStyle() {
        binding.pieChart.apply {
            description.isEnabled = false
            setUsePercentValues(true)
            setDrawEntryLabels(false)     // labels in legend only, not on slices
            isDrawHoleEnabled = true
            holeRadius = 55f
            transparentCircleRadius = 60f
            setHoleColor(Color.TRANSPARENT)
            legend.isEnabled = true
            legend.textColor = com.google.android.material.color.MaterialColors.getColor(requireView(), com.google.android.material.R.attr.colorOnSurface, Color.BLACK)
            setNoDataText("No subscriptions yet")
        }
    }

    private fun updatePieChart(data: AnalyticsData) {
        if (data.categoryBreakdown.isEmpty()) return

        val entries = data.categoryBreakdown.entries.mapIndexed { index, (category, total) ->
            PieEntry(total.toFloat(), category)
        }

        // Professional color palette for slices
        val colors = listOf(
            Color.parseColor("#6750A4"),  // primary purple
            Color.parseColor("#009688"),  // teal
            Color.parseColor("#F44336"),  // red
            Color.parseColor("#2196F3"),  // blue
            Color.parseColor("#FF9800"),  // orange
            Color.parseColor("#4CAF50"),  // green
            Color.parseColor("#E91E63"),  // pink
            Color.parseColor("#795548"),  // brown
        )

        val dataSet = PieDataSet(entries, "").apply {
            this.colors = colors.take(entries.size)
            sliceSpace = 3f
            valueTextSize = 12f
            valueTextColor = Color.WHITE
        }

        binding.pieChart.data = PieData(dataSet)
        binding.pieChart.animateY(900, Easing.EaseInOutCubic)
        binding.pieChart.invalidate()
    }

    private fun setupBarChartStyle() {
        binding.barChart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setDrawGridBackground(false)
            setPinchZoom(false)
            isDoubleTapToZoomEnabled = false

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                textSize = 11f
                textColor = com.google.android.material.color.MaterialColors.getColor(requireView(), com.google.android.material.R.attr.colorOnSurface, Color.BLACK)
            }

            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = Color.parseColor("#33888888")
                textSize = 11f
                textColor = com.google.android.material.color.MaterialColors.getColor(requireView(), com.google.android.material.R.attr.colorOnSurface, Color.BLACK)
            }

            axisRight.isEnabled = false
        }
    }

    private fun updateBarChart(data: AnalyticsData) {
        if (data.categoryBreakdown.isEmpty()) return

        val categories = data.categoryBreakdown.keys.toList()
        val entries = categories.mapIndexed { index, category ->
            val monthlyAmount = data.categoryBreakdown[category] ?: 0.0
            BarEntry(index.toFloat(), monthlyAmount.toFloat())
        }

        val dataSet = BarDataSet(entries, "Monthly cost by category").apply {
            colors = listOf(
                Color.parseColor("#6750A4"),
                Color.parseColor("#009688"),
                Color.parseColor("#F44336"),
                Color.parseColor("#2196F3"),
                Color.parseColor("#FF9800"),
                Color.parseColor("#4CAF50"),
                Color.parseColor("#E91E63"),
                Color.parseColor("#795548")
            ).take(entries.size)
            valueTextSize = 10f
            valueTextColor = com.google.android.material.color.MaterialColors.getColor(requireView(), com.google.android.material.R.attr.colorOnSurface, Color.BLACK)
        }

        // Map each X index to its category name
        binding.barChart.xAxis.valueFormatter = IndexAxisValueFormatter(categories)
        binding.barChart.xAxis.labelCount = categories.size

        binding.barChart.data = BarData(dataSet).apply { barWidth = 0.6f }
        binding.barChart.animateY(900, Easing.EaseInOutCubic)
        binding.barChart.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}