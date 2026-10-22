// util/CurrencyUtils.kt

package com.example.subtrack.util

import android.content.Context
import java.text.NumberFormat
import java.util.Currency

object CurrencyUtils {

    // The list of currencies shown in the Add/Edit form dropdown
    val supportedCurrencies = listOf("INR", "USD", "EUR", "GBP", "JPY", "CAD", "AUD")

    // Default fallback rates
    private val exchangeRatesToUsd = mutableMapOf(
        "USD" to 1.0,
        "INR" to 83.5,
        "EUR" to 0.92,
        "GBP" to 0.79,
        "JPY" to 150.0,
        "CAD" to 1.35,
        "AUD" to 1.53
    )

    suspend fun fetchLatestExchangeRates() {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val url = java.net.URL(com.example.subtrack.BuildConfig.EXCHANGE_RATE_BASE_URL)
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().readText()
                    val jsonObject = org.json.JSONObject(response)
                    if (jsonObject.getString("result") == "success") {
                        val rates = jsonObject.getJSONObject("rates")
                        for (currency in supportedCurrencies) {
                            if (rates.has(currency)) {
                                exchangeRatesToUsd[currency] = rates.getDouble(currency)
                            }
                        }
                        android.util.Log.d("CurrencyUtils", "Successfully updated live exchange rates")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("CurrencyUtils", "Failed to fetch live exchange rates", e)
            }
        }
    }

    // Gets the user's preferred default currency, defaults to INR
    fun getPreferredCurrency(context: Context): String {
        val prefs = SecurePreferences.getPrefs(context)
        return prefs.getString("default_currency", "INR") ?: "INR"
    }

    fun getDefaultCurrency(context: Context): String = getPreferredCurrency(context)

    fun formatAmount(amount: Double, currencyCode: String): String = format(amount, currencyCode)

    // Converts an amount from one currency to another
    fun convert(amount: Double, fromCurrency: String, toCurrency: String): Double {
        if (fromCurrency == toCurrency) return amount
        
        val rateFromUsd = exchangeRatesToUsd[fromCurrency] ?: 1.0
        val rateToUsd = exchangeRatesToUsd[toCurrency] ?: 1.0
        
        // Convert to USD first, then to target currency
        val amountInUsd = amount / rateFromUsd
        return amountInUsd * rateToUsd
    }

    // Formats a Double into a currency string using the user's locale
    fun format(amount: Double, currencyCode: String): String {
        return try {
            val format = NumberFormat.getCurrencyInstance()
            format.currency = Currency.getInstance(currencyCode)
            format.format(amount)
        } catch (e: Exception) {
            // Fallback if currency code is invalid
            "$currencyCode ${String.format("%.2f", amount)}"
        }
    }

    fun toMonthlyPrice(price: Double, billingCycle: String): Double {
        return when {
            billingCycle.equals("YEARLY", ignoreCase = true) || billingCycle.equals("ANNUAL", ignoreCase = true) -> price / 12.0
            billingCycle.equals("QUARTERLY", ignoreCase = true) -> price / 3.0
            billingCycle.equals("WEEKLY", ignoreCase = true) -> price * 4.33
            else -> price
        }
    }

    fun toYearlyPrice(price: Double, billingCycle: String): Double {
        return when {
            billingCycle.equals("YEARLY", ignoreCase = true) || billingCycle.equals("ANNUAL", ignoreCase = true) -> price
            billingCycle.equals("QUARTERLY", ignoreCase = true) -> price * 4.0
            billingCycle.equals("WEEKLY", ignoreCase = true) -> price * 52.0
            else -> price * 12.0
        }
    }
}