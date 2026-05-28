package com.example.pickgo.adapters.seller

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.pickgo.R
import com.example.pickgo.databinding.ItemRecentEarningsBinding
import com.example.pickgo.models.seller.SellerOrder
import com.example.pickgo.utils.PriceFormatter
import java.text.SimpleDateFormat
import java.util.*

class RecentEarningsAdapter : RecyclerView.Adapter<RecentEarningsAdapter.RecentEarningsViewHolder>() {

    private var items: List<SellerOrder> = emptyList()
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    fun submitList(newItems: List<SellerOrder>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentEarningsViewHolder {
        val binding = ItemRecentEarningsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RecentEarningsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecentEarningsViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class RecentEarningsViewHolder(private val binding: ItemRecentEarningsBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(order: SellerOrder) {
            binding.orderId.text = "#${order.orderId.take(8)}"
            binding.orderDate.text = formatDate(order.orderDate)
            binding.orderAmount.text = PriceFormatter.format(order.orderTotal)
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