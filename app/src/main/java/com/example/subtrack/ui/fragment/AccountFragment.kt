package com.example.subtrack.ui.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.subtrack.R
import com.example.subtrack.databinding.FragmentAccountBinding
import com.example.subtrack.util.CurrencyUtils
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth

class AccountFragment : Fragment() {

    private var _binding: FragmentAccountBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadProfile()
        setupCurrency()
        setupBiometric()
        setupSaveName()
        setupLogout()
    }

    private fun loadProfile() {
        val prefs = com.example.subtrack.util.SecurePreferences.getPrefs(requireContext())
        val user = FirebaseAuth.getInstance().currentUser
        val phone = user?.phoneNumber ?: prefs.getString("logged_in_phone", "") ?: ""
        val name = prefs.getString("user_display_name", "") ?: ""

        binding.tvUserPhone.text = phone.ifEmpty { getString(R.string.account_default_phone) }
        binding.tvUserName.text = name.ifEmpty { getString(R.string.account_default_name) }
        binding.etDisplayName.setText(name)

        // Avatar initial
        val initial = if (name.isNotEmpty()) {
            name.first().uppercase()
        } else if (phone.isNotEmpty()) {
            "+"
        } else {
            "U"
        }
        binding.tvAvatarInitial.text = initial
    }

    private fun setupSaveName() {
        binding.btnSaveName.setOnClickListener {
            val newName = binding.etDisplayName.text.toString().trim()
            if (newName.isEmpty()) {
                binding.tilDisplayName.error = getString(R.string.error_name_required)
                return@setOnClickListener
            }
            binding.tilDisplayName.error = null

            val prefs = com.example.subtrack.util.SecurePreferences.getPrefs(requireContext())
            prefs.edit().putString("user_display_name", newName).apply()

            // Update UI
            binding.tvUserName.text = newName
            binding.tvAvatarInitial.text = newName.first().uppercase()

            Snackbar.make(binding.root, getString(R.string.name_saved), Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun setupBiometric() {
        val prefs = com.example.subtrack.util.SecurePreferences.getPrefs(requireContext())

        val biometricManager = BiometricManager.from(requireContext())
        val canAuthenticate = biometricManager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)

        if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
            binding.switchBiometric.isEnabled = false
            binding.tvBiometricStatus.text = when (canAuthenticate) {
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> getString(R.string.biometric_not_available)
                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> getString(R.string.biometric_hw_unavailable)
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> getString(R.string.biometric_not_enrolled)
                else -> getString(R.string.biometric_not_available)
            }
            return
        }

        // Set initial state from prefs
        val isEnabled = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
        binding.switchBiometric.isChecked = isEnabled

        binding.switchBiometric.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, isChecked).apply()
            val message = if (isChecked) getString(R.string.biometric_enabled_msg) else getString(R.string.biometric_disabled_msg)
            Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun setupCurrency() {
        val prefs = com.example.subtrack.util.SecurePreferences.getPrefs(requireContext())

        val currencyAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            CurrencyUtils.supportedCurrencies
        )
        binding.actvDefaultCurrency.setAdapter(currencyAdapter)

        val currentCurrency = CurrencyUtils.getDefaultCurrency(requireContext())
        binding.actvDefaultCurrency.setText(currentCurrency, false)

        binding.actvDefaultCurrency.setOnItemClickListener { _, _, position, _ ->
            val selected = CurrencyUtils.supportedCurrencies[position]
            prefs.edit().putString(KEY_DEFAULT_CURRENCY, selected).apply()
            Snackbar.make(
                binding.root,
                getString(R.string.currency_changed, selected),
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    private fun setupLogout() {
        binding.btnLogout.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.logout_confirm_title))
                .setMessage(getString(R.string.logout_confirm_message))
                .setPositiveButton(getString(R.string.logout)) { _, _ ->
                    performLogout()
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
        }
    }

    private fun performLogout() {
        FirebaseAuth.getInstance().signOut()
        val prefs = com.example.subtrack.util.SecurePreferences.getPrefs(requireContext())
        prefs.edit()
            .remove("logged_in_phone")
            .remove("logged_in_email")
            .putBoolean(WelcomeFragment.KEY_IS_LOGGED_IN, false)
            .apply()

        // Navigate back to login
        findNavController().navigate(R.id.action_account_to_login)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        const val KEY_DEFAULT_CURRENCY = "default_currency"
    }
}
