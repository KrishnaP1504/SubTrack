package com.example.subtrack.ui.fragment

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.subtrack.R
import com.example.subtrack.databinding.FragmentWelcomeBinding
import com.google.firebase.auth.FirebaseAuth

class WelcomeFragment : Fragment() {

    private var _binding: FragmentWelcomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWelcomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Make the status bar transparent for the immersive dark welcome screen
        activity?.window?.let { window ->
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            ViewCompat.getWindowInsetsController(window.decorView)?.let { controller ->
                controller.isAppearanceLightStatusBars = false
            }
        }

        // Check if user has already seen the welcome screen
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val hasSeenWelcome = prefs.getBoolean(KEY_HAS_SEEN_WELCOME, false)
        val isLoggedIn = FirebaseAuth.getInstance().currentUser != null

        // Skip welcome if already seen
        if (hasSeenWelcome) {
            if (isLoggedIn) {
                findNavController().navigate(R.id.action_welcome_to_dashboard)
            } else {
                findNavController().navigate(R.id.action_welcome_to_login)
            }
            return
        }

        // Set up entrance animations
        playEntranceAnimations()

        // "Get Started" goes to the login/signup screen
        binding.btnGetStarted.setOnClickListener {
            markWelcomeSeen()
            findNavController().navigate(R.id.action_welcome_to_login)
        }

        // "Already have an account" goes directly to login
        binding.tvAlreadyHaveAccount.setOnClickListener {
            markWelcomeSeen()
            findNavController().navigate(R.id.action_welcome_to_login)
        }
    }

    private fun playEntranceAnimations() {
        // Initially hide elements
        binding.ivWelcomeLogo.alpha = 0f
        binding.ivWelcomeLogo.translationY = -30f
        binding.ivWelcomeIllustration.alpha = 0f
        binding.ivWelcomeIllustration.scaleX = 0.7f
        binding.ivWelcomeIllustration.scaleY = 0.7f
        binding.tvWelcomeTitle.alpha = 0f
        binding.tvWelcomeTitle.translationY = 40f
        binding.tvWelcomeSubtitle.alpha = 0f
        binding.tvWelcomeSubtitle.translationY = 30f
        binding.btnGetStarted.alpha = 0f
        binding.btnGetStarted.translationY = 30f
        binding.tvAlreadyHaveAccount.alpha = 0f
        binding.tvAlreadyHaveAccount.translationY = 30f
        binding.tvTerms.alpha = 0f

        // Logo fade in
        val logoAnim = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(binding.ivWelcomeLogo, "alpha", 0f, 1f),
                ObjectAnimator.ofFloat(binding.ivWelcomeLogo, "translationY", -30f, 0f)
            )
            duration = 600
            startDelay = 200
        }

        // Illustration scale + fade
        val illustrationAnim = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(binding.ivWelcomeIllustration, "alpha", 0f, 0.85f),
                ObjectAnimator.ofFloat(binding.ivWelcomeIllustration, "scaleX", 0.7f, 1f),
                ObjectAnimator.ofFloat(binding.ivWelcomeIllustration, "scaleY", 0.7f, 1f)
            )
            duration = 800
            startDelay = 400
            interpolator = OvershootInterpolator(1.2f)
        }

        // Title slide up
        val titleAnim = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(binding.tvWelcomeTitle, "alpha", 0f, 1f),
                ObjectAnimator.ofFloat(binding.tvWelcomeTitle, "translationY", 40f, 0f)
            )
            duration = 600
            startDelay = 700
        }

        // Subtitle fade
        val subtitleAnim = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(binding.tvWelcomeSubtitle, "alpha", 0f, 1f),
                ObjectAnimator.ofFloat(binding.tvWelcomeSubtitle, "translationY", 30f, 0f)
            )
            duration = 500
            startDelay = 900
        }

        // Buttons slide up
        val btnPrimaryAnim = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(binding.btnGetStarted, "alpha", 0f, 1f),
                ObjectAnimator.ofFloat(binding.btnGetStarted, "translationY", 30f, 0f)
            )
            duration = 500
            startDelay = 1100
        }

        val btnSecondaryAnim = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(binding.tvAlreadyHaveAccount, "alpha", 0f, 1f),
                ObjectAnimator.ofFloat(binding.tvAlreadyHaveAccount, "translationY", 30f, 0f)
            )
            duration = 500
            startDelay = 1250
        }

        // Terms fade in last
        val termsAnim = ObjectAnimator.ofFloat(binding.tvTerms, "alpha", 0f, 1f).apply {
            duration = 400
            startDelay = 1400
        }

        // Play all together (each with individual delay)
        AnimatorSet().apply {
            playTogether(
                logoAnim, illustrationAnim, titleAnim, subtitleAnim,
                btnPrimaryAnim, btnSecondaryAnim, termsAnim
            )
            start()
        }
    }

    private fun markWelcomeSeen() {
        requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_HAS_SEEN_WELCOME, true)
            .apply()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val PREFS_NAME = "subtrack_prefs"
        const val KEY_HAS_SEEN_WELCOME = "has_seen_welcome"
        const val KEY_IS_LOGGED_IN = "is_logged_in"
    }
}
