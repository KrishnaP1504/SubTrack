// data/local/entity/Subscription.kt

package com.example.subtrack.data.local.entity

data class Subscription(
    val id: String = "",
    val name: String = "",           // "Netflix", "Spotify"
    val price: Double = 0.0,         // 14.99
    val currency: String = "USD",    // "USD"
    val billingCycle: String = "MONTHLY", // "MONTHLY" or "YEARLY"
    val renewalDate: Long = 0L,      // stored as Unix timestamp in milliseconds
    val category: String = "",       // "Entertainment", "Productivity"
    val color: String = "#FF5733"    // hex color for UI card
)