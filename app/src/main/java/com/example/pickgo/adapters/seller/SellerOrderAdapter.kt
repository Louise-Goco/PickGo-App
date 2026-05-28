package com.example.pickgo.adapters.seller

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.pickgo.R
import com.example.pickgo.databinding.ItemOrderItemBinding
import com.example.pickgo.databinding.ItemSellerOrderBinding
import com.example.pickgo.models.seller.SellerOrder
import com.example.pickgo.models.seller.SellerOrderItem
import com.example.pickgo.utils.PriceFormatter
import java.text.SimpleDateFormat
import java.util.*

class SellerOrderAdapter(
    private val onConfirm: (SellerOrder) -> Unit,
    private val onReject: (SellerOrder) -> Unit,
    private val onDispatch: (SellerOrder) -> Unit
) : RecyclerView.Adapter<SellerOrderAdapter.SellerOrderViewHolder>() {

    private var items: List<SellerOrder> = emptyList()
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())

    fun submitList(newItems: List<SellerOrder>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SellerOrderViewHolder {
        val binding = ItemSellerOrderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SellerOrderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SellerOrderViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class SellerOrderViewHolder(private val binding: ItemSellerOrderBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(order: SellerOrder) {
            // Order Header
            binding.orderId.text = "#${order.orderId.take(8)}"
            binding.orderDate.text = formatDate(order.orderDate)
            binding.customerName.text = order.customerName.ifEmpty { "Customer" }
            binding.customerPhone.text = order.customerPhone ?: "No phone"
            binding.deliveryAddress.text = order.deliveryAddress
            binding.orderTotal.text = PriceFormatter.format(order.orderTotal)

            // Status Badge
            binding.orderStatus.text = formatStatus(order.orderStatus)
            when (order.orderStatus) {
                "pending" -> binding.orderStatus.setBackgroundResource(R.drawable.badge_status_processing)
                "preparing" -> binding.orderStatus.setBackgroundResource(R.drawable.badge_status_processing)
                "ready_for_pickup" -> binding.orderStatus.setBackgroundResource(R.drawable.badge_status_delivery)
                "on_the_way" -> binding.orderStatus.setBackgroundResource(R.drawable.badge_status_delivery)
                "delivered" -> binding.orderStatus.setBackgroundResource(R.drawable.badge_status_completed)
                "cancelled" -> binding.orderStatus.setBackgroundResource(R.drawable.badge_status_cancelled)
            }

            // Order Items
            val itemsAdapter = OrderItemsAdapter(order.items)
            binding.orderItemsRecycler.adapter = itemsAdapter

            // Action Buttons Visibility
            when (order.orderStatus) {
                "pending" -> {
                    binding.actionButtons.visibility = android.view.View.VISIBLE
                    binding.dispatchBtn.visibility = android.view.View.GONE
                    binding.waitingText.visibility = android.view.View.GONE

                    binding.confirmBtn.setOnClickListener { onConfirm(order) }
                    binding.rejectBtn.setOnClickListener { onReject(order) }
                }
                "preparing" -> {
                    binding.actionButtons.visibility = android.view.View.GONE
                    binding.dispatchBtn.visibility = android.view.View.VISIBLE
                    binding.waitingText.visibility = android.view.View.GONE

                    binding.dispatchBtn.setOnClickListener { onDispatch(order) }
                }
                "ready_for_pickup", "on_the_way" -> {
                    binding.actionButtons.visibility = android.view.View.GONE
                    binding.dispatchBtn.visibility = android.view.View.GONE
                    binding.waitingText.visibility = android.view.View.VISIBLE
                }
                else -> {
                    binding.actionButtons.visibility = android.view.View.GONE
                    binding.dispatchBtn.visibility = android.view.View.GONE
                    binding.waitingText.visibility = android.view.View.GONE
                }
            }
        }

        private fun formatStatus(status: String): String {
            return status.replace("_", " ").split(" ")
                .joinToString(" ") { it.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } }
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

    inner class OrderItemsAdapter(private val items: List<SellerOrderItem>) :
        RecyclerView.Adapter<OrderItemsAdapter.OrderItemViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderItemViewHolder {
            val binding = ItemOrderItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return OrderItemViewHolder(binding)
        }

        override fun onBindViewHolder(holder: OrderItemViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size

        inner class OrderItemViewHolder(private val binding: ItemOrderItemBinding) :
            RecyclerView.ViewHolder(binding.root) {
            fun bind(item: SellerOrderItem) {
                binding.itemName.text = "${item.quantity}x ${item.foodName}"
                binding.itemPrice.text = PriceFormatter.format(item.subtotal)
            }
        }
    }
}