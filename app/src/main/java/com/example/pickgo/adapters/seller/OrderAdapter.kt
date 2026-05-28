package com.example.pickgo.adapters.seller

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.pickgo.R
import com.example.pickgo.databinding.ItemOrderSummaryBinding
import com.example.pickgo.models.seller.SellerOrder
import com.example.pickgo.utils.PriceFormatter
import java.text.SimpleDateFormat
import java.util.*

class OrderAdapter(
    private val onOrderClick: (SellerOrder) -> Unit
) : RecyclerView.Adapter<OrderAdapter.OrderViewHolder>() {

    private var items: List<SellerOrder> = emptyList()
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    fun submitList(newItems: List<SellerOrder>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val binding = ItemOrderSummaryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return OrderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class OrderViewHolder(private val binding: ItemOrderSummaryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(order: SellerOrder) {
            val itemSummary = order.items.take(2).joinToString(", ") { it.foodName }
            val displayText = if (order.items.size > 2) {
                "$itemSummary + ${order.items.size - 2} more"
            } else {
                itemSummary.ifEmpty { "Order" }
            }

            binding.orderSummary.text = displayText
            binding.merchantName.text = order.merchantName
            binding.orderDate.text = formatDate(order.orderDate)
            binding.orderTotal.text = PriceFormatter.format(order.orderTotal)

            // Status
            val statusLabel = order.orderStatus.replace("_", " ")
                .split(" ")
                .joinToString(" ") { it.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } }
            binding.orderStatus.text = statusLabel

            when (order.orderStatus) {
                "pending", "preparing" -> binding.orderStatus.setBackgroundResource(R.drawable.badge_status_processing)
                "ready_for_pickup", "on_the_way" -> binding.orderStatus.setBackgroundResource(R.drawable.badge_status_delivery)
                "delivered" -> binding.orderStatus.setBackgroundResource(R.drawable.badge_status_completed)
                "cancelled" -> binding.orderStatus.setBackgroundResource(R.drawable.badge_status_cancelled)
            }

            binding.root.setOnClickListener { onOrderClick(order) }
        }

        private fun formatDate(dateString: String): String {
            return try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val date = inputFormat.parse(dateString)
                dateFormat.format(date ?: Date())
            } catch (e: Exception) {
                dateString
            }
        }
    }
}