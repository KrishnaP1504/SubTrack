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
import com.example.subtrack.ui.adapter.TrendingSubscriptionAdapter
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
    private lateinit var trendingAdapter: TrendingSubscriptionAdapter

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
        setupFab()
        observeSubscriptions()
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

        val swipeToDelete = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val subscription = adapter.currentList[position]
                showDeleteDialog(subscription)
                adapter.notifyItemChanged(position)
            }
        }
        ItemTouchHelper(swipeToDelete).attachToRecyclerView(binding.rvSubscriptions)

        // Setup Trending Subscriptions
        trendingAdapter = TrendingSubscriptionAdapter(
            trendingList = getMockTrendingSubscriptions(),
            onItemClick = { sub ->
                // When clicked, navigate to AddEdit pre-filled? For now, we just pass the ID if it exists or do nothing.
                // The user requested to list them, so we will show a snackbar or just do nothing.
            }
        )
        binding.rvTrendingSubscriptions.adapter = trendingAdapter
    }

    private fun getMockTrendingSubscriptions(): List<Subscription> {
        return listOf(
            Subscription(id = "1", name = "Netflix", price = 15.49, currency = "USD", category = "Entertainment", color = "#E50914"),
            Subscription(id = "2", name = "Spotify", price = 10.99, currency = "USD", category = "Music", color = "#1DB954"),
            Subscription(id = "3", name = "Prime Video", price = 14.99, currency = "USD", category = "Entertainment", color = "#00A8E1"),
            Subscription(id = "4", name = "Disney+", price = 7.99, currency = "USD", category = "Entertainment", color = "#113CCF"),
            Subscription(id = "5", name = "Apple Music", price = 10.99, currency = "USD", category = "Music", color = "#FA243C")
        )
    }

    private fun setupFab() {
        binding.fabAddSubscription.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_addEdit)
        }
        binding.nestedScrollView.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            if (scrollY > oldScrollY) binding.fabAddSubscription.shrink()
            else binding.fabAddSubscription.extend()
        }
    }

    private fun observeSubscriptions() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.subscriptions.collect { subscriptions ->
                adapter.submitList(subscriptions)
                binding.layoutEmptyState.visibility =
                    if (subscriptions.isEmpty()) View.VISIBLE else View.GONE
                val preferredCurrency = CurrencyUtils.getPreferredCurrency(requireContext())
                val monthlyTotal = subscriptions.sumOf { 
                    val monthlyPrice = CurrencyUtils.toMonthlyPrice(it.price, it.billingCycle)
                    CurrencyUtils.convert(monthlyPrice, it.currency, preferredCurrency)
                }
                binding.tvMonthlyTotal.text = CurrencyUtils.format(monthlyTotal, preferredCurrency)
                binding.tvActiveCount.text = "${subscriptions.size} active"
                val upcomingCount = subscriptions.count { DateUtils.isWithinDays(it.renewalDate, 7) }
                binding.tvUpcomingCount.text = upcomingCount.toString()
            }
        }
    }

    private fun showDeleteDialog(subscription: Subscription) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.confirm_delete_title))
            .setMessage(getString(R.string.confirm_delete_message))
            .setPositiveButton(getString(R.string.delete)) { _, _ -> viewModel.deleteSubscription(subscription) }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}