package com.example.subtrack.ui.fragment

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.subtrack.R
import com.example.subtrack.SubTrackApplication
import com.example.subtrack.data.local.entity.Subscription
import com.example.subtrack.databinding.FragmentEmailSyncBinding
import com.example.subtrack.ui.adapter.DetectedSubscriptionAdapter
import com.example.subtrack.ui.viewmodel.SubscriptionViewModel
import com.example.subtrack.util.SmsSubscriptionDetector
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    private val requestSmsPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startRealSmsScan()
        } else {
            showPermissionNeededState()
        }
    }

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

        binding.btnGrantPermission.setOnClickListener {
            showSmsDisclosureDialog()
        }

        binding.btnImportSelected.setOnClickListener {
            importRealSubscriptions()
        }

        binding.btnAddManually.setOnClickListener {
            findNavController().navigate(R.id.action_emailSync_to_addEdit)
        }

        checkPermissionAndScan()
    }

    private fun setupRecyclerView() {
        adapter = DetectedSubscriptionAdapter(emptyList(), selectedSubscriptions)
        binding.rvDetectedSubscriptions.adapter = adapter
    }

    private fun checkPermissionAndScan() {
        val hasPermission = ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            startRealSmsScan()
        } else {
            showPermissionNeededState()
        }
    }

    private fun showSmsDisclosureDialog() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.sms_disclosure_title))
            .setMessage(getString(R.string.sms_disclosure_body))
            .setPositiveButton(getString(R.string.continue_action)) { _, _ ->
                requestSmsPermission.launch(Manifest.permission.READ_SMS)
            }
            .setNegativeButton(getString(R.string.not_now), null)
            .show()
    }

    private fun showPermissionNeededState() {
        binding.layoutLoading.visibility = View.GONE
        binding.layoutResults.visibility = View.GONE
        binding.layoutEmpty.visibility = View.GONE
        binding.layoutPermission.visibility = View.VISIBLE
    }

    private fun startRealSmsScan() {
        binding.layoutPermission.visibility = View.GONE
        binding.layoutResults.visibility = View.GONE
        binding.layoutEmpty.visibility = View.GONE
        binding.layoutLoading.visibility = View.VISIBLE

        viewLifecycleOwner.lifecycleScope.launch {
            // Scan real SMS on IO dispatcher
            val realSubs = withContext(Dispatchers.IO) {
                SmsSubscriptionDetector.scanDeviceSms(requireContext().applicationContext)
            }

            if (!isAdded) return@launch
            binding.layoutLoading.visibility = View.GONE

            if (realSubs.isNotEmpty()) {
                selectedSubscriptions.clear()
                selectedSubscriptions.addAll(realSubs)
                adapter.submitList(realSubs)
                binding.tvDetectedHeader.text = getString(R.string.real_subs_detected_count, realSubs.size)
                binding.layoutResults.visibility = View.VISIBLE
                binding.layoutEmpty.visibility = View.GONE
            } else {
                binding.layoutResults.visibility = View.GONE
                binding.layoutEmpty.visibility = View.VISIBLE
            }
        }
    }

    private fun importRealSubscriptions() {
        if (selectedSubscriptions.isEmpty()) {
            findNavController().navigateUp()
            return
        }

        val toAdd = selectedSubscriptions.toList()
        viewModel.addMultipleSubscriptions(toAdd)

        Snackbar.make(binding.root, "Added ${toAdd.size} real subscription(s)!", Snackbar.LENGTH_SHORT).show()
        findNavController().navigateUp()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
