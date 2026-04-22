package com.example.subtrack.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.subtrack.SubTrackApplication
import com.example.subtrack.data.local.entity.Subscription
import com.example.subtrack.databinding.FragmentEmailSyncBinding
import com.example.subtrack.ui.adapter.DetectedSubscriptionAdapter
import com.example.subtrack.ui.viewmodel.SubscriptionViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class EmailSyncFragment : Fragment() {

    private var _binding: FragmentEmailSyncBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SubscriptionViewModel by viewModels {
        SubscriptionViewModel.Factory(
            requireActivity().application,
            (requireActivity().application as SubTrackApplication).repository
        )
    }

    private lateinit var adapter: DetectedSubscriptionAdapter
    private val selectedSubscriptions = mutableSetOf<Subscription>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEmailSyncBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        setupRecyclerView()

        // Simulate fetching emails and parsing subscriptions
        simulateEmailScan()

        binding.btnImportSelected.setOnClickListener {
            importSelectedSubscriptions()
        }
    }

    private fun setupRecyclerView() {
        adapter = DetectedSubscriptionAdapter(emptyList(), selectedSubscriptions)
        binding.rvDetectedSubscriptions.adapter = adapter
    }

    private fun simulateEmailScan() {
        viewLifecycleOwner.lifecycleScope.launch {
            // Show loading state
            binding.layoutLoading.visibility = View.VISIBLE
            binding.layoutResults.visibility = View.GONE

            // Simulate network delay for "scanning emails"
            delay(2500)

            // Mock Data representing found subscriptions
            val mockData = listOf(
                Subscription(id = "mock1", name = "Netflix", price = 15.49, currency = "USD", billingCycle = "MONTHLY", renewalDate = System.currentTimeMillis() + 86400000L * 14, category = "Entertainment", color = "#E50914"),
                Subscription(id = "mock2", name = "Spotify", price = 9.99, currency = "USD", billingCycle = "MONTHLY", renewalDate = System.currentTimeMillis() + 86400000L * 5, category = "Music", color = "#1DB954"),
                Subscription(id = "mock3", name = "Amazon Prime", price = 139.00, currency = "USD", billingCycle = "YEARLY", renewalDate = System.currentTimeMillis() + 86400000L * 100, category = "Shopping", color = "#FF9900")
            )

            // By default, select all mock data
            selectedSubscriptions.clear()
            selectedSubscriptions.addAll(mockData)

            adapter.submitList(mockData)

            // Switch to results state
            binding.layoutLoading.visibility = View.GONE
            binding.layoutResults.visibility = View.VISIBLE
        }
    }

    private fun importSelectedSubscriptions() {
        if (selectedSubscriptions.isEmpty()) {
            findNavController().navigateUp()
            return
        }

        // Add each selected subscription to the viewmodel/repository
        for (sub in selectedSubscriptions) {
            // We need to generate a real ID because mock ones are hardcoded and might collide
            val newSub = sub.copy(id = java.util.UUID.randomUUID().toString())
            viewModel.addSubscription(newSub)
        }

        // Navigate back to the dashboard once done
        findNavController().navigateUp()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
