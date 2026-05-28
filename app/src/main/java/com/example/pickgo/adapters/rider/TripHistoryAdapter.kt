package com.example.pickgo.adapters.rider

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.pickgo.R
import com.example.pickgo.databinding.ItemTripHistoryBinding
import com.example.pickgo.models.rider.DeliveryOrder
import com.example.pickgo.utils.PriceFormatter
import java.text.SimpleDateFormat
import java.util.*

class TripHistoryAdapter : RecyclerView.Adapter<TripHistoryAdapter.TripHistoryViewHolder>() {

    private var items: List<DeliveryOrder> = emptyList()
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    fun submitList(newItems: List<DeliveryOrder>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripHistoryViewHolder {
        val binding = ItemTripHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TripHistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TripHistoryViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class TripHistoryViewHolder(private val binding: ItemTripHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(order: DeliveryOrder) {
            // Order ID
            binding.orderId.text = "#${order.orderId.take(8)}"

            // Merchant Name
            binding.merchantName.text = order.merchantName

            // Order Total
            binding.orderTotal.text = PriceFormatter.format(order.orderTotal)

            // Rider Earnings
            binding.riderEarnings.text = PriceFormatter.format(order.riderEarnings)

            // Order Date
            binding.orderDate.text = formatDate(order.orderDate)

            // Bonus Pill (if applicable)
            val hasBonus = order.riderEarnings > order.orderTotal * 0.1
            binding.bonusPill.visibility = if (hasBonus) android.view.View.VISIBLE else android.view.View.GONE
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