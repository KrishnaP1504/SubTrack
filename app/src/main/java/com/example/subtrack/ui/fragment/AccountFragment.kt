package com.example.subtrack.ui.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.example.subtrack.R
import com.example.subtrack.databinding.FragmentAccountBinding
import com.example.subtrack.util.CurrencyUtils
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
        setupTheme()
        setupBiometric()
        setupResetPassword()
        setupSaveName()
        setupLogout()
    }

    private fun loadProfile() {
        val prefs = requireContext().getSharedPreferences(
            WelcomeFragment.PREFS_NAME, Context.MODE_PRIVATE
        )
        val user = FirebaseAuth.getInstance().currentUser
        val email = user?.email ?: prefs.getString("logged_in_email", "") ?: ""
        val name = prefs.getString("user_display_name", "") ?: ""

        binding.tvUserEmail.text = email.ifEmpty { getString(R.string.account_default_email) }
        binding.tvUserName.text = name.ifEmpty { getString(R.string.account_default_name) }
        binding.etDisplayName.setText(name)

        // Avatar initial
        val initial = if (name.isNotEmpty()) {
            name.first().uppercase()
        } else if (email.isNotEmpty()) {
            email.first().uppercase()
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

            val prefs = requireContext().getSharedPreferences(
                WelcomeFragment.PREFS_NAME, Context.MODE_PRIVATE
            )
            prefs.edit().putString("user_display_name", newName).apply()

            // Update UI
            binding.tvUserName.text = newName
            binding.tvAvatarInitial.text = newName.first().uppercase()

            Snackbar.make(binding.root, getString(R.string.name_saved), Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun setupBiometric() {
        val prefs = requireContext().getSharedPreferences(
            WelcomeFragment.PREFS_NAME, Context.MODE_PRIVATE
        )

        // Check if biometric hardware is available
        val biometricManager = BiometricManager.from(requireContext())
        val canAuthenticate = biometricManager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)

        when (canAuthenticate) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                binding.switchBiometric.isEnabled = true
                binding.tvBiometricStatus.text = getString(R.string.biometric_description)
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                binding.switchBiometric.isEnabled = false
                binding.tvBiometricStatus.text = getString(R.string.biometric_not_available)
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                binding.switchBiometric.isEnabled = false
                binding.tvBiometricStatus.text = getString(R.string.biometric_hw_unavailable)
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                binding.switchBiometric.isEnabled = false
                binding.tvBiometricStatus.text = getString(R.string.biometric_not_enrolled)
            }
            else -> {
                binding.switchBiometric.isEnabled = false
                binding.tvBiometricStatus.text = getString(R.string.biometric_not_available)
            }
        }

        // Load saved preference
        val biometricEnabled = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
        binding.switchBiometric.isChecked = biometricEnabled

        // Save toggle state
        binding.switchBiometric.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, isChecked).apply()

            val message = if (isChecked) {
                getString(R.string.biometric_enabled_msg)
            } else {
                getString(R.string.biometric_disabled_msg)
            }
            Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun setupResetPassword() {
        binding.cardResetPassword.setOnClickListener {
            showResetPasswordDialog()
        }
    }

    private fun showResetPasswordDialog() {
        val user = FirebaseAuth.getInstance().currentUser
        val email = user?.email ?: return

        // Build a dialog with current + new password fields
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(64, 32, 64, 0)
        }

        val currentPasswordInput = EditText(requireContext()).apply {
            hint = getString(R.string.current_password_hint)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        layout.addView(currentPasswordInput)

        val newPasswordInput = EditText(requireContext()).apply {
            hint = getString(R.string.new_password_hint)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        layout.addView(newPasswordInput)

        val confirmPasswordInput = EditText(requireContext()).apply {
            hint = getString(R.string.confirm_password_hint)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        layout.addView(confirmPasswordInput)

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.reset_password))
            .setView(layout)
            .setPositiveButton(getString(R.string.save_password)) { _, _ ->
                val currentPw = currentPasswordInput.text.toString()
                val newPw = newPasswordInput.text.toString()
                val confirmPw = confirmPasswordInput.text.toString()

                // Validate current password via Firebase re-authentication
                val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, currentPw)
                user.reauthenticate(credential).addOnCompleteListener { reauthTask ->
                    if (reauthTask.isSuccessful) {
                        user.updatePassword(newPw).addOnCompleteListener { updateTask ->
                            if (updateTask.isSuccessful) {
                                Snackbar.make(binding.root, getString(R.string.password_changed), Snackbar.LENGTH_SHORT).show()
                            } else {
                                Snackbar.make(binding.root, updateTask.exception?.message ?: "Failed to update password", Snackbar.LENGTH_LONG).show()
                            }
                        }
                    } else {
                        Snackbar.make(binding.root, getString(R.string.error_wrong_password), Snackbar.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
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
        val prefs = requireContext().getSharedPreferences(
            WelcomeFragment.PREFS_NAME, Context.MODE_PRIVATE
        )
        prefs.edit()
            .remove("logged_in_email")
            .apply()

        // Navigate back to login, clearing the entire back stack
        findNavController().navigate(R.id.action_account_to_login)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        const val KEY_DEFAULT_CURRENCY = "default_currency"
        const val KEY_APP_THEME = "app_theme"
    }

    private fun setupCurrency() {
        val prefs = requireContext().getSharedPreferences(
            WelcomeFragment.PREFS_NAME, Context.MODE_PRIVATE
        )

        val currencyAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            CurrencyUtils.supportedCurrencies
        )
        binding.actvDefaultCurrency.setAdapter(currencyAdapter)

        // Load saved currency
        val savedCurrency = prefs.getString(KEY_DEFAULT_CURRENCY, "INR") ?: "INR"
        binding.actvDefaultCurrency.setText(savedCurrency, false)

        // Save when user picks a new currency
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

    private fun setupTheme() {
        val prefs = requireContext().getSharedPreferences(
            WelcomeFragment.PREFS_NAME, Context.MODE_PRIVATE
        )
        val themes = listOf("System Default", "Light", "Dark")
        val themeAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            themes
        )
        binding.actvTheme.setAdapter(themeAdapter)

        val savedTheme = prefs.getString(KEY_APP_THEME, "System Default") ?: "System Default"
        binding.actvTheme.setText(savedTheme, false)

        binding.actvTheme.setOnItemClickListener { _, _, position, _ ->
            val selected = themes[position]
            prefs.edit().putString(KEY_APP_THEME, selected).apply()

            when (selected) {
                "Light" -> androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO)
                "Dark" -> androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES)
                else -> androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
        }
    }
}
