package com.example.subtrack.ui.fragment

import android.content.Context
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.subtrack.R
import com.example.subtrack.SubTrackApplication
import com.example.subtrack.databinding.FragmentLoginBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private var storedVerificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    private var fullPhoneNumber: String = ""
    private var resendTimer: CountDownTimer? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()

        // Check if already logged in
        if (auth.currentUser != null) {
            findNavController().navigate(R.id.action_login_to_dashboard)
            return
        }

        // Back press dispatcher: If on OTP screen, go back to phone number input screen
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (_binding != null && binding.layoutOtpInput.visibility == View.VISIBLE) {
                        showPhoneInputStep()
                    } else {
                        isEnabled = false
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        )

        binding.toolbar.setNavigationOnClickListener {
            if (binding.layoutOtpInput.visibility == View.VISIBLE) {
                showPhoneInputStep()
            } else {
                findNavController().navigateUp()
            }
        }

        binding.btnSendOtp.setOnClickListener {
            attemptSendOtp()
        }

        binding.btnVerifyOtp.setOnClickListener {
            attemptVerifyOtp()
        }

        binding.btnChangePhone.setOnClickListener {
            showPhoneInputStep()
        }

        binding.tvResendOtp.setOnClickListener {
            if (binding.tvResendOtp.isEnabled) {
                resendVerificationCode()
            }
        }
    }

    private fun attemptSendOtp() {
        val countryCode = binding.etCountryCode.text.toString().trim()
        val phoneNumber = binding.etPhone.text.toString().trim()

        if (phoneNumber.isEmpty()) {
            binding.tilPhone.error = getString(R.string.error_phone_required)
            return
        }

        if (phoneNumber.length < 10) {
            binding.tilPhone.error = getString(R.string.error_phone_invalid)
            return
        }

        binding.tilPhone.error = null
        fullPhoneNumber = if (countryCode.startsWith("+")) {
            "$countryCode$phoneNumber"
        } else {
            "+$countryCode$phoneNumber"
        }

        // Test credentials bypass for fast offline / mock testing: +91 9999999999
        if (phoneNumber == "9999999999" || phoneNumber == "1234567890") {
            storedVerificationId = "TEST_VERIFICATION_ID"
            showOtpInputStep()
            Snackbar.make(binding.root, "Test mode: enter OTP 123456", Snackbar.LENGTH_LONG).show()
            return
        }

        setPhoneLoading(true)

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                if (!isAdded) return
                setPhoneLoading(false)
                // Auto-retrieval completed or instant verification succeeded
                val code = credential.smsCode
                if (!code.isNullOrEmpty()) {
                    binding.etOtp.setText(code)
                }
                signInWithPhoneCredential(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                if (!isAdded) return
                setPhoneLoading(false)
                val errorMessage = e.message ?: getString(R.string.error_otp_failed)
                Snackbar.make(binding.root, errorMessage, Snackbar.LENGTH_LONG).show()
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                if (!isAdded) return
                setPhoneLoading(false)
                storedVerificationId = verificationId
                resendToken = token
                showOtpInputStep()
            }
        }

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(fullPhoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(requireActivity())
            .setCallbacks(callbacks)
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun attemptVerifyOtp() {
        val otpCode = binding.etOtp.text.toString().trim()

        if (otpCode.isEmpty()) {
            binding.tilOtp.error = getString(R.string.error_otp_required)
            return
        }

        if (otpCode.length < 6) {
            binding.tilOtp.error = getString(R.string.error_otp_invalid)
            return
        }

        binding.tilOtp.error = null

        // Test credentials bypass
        if (storedVerificationId == "TEST_VERIFICATION_ID") {
            if (otpCode == "123456") {
                onLoginSuccess(fullPhoneNumber)
            } else {
                binding.tilOtp.error = getString(R.string.error_otp_failed)
            }
            return
        }

        val verificationId = storedVerificationId
        if (verificationId == null) {
            Snackbar.make(binding.root, getString(R.string.error_otp_failed), Snackbar.LENGTH_SHORT).show()
            return
        }

        setOtpLoading(true)
        val credential = PhoneAuthProvider.getCredential(verificationId, otpCode)
        signInWithPhoneCredential(credential)
    }

    private fun signInWithPhoneCredential(credential: PhoneAuthCredential) {
        setOtpLoading(true)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(requireActivity()) { task ->
                if (!isAdded) return@addOnCompleteListener
                setOtpLoading(false)
                if (task.isSuccessful) {
                    onLoginSuccess(fullPhoneNumber)
                } else {
                    val message = task.exception?.message ?: getString(R.string.error_otp_failed)
                    Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
                }
            }
    }

    private fun onLoginSuccess(phone: String) {
        setOtpLoading(true)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val app = requireActivity().application as SubTrackApplication
                app.repository.saveUserProfile(phone)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            if (!isAdded) return@launch

            // Save local session in SharedPreferences
            val prefs = requireContext().getSharedPreferences(
                WelcomeFragment.PREFS_NAME, Context.MODE_PRIVATE
            )
            prefs.edit()
                .putString("logged_in_phone", phone)
                .putString("logged_in_email", phone) // fallback for existing displays
                .putBoolean(WelcomeFragment.KEY_IS_LOGGED_IN, true)
                .apply()

            setOtpLoading(false)
            Snackbar.make(binding.root, getString(R.string.phone_verified), Snackbar.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_login_to_dashboard)
        }
    }

    private fun resendVerificationCode() {
        val token = resendToken ?: return
        setOtpLoading(true)

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                if (!isAdded) return
                setOtpLoading(false)
                signInWithPhoneCredential(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                if (!isAdded) return
                setOtpLoading(false)
                Snackbar.make(binding.root, e.message ?: "Resend failed", Snackbar.LENGTH_LONG).show()
            }

            override fun onCodeSent(
                verificationId: String,
                newToken: PhoneAuthProvider.ForceResendingToken
            ) {
                if (!isAdded) return
                setOtpLoading(false)
                storedVerificationId = verificationId
                resendToken = newToken
                startResendTimer()
                Snackbar.make(binding.root, "New code sent!", Snackbar.LENGTH_SHORT).show()
            }
        }

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(fullPhoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(requireActivity())
            .setCallbacks(callbacks)
            .setForceResendingToken(token)
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun showOtpInputStep() {
        binding.layoutPhoneInput.visibility = View.GONE
        binding.layoutOtpInput.visibility = View.VISIBLE
        binding.tvHeaderTitle.text = getString(R.string.otp_title)
        binding.tvHeaderSubtitle.text = getString(R.string.otp_subtitle, fullPhoneNumber)
        binding.tvOtpSubtitle.text = getString(R.string.otp_subtitle, fullPhoneNumber)
        startResendTimer()
    }

    private fun showPhoneInputStep() {
        resendTimer?.cancel()
        binding.layoutOtpInput.visibility = View.GONE
        binding.layoutPhoneInput.visibility = View.VISIBLE
        binding.tvHeaderTitle.text = getString(R.string.phone_login_title)
        binding.tvHeaderSubtitle.text = getString(R.string.phone_login_subtitle)
    }

    private fun startResendTimer() {
        binding.tvResendOtp.isEnabled = false
        binding.tvResendOtp.setTextColor(
            ContextCompat.getColor(requireContext(), R.color.app_text_muted)
        )
        resendTimer?.cancel()
        resendTimer = object : CountDownTimer(30000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                if (!isAdded) return
                val seconds = millisUntilFinished / 1000
                binding.tvResendOtp.text = getString(R.string.resend_otp_timer, seconds)
            }

            override fun onFinish() {
                if (!isAdded) return
                binding.tvResendOtp.isEnabled = true
                binding.tvResendOtp.text = getString(R.string.resend_otp)
                binding.tvResendOtp.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.app_primary)
                )
            }
        }.start()
    }

    private fun setPhoneLoading(loading: Boolean) {
        binding.btnSendOtp.isEnabled = !loading
        binding.btnSendOtp.text = if (loading) "" else getString(R.string.get_otp)
        binding.pbSendOtp.visibility = if (loading) View.VISIBLE else View.GONE
    }

    private fun setOtpLoading(loading: Boolean) {
        binding.btnVerifyOtp.isEnabled = !loading
        binding.btnVerifyOtp.text = if (loading) "" else getString(R.string.verify_and_continue)
        binding.pbVerifyOtp.visibility = if (loading) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        resendTimer?.cancel()
        _binding = null
    }
}
