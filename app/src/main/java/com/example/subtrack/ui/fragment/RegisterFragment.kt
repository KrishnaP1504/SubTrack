package com.example.subtrack.ui.fragment

import android.content.Context
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.subtrack.R
import com.example.subtrack.databinding.FragmentRegisterBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.tvSignIn.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnCreateAccount.setOnClickListener {
            attemptCreateAccount()
        }
    }

    private fun attemptCreateAccount() {
        val name = binding.etName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()

        if (!validateForm(name, email, password, confirmPassword)) return

        binding.btnCreateAccount.isEnabled = false

        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (isAdded) binding.btnCreateAccount.isEnabled = true
                if (task.isSuccessful) {
                    Snackbar.make(binding.root, getString(R.string.account_created), Snackbar.LENGTH_SHORT).show()
                    loginSuccess(email)
                } else {
                    Snackbar.make(binding.root, task.exception?.message ?: "Sign up failed", Snackbar.LENGTH_LONG).show()
                }
            }
    }

    private fun loginSuccess(email: String) {
        val prefs = requireContext().getSharedPreferences(
            WelcomeFragment.PREFS_NAME, Context.MODE_PRIVATE
        )
        prefs.edit().putString("logged_in_email", email).apply()
        
        findNavController().navigate(R.id.action_register_to_dashboard)
    }

    private fun validateForm(name: String, email: String, password: String, confirmPassword: String): Boolean {
        var valid = true

        if (name.isEmpty()) {
            binding.tilName.error = "Please enter your name"
            valid = false
        } else {
            binding.tilName.error = null
        }

        if (email.isEmpty()) {
            binding.tilEmail.error = getString(R.string.error_email_required)
            valid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = getString(R.string.error_email_invalid)
            valid = false
        } else {
            binding.tilEmail.error = null
        }

        if (password.isEmpty()) {
            binding.tilPassword.error = getString(R.string.error_password_required)
            valid = false
        } else if (password.length < 6) {
            binding.tilPassword.error = getString(R.string.error_password_too_short)
            valid = false
        } else {
            binding.tilPassword.error = null
        }

        if (confirmPassword != password) {
            binding.tilConfirmPassword.error = getString(R.string.error_passwords_dont_match)
            valid = false
        } else {
            binding.tilConfirmPassword.error = null
        }

        return valid
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
