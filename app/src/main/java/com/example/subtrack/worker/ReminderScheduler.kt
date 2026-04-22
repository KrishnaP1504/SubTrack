// worker/ReminderScheduler.kt

package com.example.subtrack.worker

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

object ReminderScheduler {

    fun scheduleForSubscription(
        context: Context,
        id: String,
        renewalDate: Long,
        name: String,
        hoursBeforeRenewal: Long = 24  // remind 24 hours before
    ) {
        val reminderTime = renewalDate - (hoursBeforeRenewal * 60 * 60 * 1000)
        val delay = reminderTime - System.currentTimeMillis()

        if (delay <= 0) return // renewal is already past, don't schedule

        // Package the data to send to the Worker
        val inputData = workDataOf(
            ReminderWorker.KEY_SUBSCRIPTION_NAME to name,
            ReminderWorker.KEY_SUBSCRIPTION_ID to id
        )

        // OneTimeWorkRequest = fire once at the right time, then done
        val reminderRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .addTag("reminder_$id") // tag lets us cancel this specific reminder later
            .build()

        // REPLACE means: if a reminder for this subscription already exists,
        // cancel the old one and set the new one (handles updates correctly)
        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                "reminder_$id",
                ExistingWorkPolicy.REPLACE,
                reminderRequest
            )
    }

    fun cancelReminder(context: Context, id: String) {
        WorkManager.getInstance(context)
            .cancelUniqueWork("reminder_$id")
    }
}