package com.example.subtrack.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.subtrack.data.local.entity.Subscription
import com.example.subtrack.databinding.ItemDetectedSubscriptionBinding

class DetectedSubscriptionAdapter(
    private var detectedList: List<Subscription>,
    private val selectedItems: MutableSet<Subscription>
) : RecyclerView.Adapter<DetectedSubscriptionAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemDetectedSubscriptionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(subscription: Subscription) {
            binding.tvServiceName.text = subscription.name
            binding.tvServicePrice.text = "$${subscription.price} / ${subscription.billingCycle}"
            
            // Set checkbox state based on selection set
            binding.cbSelect.isChecked = selectedItems.contains(subscription)
            
            binding.cbSelect.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedItems.add(subscription)
                } else {
                    selectedItems.remove(subscription)
                }
            }

            // Allow clicking the whole card to toggle checkbox
            binding.root.setOnClickListener {
                binding.cbSelect.isChecked = !binding.cbSelect.isChecked
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDetectedSubscriptionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(detectedList[position])
    }

    override fun getItemCount(): Int = detectedList.size

    fun submitList(newList: List<Subscription>) {
        detectedList = newList
        notifyDataSetChanged()
    }
}
