package com.example.subtrack.util

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object SecurePreferences {

    private const val SECURE_PREFS_NAME = "subtrack_secure_prefs"
    private const val FALLBACK_PREFS_NAME = "subtrack_prefs"

    @Volatile
    private var sharedPreferences: SharedPreferences? = null

    fun getPrefs(context: Context): SharedPreferences {
        return sharedPreferences ?: synchronized(this) {
            sharedPreferences ?: createEncryptedPrefs(context.applicationContext).also {
                sharedPreferences = it
            }
        }
    }

    private fun createEncryptedPrefs(context: Context): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            val encryptedPrefs = EncryptedSharedPreferences.create(
                context,
                SECURE_PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            // Migrate any existing unencrypted keys if present
            migrateFromLegacyPrefs(context, encryptedPrefs)

            encryptedPrefs
        } catch (e: Exception) {
            android.util.Log.e("SecurePreferences", "KeyStore initialization failed, using standard mode", e)
            context.getSharedPreferences(FALLBACK_PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    private fun migrateFromLegacyPrefs(context: Context, securePrefs: SharedPreferences) {
        val legacyPrefs = context.getSharedPreferences(FALLBACK_PREFS_NAME, Context.MODE_PRIVATE)
        val allEntries = legacyPrefs.all
        if (allEntries.isNotEmpty()) {
            val editor = securePrefs.edit()
            for ((key, value) in allEntries) {
                if (!securePrefs.contains(key)) {
                    when (value) {
                        is String -> editor.putString(key, value)
                        is Boolean -> editor.putBoolean(key, value)
                        is Int -> editor.putInt(key, value)
                        is Long -> editor.putLong(key, value)
                        is Float -> editor.putFloat(key, value)
                    }
                }
            }
            editor.apply()
        }
    }
}
