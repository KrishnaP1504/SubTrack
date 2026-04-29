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
import androidx.navigation.fragment.navArgs
import com.example.subtrack.R
import com.example.subtrack.SubTrackApplication
import com.example.subtrack.databinding.FragmentSubscriptionDetailBinding
import com.example.subtrack.ui.viewmodel.SubscriptionViewModel
import com.example.subtrack.util.CurrencyUtils
import kotlinx.coroutines.launch

class SubscriptionDetailFragment : Fragment() {

    private var _binding: FragmentSubscriptionDetailBinding? = null
    private val binding get() = _binding!!

    private val args: SubscriptionDetailFragmentArgs by navArgs()

    private val viewModel: SubscriptionViewModel by viewModels {
        SubscriptionViewModel.Factory(
            requireActivity().application,
            (requireActivity().application as SubTrackApplication).repository
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSubscriptionDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        // Fix: Track the current subscription in a local var so button listeners
        // are registered only once (not re-registered on every Firestore emit).
        var currentSubscription: com.example.subtrack.data.local.entity.Subscription? = null

        binding.btnEdit.setOnClickListener {
            currentSubscription?.let { sub ->
                val action = SubscriptionDetailFragmentDirections.actionSubscriptionDetailToAddEdit(sub.id)
                findNavController().navigate(action)
            }
        }

        binding.btnDelete.setOnClickListener {
            currentSubscription?.let { sub ->
                AlertDialog.Builder(requireContext())
                    .setTitle(getString(R.string.confirm_delete_title))
                    .setMessage(getString(R.string.confirm_delete_message))
                    .setPositiveButton(getString(R.string.delete)) { _, _ ->
                        viewModel.deleteSubscription(sub)
                        findNavController().navigateUp()
                    }
                    .setNegativeButton(getString(R.string.cancel), null)
                    .show()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.subscriptions.collect { subscriptions ->
                val subscription = subscriptions.find { it.id == args.subscriptionId }
                if (subscription != null) {
                    currentSubscription = subscription
                    binding.tvName.text = subscription.name

                    // Load the real company logo
                    try {
                        binding.cvIcon.setCardBackgroundColor(android.graphics.Color.parseColor(subscription.color))
                    } catch (e: Exception) {
                        // fallback
                    }
                    com.example.subtrack.util.LogoUtils.loadLogo(
                        imageView = binding.ivLogo,
                        subscriptionName = subscription.name,
                        fallbackLetterView = binding.tvIconLetter,
                        fallbackColor = subscription.color
                    )

                    binding.tvDescription.text = "${subscription.name} is a subscription service. Track your billing cycle and payments here to manage your expenses effectively."
                    binding.tvCycle.text = subscription.billingCycle.lowercase().replaceFirstChar { it.uppercase() }
                    binding.tvPrice.text = CurrencyUtils.format(subscription.price, subscription.currency)
                    binding.tvRenews.text = com.example.subtrack.util.DateUtils.formatDate(subscription.renewalDate)
                    binding.tvCategory.text = subscription.category
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
