package com.example.subtrack.util

import android.content.Context
import android.provider.Telephony
import com.example.subtrack.data.local.entity.Subscription
import java.util.Calendar
import java.util.Locale
import java.util.UUID
import java.util.regex.Pattern

object SmsSubscriptionDetector {

    // Known service providers and their categories & brand colors
    private data class ServiceInfo(
        val name: String,
        val category: String,
        val colorHex: String
    )

    private val knownServices = listOf(
        ServiceInfo("Netflix", "Entertainment", "#E50914"),
        ServiceInfo("Spotify", "Music", "#1DB954"),
        ServiceInfo("Amazon Prime", "Shopping", "#00A8E1"),
        ServiceInfo("Prime Video", "Entertainment", "#00A8E1"),
        ServiceInfo("Disney+ Hotstar", "Entertainment", "#0C2053"),
        ServiceInfo("Hotstar", "Entertainment", "#0C2053"),
        ServiceInfo("YouTube Premium", "Entertainment", "#FF0000"),
        ServiceInfo("YouTube", "Entertainment", "#FF0000"),
        ServiceInfo("Apple Music", "Music", "#FA2D48"),
        ServiceInfo("Apple Services", "Productivity", "#000000"),
        ServiceInfo("iCloud", "Cloud Storage", "#3399FF"),
        ServiceInfo("Google One", "Cloud Storage", "#4285F4"),
        ServiceInfo("Google Play", "Utilities", "#00875F"),
        ServiceInfo("SonyLIV", "Entertainment", "#0032FF"),
        ServiceInfo("Zee5", "Entertainment", "#8230C6"),
        ServiceInfo("JioCinema", "Entertainment", "#E80071"),
        ServiceInfo("ChatGPT", "Productivity", "#10A37F"),
        ServiceInfo("OpenAI", "Productivity", "#10A37F"),
        ServiceInfo("Cult.fit", "Health & Fitness", "#FF3278"),
        ServiceInfo("Adobe", "Design", "#FF0000"),
        ServiceInfo("Microsoft 365", "Productivity", "#0078D4"),
        ServiceInfo("GitHub", "Developer", "#24292E"),
        ServiceInfo("LinkedIn", "Professional", "#0A66C2")
    )

    // Keywords that indicate recurring subscription / payment
    private val subscriptionKeywords = listOf(
        "subscription",
        "renewed",
        "renewal",
        "auto-debit",
        "autodebit",
        "autopay",
        "mandate",
        "recurring",
        "membership",
        "monthly plan",
        "annual plan",
        "standing instruction",
        "debited for"
    )

    // Regex to extract monetary amounts
    private val amountPatterns = listOf(
        Pattern.compile("""(?:Rs\.?|INR|₹)\s*([0-9]+(?:\.[0-9]{1,2})?)""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""\$\s*([0-9]+(?:\.[0-9]{1,2})?)"""),
        Pattern.compile("""€\s*([0-9]+(?:\.[0-9]{1,2})?)"""),
        Pattern.compile("""£\s*([0-9]+(?:\.[0-9]{1,2})?)"""),
        Pattern.compile("""([0-9]+(?:\.[0-9]{1,2})?)\s*(?:INR|USD|EUR|GBP)""", Pattern.CASE_INSENSITIVE)
    )

    /**
     * Reads real SMS messages from the device inbox and extracts actual detected subscriptions.
     * Returns ONLY real subscriptions found in user SMS. If none found, returns an empty list.
     */
    fun scanDeviceSms(context: Context): List<Subscription> {
        val detectedMap = mutableMapOf<String, Subscription>()

        try {
            val contentResolver = context.contentResolver
            val cursor = contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                arrayOf(
                    Telephony.Sms._ID,
                    Telephony.Sms.ADDRESS,
                    Telephony.Sms.BODY,
                    Telephony.Sms.DATE
                ),
                null,
                null,
                "${Telephony.Sms.DATE} DESC"
            )

            cursor?.use {
                val bodyIndex = it.getColumnIndex(Telephony.Sms.BODY)
                val dateIndex = it.getColumnIndex(Telephony.Sms.DATE)
                val addressIndex = it.getColumnIndex(Telephony.Sms.ADDRESS)

                while (it.moveToNext()) {
                    val body = it.getString(bodyIndex) ?: continue
                    val smsDate = it.getLong(dateIndex)
                    val sender = it.getString(addressIndex) ?: ""

                    val bodyLower = body.lowercase(Locale.ROOT)
                    val senderLower = sender.lowercase(Locale.ROOT)

                    // 1. Identify which service is referenced
                    var matchedService: ServiceInfo? = null
                    for (service in knownServices) {
                        if (bodyLower.contains(service.name.lowercase(Locale.ROOT)) ||
                            senderLower.contains(service.name.lowercase(Locale.ROOT).replace(" ", ""))
                        ) {
                            matchedService = service
                            break
                        }
                    }

                    if (matchedService == null) continue

                    // 2. Check if the message contains subscription or debit keywords
                    val hasKeyword = subscriptionKeywords.any { kw -> bodyLower.contains(kw) }
                    val hasDebit = bodyLower.contains("debited") || bodyLower.contains("paid") || bodyLower.contains("charged")

                    if (!hasKeyword && !hasDebit) continue

                    // 3. Extract the price/amount
                    var extractedAmount: Double? = null
                    var detectedCurrency = "USD"

                    for (pattern in amountPatterns) {
                        val matcher = pattern.matcher(body)
                        if (matcher.find()) {
                            val amountStr = matcher.group(1)
                            extractedAmount = amountStr?.toDoubleOrNull()
                            if (extractedAmount != null && extractedAmount > 0) {
                                val matchFull = matcher.group(0)?.uppercase(Locale.ROOT) ?: ""
                                detectedCurrency = when {
                                    matchFull.contains("RS") || matchFull.contains("INR") || matchFull.contains("₹") -> "INR"
                                    matchFull.contains("€") || matchFull.contains("EUR") -> "EUR"
                                    matchFull.contains("£") || matchFull.contains("GBP") -> "GBP"
                                    else -> "USD"
                                }
                                break
                            }
                        }
                    }

                    if (extractedAmount == null || extractedAmount <= 0.0) continue

                    // 4. Calculate next renewal date (default next month from transaction date)
                    val cal = Calendar.getInstance().apply {
                        timeInMillis = smsDate
                        add(Calendar.MONTH, 1)
                        // If that's already in the past, roll forward to future
                        while (timeInMillis < System.currentTimeMillis()) {
                            add(Calendar.MONTH, 1)
                        }
                    }

                    // 5. Store if we don't already have a more recent record for this service
                    if (!detectedMap.containsKey(matchedService.name)) {
                        val subscription = Subscription(
                            id = UUID.randomUUID().toString(),
                            name = matchedService.name,
                            price = extractedAmount,
                            currency = detectedCurrency,
                            billingCycle = "MONTHLY",
                            renewalDate = cal.timeInMillis,
                            category = matchedService.category,
                            color = matchedService.colorHex
                        )
                        detectedMap[matchedService.name] = subscription
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return detectedMap.values.toList()
    }
}
