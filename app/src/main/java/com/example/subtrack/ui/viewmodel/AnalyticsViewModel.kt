package com.example.subtrack.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.subtrack.data.local.entity.Subscription
import com.example.subtrack.data.repository.SubscriptionRepository
import com.example.subtrack.util.CurrencyUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class AnalyticsData(
    val annualBurnRate: Double,
    val monthlyBurnRate: Double,
    val dailyAverage: Double,
    val categoryBreakdown: Map<String, Double>,
    val totalActiveCount: Int
)

class AnalyticsViewModel(application: Application, repository: SubscriptionRepository) : AndroidViewModel(application) {

    val analyticsData: StateFlow<AnalyticsData> = repository.allActiveSubscriptions
        .map { subscriptions -> computeAnalytics(subscriptions) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AnalyticsData(0.0, 0.0, 0.0, emptyMap(), 0)
        )

    private fun computeAnalytics(subscriptions: List<Subscription>): AnalyticsData {
        val preferredCurrency = CurrencyUtils.getPreferredCurrency(getApplication())
        val monthlyTotal = subscriptions.sumOf { 
            val monthlyPrice = CurrencyUtils.toMonthlyPrice(it.price, it.billingCycle)
            CurrencyUtils.convert(monthlyPrice, it.currency, preferredCurrency)
        }
        val annualTotal = monthlyTotal * 12.0
        val dailyAverage = annualTotal / 365.0
        val breakdown = subscriptions
            .groupBy { it.category }
            .mapValues { (_, subs) -> 
                subs.sumOf { 
                    val monthlyPrice = CurrencyUtils.toMonthlyPrice(it.price, it.billingCycle)
                    CurrencyUtils.convert(monthlyPrice, it.currency, preferredCurrency)
                } 
            }
            .toSortedMap()

        return AnalyticsData(annualTotal, monthlyTotal, dailyAverage, breakdown, subscriptions.size)
    }

    class Factory(private val application: Application, private val repository: SubscriptionRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AnalyticsViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return AnalyticsViewModel(application, repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}