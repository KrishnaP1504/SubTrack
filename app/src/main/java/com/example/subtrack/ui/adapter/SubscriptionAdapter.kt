package com.example.subtrack.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.subtrack.data.local.entity.Subscription
import com.example.subtrack.databinding.ItemSubscriptionBinding
import com.example.subtrack.util.CurrencyUtils
import com.example.subtrack.util.DateUtils

// ListAdapter is a smarter version of RecyclerView.Adapter.
// It takes a DiffUtil.ItemCallback and handles list updates automatically.
// You just call adapter.submitList(newList) and it does the rest.
class SubscriptionAdapter(
    private val onItemClick: (Subscription) -> Unit,
    private val onDeleteClick: (Subscription) -> Unit
) : ListAdapter<Subscription, SubscriptionAdapter.SubscriptionViewHolder>(DiffCallback) {

    // DiffCallback tells the adapter HOW to compare two Subscription objects.
    // It needs two checks:
    //   1. areItemsTheSame  — are these the same logical item? (check the ID)
    //   2. areContentsTheSame — has any data in this item changed? (check all fields)
    companion object DiffCallback : DiffUtil.ItemCallback<Subscription>() {

        override fun areItemsTheSame(oldItem: Subscription, newItem: Subscription): Boolean {
            // If the IDs match, they represent the same subscription in the database
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Subscription, newItem: Subscription): Boolean {
            // data class gives us .equals() that compares every field automatically
            return oldItem == newItem
        }
    }

    // onCreateViewHolder is called once per visible row to INFLATE the layout.
    // Think of it as building the empty template for a row.
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubscriptionViewHolder {
        // ViewBinding inflates item_subscription.xml and gives us typed references
        // to every view inside it — no findViewById() needed.
        val binding = ItemSubscriptionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false  // false = don't attach to parent yet, RecyclerView handles that
        )
        return SubscriptionViewHolder(binding)
    }

    // onBindViewHolder is called every time a row becomes visible.
    // It takes the ViewHolder (the empty template) and fills it with real data.
    override fun onBindViewHolder(holder: SubscriptionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // ViewHolder holds references to all the views in ONE row.
    // Caching these references avoids calling findViewById() on every scroll.
    inner class SubscriptionViewHolder(
        private val binding: ItemSubscriptionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(subscription: Subscription) {

            // ── Text fields ──
            binding.tvSubscriptionName.text = subscription.name
            binding.tvCategory.text = subscription.category
            binding.tvRenewalDate.text = DateUtils.getRenewalLabel(subscription.renewalDate)
            val preferredCurrency = CurrencyUtils.getPreferredCurrency(binding.root.context)
            val convertedPrice = CurrencyUtils.convert(subscription.price, subscription.currency, preferredCurrency)
            binding.tvPrice.text = CurrencyUtils.format(convertedPrice, preferredCurrency)
            binding.chipBillingCycle.text = if (subscription.billingCycle == "MONTHLY") "Monthly" else "Yearly"

            // ── Subscription Logo ──
            // Try to load the company logo; fall back to colored letter if unavailable
            try {
                binding.cvLogoCard.setCardBackgroundColor(Color.parseColor(subscription.color))
            } catch (e: Exception) {
                binding.cvLogoCard.setCardBackgroundColor(Color.parseColor("#6750A4"))
            }
            binding.tvFallbackLetter.text = subscription.name.firstOrNull()?.uppercase() ?: "S"
            binding.tvFallbackLetter.setTextColor(Color.WHITE)

            com.example.subtrack.util.LogoUtils.loadLogo(
                imageView = binding.ivLogo,
                subscriptionName = subscription.name,
                fallbackLetterView = binding.tvFallbackLetter,
                fallbackColor = subscription.color
            )

            // ── Urgency color on renewal date text ──
            // If renewal is within 3 days, turn the date label red to alert the user
            val daysLeft = DateUtils.daysUntil(subscription.renewalDate)
            binding.tvRenewalDate.setTextColor(
                when {
                    daysLeft < 0  -> binding.root.context.getColor(android.R.color.holo_red_light)
                    daysLeft <= 3 -> binding.root.context.getColor(android.R.color.holo_orange_light)
                    else          -> binding.root.context.getAttr(com.google.android.material.R.attr.colorOnSurfaceVariant)
                }
            )

            // ── Click listeners ──
            binding.root.setOnClickListener { onItemClick(subscription) }

            // Long press to show delete option
            binding.root.setOnLongClickListener {
                onDeleteClick(subscription)
                true  // return true = we consumed the event, don't propagate it
            }
        }
    }
}

// Extension function to resolve a theme color attribute into an actual Int color.
// ?attr/colorOnSurfaceVariant is a theme reference — we need to resolve it at runtime.
fun android.content.Context.getAttr(attr: Int): Int {
    val typedArray = obtainStyledAttributes(intArrayOf(attr))
    val color = typedArray.getColor(0, 0)
    typedArray.recycle()  // always recycle TypedArray to avoid memory leaks
    return color
}