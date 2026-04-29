// SubTrackApplication.kt  (package root)

package com.example.subtrack

import android.app.Application
import com.example.subtrack.data.repository.SubscriptionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SubTrackApplication : Application() {

    // Removed Room database since we are migrating to Firebase Firestore

    // Fix: Use a proper application-scoped coroutine scope instead of GlobalScope.
    // SupervisorJob ensures one failed child doesn't cancel sibling coroutines.
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val repository: SubscriptionRepository by lazy {
        SubscriptionRepository()
    }

    override fun onCreate() {
        super.onCreate()
        val prefs = getSharedPreferences(com.example.subtrack.ui.fragment.WelcomeFragment.PREFS_NAME, android.content.Context.MODE_PRIVATE)
        val theme = prefs.getString("app_theme", "System Default")
        when (theme) {
            "Light" -> androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO)
            "Dark" -> androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES)
            else -> androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }

        // Fetch live exchange rates in the background so it's ready when needed
        appScope.launch {
            com.example.subtrack.util.CurrencyUtils.fetchLatestExchangeRates()
        }
    }
}