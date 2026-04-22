package com.example.subtrack.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.subtrack.data.local.entity.Subscription
import com.example.subtrack.databinding.ItemTrendingSubscriptionBinding

class TrendingSubscriptionAdapter(
    private var trendingList: List<Subscription>,
    private val onItemClick: (Subscription) -> Unit
) : RecyclerView.Adapter<TrendingSubscriptionAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemTrendingSubscriptionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(subscription: Subscription) {
            binding.tvTrendingName.text = subscription.name
            binding.tvTrendingCategory.text = subscription.category
            
            // Set fallback color on card
            try {
                binding.cvTrendingIcon.setCardBackgroundColor(Color.parseColor(subscription.color))
            } catch (e: Exception) {
                // fallback
            }

            // Load real company logo (falls back to letter if unavailable)
            com.example.subtrack.util.LogoUtils.loadLogo(
                imageView = binding.ivTrendingLogo,
                subscriptionName = subscription.name,
                fallbackLetterView = binding.tvTrendingIconLetter,
                fallbackColor = subscription.color
            )

            binding.root.setOnClickListener {
                onItemClick(subscription)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTrendingSubscriptionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(trendingList[position])
    }

    override fun getItemCount(): Int = trendingList.size

    fun submitList(newList: List<Subscription>) {
        trendingList = newList
        notifyDataSetChanged()
    }
}
