package com.example.subtrack.ui.fragment

import android.content.Context
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.example.subtrack.R
import com.example.subtrack.databinding.FragmentLoginBinding
import com.google.firebase.auth.FirebaseAuth

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Check if already logged in
        if (FirebaseAuth.getInstance().currentUser != null) {
            findNavController().navigate(R.id.action_login_to_dashboard)
            return
        }

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnSignIn.setOnClickListener {
            attemptLogin()
        }

        binding.tvSignup.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }

        binding.tvForgotPassword.setOnClickListener {
            Snackbar.make(binding.root, getString(R.string.forgot_password_message), Snackbar.LENGTH_LONG).show()
        }
    }

    private fun attemptLogin() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (!validateForm(email, password)) return

        // Show loading state (if you had a progress bar)
        binding.btnSignIn.isEnabled = false

        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (isAdded) binding.btnSignIn.isEnabled = true
                if (task.isSuccessful) {
                    loginSuccess(email)
                } else {
                    Snackbar.make(binding.root, task.exception?.message ?: getString(R.string.error_wrong_password), Snackbar.LENGTH_LONG).show()
                }
            }
    }


    private fun loginSuccess(email: String) {
        val prefs = requireContext().getSharedPreferences(
            WelcomeFragment.PREFS_NAME, Context.MODE_PRIVATE
        )
        prefs.edit().putString("logged_in_email", email).apply()
        
        findNavController().navigate(R.id.action_login_to_dashboard)
    }

    private fun validateForm(email: String, password: String): Boolean {
        var valid = true

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
        } else {
            binding.tilPassword.error = null
        }

        return valid
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
