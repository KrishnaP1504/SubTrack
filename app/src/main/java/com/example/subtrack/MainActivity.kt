// MainActivity.kt

package com.example.subtrack

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
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
    private var systemBarsInsets: androidx.core.graphics.Insets = androidx.core.graphics.Insets.NONE

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Notification permission handled
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflate the layout using ViewBinding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Ensure light status bar icons and light navigation bar icons
        androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

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

        // Handle edge-to-edge window insets (status bar at top + 3-button / gesture bar at bottom)
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.mainRoot) { _, windowInsets ->
            systemBarsInsets = windowInsets.getInsets(
                androidx.core.view.WindowInsetsCompat.Type.systemBars()
            )
            updateLayoutInsets(navController.currentDestination?.id)
            androidx.core.view.WindowInsetsCompat.CONSUMED
        }

        // Show/hide bottom nav and adapt padding on destination change
        navController.addOnDestinationChangedListener { _, destination, _ ->
            updateLayoutInsets(destination.id)
        }

        // Modern back press dispatcher for Navigation Component
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!navController.navigateUp()) {
                    finish()
                }
            }
        })

        requestNotificationPermissionIfNeeded()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun updateLayoutInsets(destinationId: Int?) {
        val isBottomNavVisible = when (destinationId) {
            R.id.dashboardFragment,
            R.id.analyticsFragment,
            R.id.scannerFragment,
            R.id.accountFragment -> true
            else -> false
        }

        if (isBottomNavVisible) {
            binding.bottomNav.visibility = View.VISIBLE
            binding.bottomNavDivider.visibility = View.VISIBLE
            // Push content down below status bar (top panel) so toolbar is never hidden
            binding.mainRoot.setPadding(systemBarsInsets.left, systemBarsInsets.top, systemBarsInsets.right, 0)
            // Push bottom navigation items up above 3-button bar or gesture pill
            binding.bottomNav.setPadding(0, 0, 0, systemBarsInsets.bottom)
        } else {
            binding.bottomNav.visibility = View.GONE
            binding.bottomNavDivider.visibility = View.GONE
            // On non-tab screens, pad both top and bottom
            binding.mainRoot.setPadding(
                systemBarsInsets.left,
                systemBarsInsets.top,
                systemBarsInsets.right,
                systemBarsInsets.bottom
            )
        }
    }

    override fun onStart() {
        super.onStart()
        checkBiometricOnLaunch()
    }

    private fun checkBiometricOnLaunch() {
        val prefs = com.example.subtrack.util.SecurePreferences.getPrefs(this)
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

    private fun getBiometricCryptoObject(): BiometricPrompt.CryptoObject? {
        return try {
            val keyStore = java.security.KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            val keyAlias = "subtrack_biometric_key"
            if (!keyStore.containsAlias(keyAlias)) {
                val keyGenerator = javax.crypto.KeyGenerator.getInstance(
                    android.security.keystore.KeyProperties.KEY_ALGORITHM_AES,
                    "AndroidKeyStore"
                )
                val builder = android.security.keystore.KeyGenParameterSpec.Builder(
                    keyAlias,
                    android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or
                    android.security.keystore.KeyProperties.PURPOSE_DECRYPT
                )
                .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_CBC)
                .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_PKCS7)
                .setUserAuthenticationRequired(true)

                keyGenerator.init(builder.build())
                keyGenerator.generateKey()
            }

            val secretKey = keyStore.getKey(keyAlias, null) as javax.crypto.SecretKey
            val cipher = javax.crypto.Cipher.getInstance(
                "${android.security.keystore.KeyProperties.KEY_ALGORITHM_AES}/${android.security.keystore.KeyProperties.BLOCK_MODE_CBC}/${android.security.keystore.KeyProperties.ENCRYPTION_PADDING_PKCS7}"
            )
            cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, secretKey)
            BiometricPrompt.CryptoObject(cipher)
        } catch (e: Exception) {
            null
        }
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
                // If user explicitly cancels or presses negative button, close the app
                if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                    errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                    errorCode == BiometricPrompt.ERROR_CANCELED) {
                    finishAffinity()
                } else {
                    finishAffinity()
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                // Don't close app on failed attempt — BiometricPrompt handles retries
            }
        }

        val biometricPrompt = BiometricPrompt(this, executor, callback)
        val cryptoObject = getBiometricCryptoObject()

        if (cryptoObject != null) {
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.biometric_prompt_title))
                .setSubtitle(getString(R.string.biometric_prompt_subtitle))
                .setNegativeButtonText(getString(R.string.cancel))
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .build()
            biometricPrompt.authenticate(promptInfo, cryptoObject)
        } else {
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
    }
}