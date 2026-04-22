// MainActivity.kt

package com.example.subtrack

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.example.subtrack.databinding.ActivityMainBinding
import com.example.subtrack.ui.fragment.AccountFragment
import com.example.subtrack.ui.fragment.WelcomeFragment

class MainActivity : AppCompatActivity() {

    // ViewBinding gives us typed references to every view in activity_main.xml
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflate the layout using ViewBinding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Step 1: Find the NavHostFragment we declared in activity_main.xml
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment

        // Step 2: Get the NavController from it
        navController = navHostFragment.navController

        // Step 3: Tell the BottomNavigationView to work with the NavController.
        binding.bottomNav.setupWithNavController(navController)

        // Step 4: Define which fragments are "top-level" (no back arrow shown)
        val appBarConfig = AppBarConfiguration(
            setOf(
                R.id.dashboardFragment,
                R.id.analyticsFragment,
                R.id.scannerFragment,
                R.id.accountFragment
            )
        )

        // Hide bottom nav on screens that aren't main tabs
        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.bottomNav.visibility = when (destination.id) {
                R.id.dashboardFragment,
                R.id.analyticsFragment,
                R.id.scannerFragment,
                R.id.accountFragment -> View.VISIBLE
                else                 -> View.GONE
            }
        }

        // Check biometric on app open (only if user is logged in and biometric is enabled)
        if (savedInstanceState == null) {
            checkBiometricOnLaunch()
        }
    }

    private fun checkBiometricOnLaunch() {
        val prefs = getSharedPreferences(WelcomeFragment.PREFS_NAME, Context.MODE_PRIVATE)
        val isLoggedIn = prefs.getBoolean(WelcomeFragment.KEY_IS_LOGGED_IN, false)
        val biometricEnabled = prefs.getBoolean(AccountFragment.KEY_BIOMETRIC_ENABLED, false)

        if (!isLoggedIn || !biometricEnabled) return

        // Check hardware availability
        val biometricManager = BiometricManager.from(this)
        val canAuthenticate = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) return

        // Show biometric prompt
        showBiometricPrompt()
    }

    private fun showBiometricPrompt() {
        // Cover the content while biometric is shown
        binding.root.visibility = View.INVISIBLE

        val executor = ContextCompat.getMainExecutor(this)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                binding.root.visibility = View.VISIBLE
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                // If user cancels or fails, close the app
                if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                    errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                    errorCode == BiometricPrompt.ERROR_CANCELED) {
                    finishAffinity()
                } else {
                    // For other errors, let them in (graceful fallback)
                    binding.root.visibility = View.VISIBLE
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                // Don't close app on failed attempt — BiometricPrompt handles retries
            }
        }

        val biometricPrompt = BiometricPrompt(this, executor, callback)

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.biometric_prompt_title))
            .setSubtitle(getString(R.string.biometric_prompt_subtitle))
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    // Make the system back button work correctly with Navigation Component
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (!navController.navigateUp()) {
            super.onBackPressed()
        }
    }
}