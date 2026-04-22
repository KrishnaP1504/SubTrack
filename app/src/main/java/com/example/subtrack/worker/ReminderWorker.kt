// worker/ReminderWorker.kt

package com.example.subtrack.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.subtrack.R

class ReminderWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // Pull data as String
        val subscriptionName = inputData.getString(KEY_SUBSCRIPTION_NAME) ?: return Result.failure()
        val subscriptionId = inputData.getString(KEY_SUBSCRIPTION_ID) ?: ""

        // Build and show the notification
        showReminderNotification(subscriptionName, subscriptionId)

        return Result.success()
    }

    private fun showReminderNotification(name: String, id: String) {
        val channelId = context.getString(R.string.notification_channel_id)

        // Create notification channel (required on Android 8+)
        val channel = NotificationChannel(
            channelId,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Subscription renewal reminders"
        }
        
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager?.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification) // Ensure this exists in res/drawable
            .setContentTitle(context.getString(R.string.notification_title))
            .setContentText(context.getString(R.string.notification_body, name))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        // Use hashCode of the string ID to ensure unique notifications for different subs
        notificationManager?.notify(id.hashCode(), notification)
    }

    companion object {
        const val KEY_SUBSCRIPTION_NAME = "subscription_name"
        const val KEY_SUBSCRIPTION_ID = "subscription_id"
    }
}