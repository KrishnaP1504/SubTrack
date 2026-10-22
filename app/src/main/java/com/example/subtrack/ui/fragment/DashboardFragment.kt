package com.example.subtrack.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.subtrack.R
import com.example.subtrack.SubTrackApplication
import com.example.subtrack.data.local.entity.Subscription
import com.example.subtrack.databinding.FragmentDashboardBinding
import com.example.subtrack.ui.adapter.SubscriptionAdapter
import com.example.subtrack.ui.viewmodel.SubscriptionViewModel
import com.example.subtrack.util.CurrencyUtils
import com.example.subtrack.util.DateUtils
import kotlinx.coroutines.launch

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SubscriptionViewModel by viewModels {
        SubscriptionViewModel.Factory(
            requireActivity().application,
            (requireActivity().application as SubTrackApplication).repository
        )
    }

    private lateinit var adapter: SubscriptionAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.inflateMenu(R.menu.menu_dashboard)
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_email_sync -> {
                    findNavController().navigate(R.id.action_dashboard_to_emailSync)
                    true
                }
                else -> false
            }
        }

        setupRecyclerView()
        setupButtons()
        observeSubscriptions()
    }

    private fun setupButtons() {
        // FAB Add Subscription
        binding.fabAddSubscription.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_addEdit)
        }

        // Auto-detect banner
        binding.cardAutoDetectBanner.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_emailSync)
        }

        // Auto-detect empty state button
        binding.btnEmptyAutoDetect.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_emailSync)
        }
    }

    private fun setupRecyclerView() {
        adapter = SubscriptionAdapter(
            onItemClick = { sub ->
                val action = DashboardFragmentDirections
                    .actionDashboardToSubscriptionDetail(subscriptionId = sub.id)
                findNavController().navigate(action)
            },
            onDeleteClick = { sub -> showDeleteDialog(sub) }
        )
        binding.rvSubscriptions.adapter = adapter
        binding.rvSubscriptions.layoutManager = LinearLayoutManager(requireContext())

        // Swipe-to-delete
        val swipeToDelete = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                if (position != RecyclerView.NO_POSITION && position in adapter.currentList.indices) {
                    val subscription = adapter.currentList[position]
                    showDeleteDialog(subscription)
                    adapter.notifyItemChanged(position)
                }
            }
        }
        ItemTouchHelper(swipeToDelete).attachToRecyclerView(binding.rvSubscriptions)
    }

    private fun observeSubscriptions() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.subscriptions.collect { subscriptions ->
                // Submit only real subscriptions
                adapter.submitList(subscriptions)

                // Update empty state
                if (subscriptions.isEmpty()) {
                    binding.rvSubscriptions.visibility = View.GONE
                    binding.layoutEmptyState.visibility = View.VISIBLE
                } else {
                    binding.rvSubscriptions.visibility = View.VISIBLE
                    binding.layoutEmptyState.visibility = View.GONE
                }

                // Calculate real monthly spend
                val userCurrency = CurrencyUtils.getDefaultCurrency(requireContext())
                val monthlyTotal = subscriptions.sumOf { sub ->
                    val monthlyPrice = if (sub.billingCycle.equals("YEARLY", ignoreCase = true)) {
                        sub.price / 12.0
                    } else {
                        sub.price
                    }
                    CurrencyUtils.convert(monthlyPrice, sub.currency, userCurrency)
                }
                binding.tvMonthlyTotal.text = CurrencyUtils.formatAmount(monthlyTotal, userCurrency)
                binding.tvActiveCount.text = "${subscriptions.size} active"

                // Calculate upcoming renewals (next 7 days)
                val upcomingCount = subscriptions.count { sub ->
                    val days = DateUtils.daysUntil(sub.renewalDate)
                    days in 0..7
                }
                binding.tvUpcomingCount.text = upcomingCount.toString()
            }
        }
    }

    private fun showDeleteDialog(subscription: Subscription) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.confirm_delete_title))
            .setMessage(getString(R.string.confirm_delete_message))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                viewModel.deleteSubscription(subscription)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}