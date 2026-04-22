// ui/viewmodel/SubscriptionViewModel.kt

package com.example.subtrack.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.subtrack.data.local.entity.Subscription
import com.example.subtrack.data.repository.SubscriptionRepository
import com.example.subtrack.worker.ReminderScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SubscriptionViewModel(
    application: Application,
    private val repository: SubscriptionRepository
) : AndroidViewModel(application) {

    val subscriptions: StateFlow<List<Subscription>> = repository.allActiveSubscriptions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addSubscription(subscription: Subscription) {
        viewModelScope.launch {
            repository.addSubscription(subscription)
            // Schedule the reminder
            ReminderScheduler.scheduleForSubscription(
                getApplication(),
                subscription.id,
                subscription.renewalDate,
                subscription.name
            )
        }
    }

    fun updateSubscription(subscription: Subscription) {
        viewModelScope.launch {
            repository.updateSubscription(subscription)
            // Update the reminder (ReminderScheduler.REPLACE handles this)
            ReminderScheduler.scheduleForSubscription(
                getApplication(),
                subscription.id,
                subscription.renewalDate,
                subscription.name
            )
        }
    }

    fun deleteSubscription(subscription: Subscription) {
        viewModelScope.launch {
            repository.deleteSubscription(subscription)
            // Cancel the reminder
            ReminderScheduler.cancelReminder(getApplication(), subscription.id)
        }
    }

    class Factory(
        private val application: Application,
        private val repository: SubscriptionRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SubscriptionViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return SubscriptionViewModel(application, repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}