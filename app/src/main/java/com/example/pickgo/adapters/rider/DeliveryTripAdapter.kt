package com.example.pickgo.adapters.rider

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.pickgo.R
import com.example.pickgo.databinding.ItemDeliveryTripBinding
import com.example.pickgo.models.rider.DeliveryOrder
import com.example.pickgo.utils.PriceFormatter

class DeliveryTripAdapter(
    private val onAcceptClick: (DeliveryOrder) -> Unit
) : RecyclerView.Adapter<DeliveryTripAdapter.DeliveryTripViewHolder>() {

    private var items: List<DeliveryOrder> = emptyList()

    fun submitList(newItems: List<DeliveryOrder>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeliveryTripViewHolder {
        val binding = ItemDeliveryTripBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DeliveryTripViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DeliveryTripViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class DeliveryTripViewHolder(private val binding: ItemDeliveryTripBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(order: DeliveryOrder) {
            // Order Label
            val orderLabel = if (order.batchId != null) {
                "Batch Delivery (Multi-Store)"
            } else {
                "Order #${order.orderId.take(8)}"
            }
            binding.orderLabel.text = orderLabel

            // Earnings
            val earnings = order.riderEarnings
            binding.earningsAmount.text = PriceFormatter.format(earnings)

            // Merchant Info
            binding.merchantName.text = order.merchantName
            binding.merchantAddress.text = order.merchantAddress

            // Delivery Address
            binding.deliveryAddress.text = order.deliveryAddress

            // Accept Button
            binding.acceptButton.setOnClickListener {
                onAcceptClick(order)
            }
        }
    }
}