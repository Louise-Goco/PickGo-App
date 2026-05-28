package com.example.pickgo.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.pickgo.R
import com.example.pickgo.databinding.OrderCardBinding
import com.example.pickgo.models.Order
import com.example.pickgo.models.OrderStatus
import com.example.pickgo.utils.PriceFormatter
import java.text.SimpleDateFormat
import java.util.Locale

class OrderAdapter(
    private val onOrderClick: (Order) -> Unit
) : RecyclerView.Adapter<OrderAdapter.OrderViewHolder>() {

    private var orders: List<Order> = emptyList()
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    fun submitList(newOrders: List<Order>) {
        orders = newOrders
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val binding = OrderCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return OrderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.bind(orders[position])
    }

    override fun getItemCount(): Int = orders.size

    inner class OrderViewHolder(private val binding: OrderCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(order: Order) {
            val itemsSummary = order.items.take(2).joinToString(", ") { it.foodName }
            val displayText = if (order.items.size > 2) "$itemsSummary + ${order.items.size - 2} more" else itemsSummary

            binding.orderSummary.text = displayText.ifEmpty { "Order" }
            binding.merchantName.text = order.merchantName
            binding.orderDate.text = dateFormat.format(order.orderDate)
            binding.orderTotal.text = PriceFormatter.format(order.orderTotal)

            val status = OrderStatus.fromString(order.orderStatus)
            binding.orderStatus.text = status.displayName
            binding.orderStatus.setBackgroundResource(getStatusBackground(status))

            binding.root.setOnClickListener {
                onOrderClick(order)
            }
        }

        private fun getStatusBackground(status: OrderStatus): Int {
            return when (status) {
                OrderStatus.PENDING, OrderStatus.PREPARING -> R.drawable.badge_status_processing
                OrderStatus.READY_FOR_PICKUP, OrderStatus.ON_THE_WAY -> R.drawable.badge_status_delivery
                OrderStatus.DELIVERED -> R.drawable.badge_status_completed
                OrderStatus.CANCELLED -> R.drawable.badge_status_cancelled
            }
        }
    }
}