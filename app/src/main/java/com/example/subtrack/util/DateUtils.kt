// util/DateUtils.kt

package com.example.subtrack.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object DateUtils {

    // The format shown in the UI: "Jun 1, 2025"
    private val displayFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

    // Converts a Unix timestamp (Long) into a readable string for the UI
    fun formatDate(timestamp: Long): String {
        return displayFormat.format(Date(timestamp))
    }

    // Converts a readable string back into a timestamp
    // Returns null if parsing fails (defensive programming)
    fun parseDate(dateString: String): Long? {
        return try {
            displayFormat.parse(dateString)?.time
        } catch (e: Exception) {
            null
        }
    }

    // Returns how many days until a given timestamp
    // Negative number means the date has already passed
    fun daysUntil(timestamp: Long): Long {
        val now = System.currentTimeMillis()
        val diffMillis = timestamp - now
        return TimeUnit.MILLISECONDS.toDays(diffMillis)
    }

    // Builds a human-readable "due soon" label
    // e.g. "Renews tomorrow", "Renews in 5 days", "Overdue"
    fun getRenewalLabel(timestamp: Long): String {
        val days = daysUntil(timestamp)
        return when {
            days < 0  -> "Overdue"
            days == 0L -> "Renews today"
            days == 1L -> "Renews tomorrow"
            days <= 7  -> "Renews in $days days"
            else       -> "Renews ${formatDate(timestamp)}"
        }
    }

    // Returns a Calendar object set to midnight of the given timestamp
    // Used to pre-populate the DatePickerDialog correctly
    fun timestampToCalendar(timestamp: Long): Calendar {
        return Calendar.getInstance().apply {
            timeInMillis = timestamp
        }
    }

    // Checks if a renewal is coming up within N days (used for "upcoming" count)
    fun isWithinDays(timestamp: Long, days: Int): Boolean {
        val future = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(days.toLong())
        return timestamp in System.currentTimeMillis()..future
    }
}