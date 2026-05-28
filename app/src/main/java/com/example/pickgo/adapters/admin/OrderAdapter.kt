package com.example.pickgo.adapters.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.recyclerview.widget.RecyclerView
import com.example.pickgo.R
import com.example.pickgo.databinding.ItemOrderBinding
import com.example.pickgo.models.admin.AdminOrder
import com.example.pickgo.utils.PriceFormatter
import java.text.SimpleDateFormat
import java.util.*

class OrderAdapter(
    private val onStatusChange: (AdminOrder, String) -> Unit,
    private val onViewDetails: (AdminOrder) -> Unit
) : RecyclerView.Adapter<OrderAdapter.OrderViewHolder>() {

    private var items: List<AdminOrder> = emptyList()
    private val statuses = listOf("pending", "preparing", "ready_for_pickup", "on_the_way", "delivered", "cancelled")

    fun submitList(newItems: List<AdminOrder>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val binding = ItemOrderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return OrderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class OrderViewHolder(private val binding: ItemOrderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(order: AdminOrder) {
            binding.orderId.text = "#${order.orderId}"
            binding.customerName.text = order.customerName
            binding.customerEmail.text = order.customerEmail
            binding.merchantName.text = order.merchantName
            binding.orderTotal.text = PriceFormatter.format(order.orderTotal)
            binding.orderDate.text = formatDate(order.orderDate)

            // Set status badge color
            when (order.orderStatus) {
                "pending", "preparing" -> binding.orderStatus.setBackgroundResource(R.drawable.badge_status_processing)
                "ready_for_pickup", "on_the_way" -> binding.orderStatus.setBackgroundResource(R.drawable.badge_status_delivery)
                "delivered" -> binding.orderStatus.setBackgroundResource(R.drawable.badge_status_completed)
                "cancelled" -> binding.orderStatus.setBackgroundResource(R.drawable.badge_status_cancelled)
            }
            binding.orderStatus.text = order.orderStatus.replace("_", " ").uppercase()

            // Setup status spinner
            val statusAdapter = ArrayAdapter(binding.root.context, android.R.layout.simple_spinner_dropdown_item, statuses)
            binding.statusSpinner.adapter = statusAdapter
            val position = statuses.indexOf(order.orderStatus)
            if (position >= 0) {
                binding.statusSpinner.setSelection(position)
            }

            binding.statusSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, pos: Int, id: Long) {
                    val newStatus = statuses[pos]
                    if (newStatus != order.orderStatus) {
                        onStatusChange(order, newStatus)
                    }
                }
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            }

            binding.root.setOnClickListener {
                onViewDetails(order)
            }
        }

        private fun formatDate(dateString: String): String {
            return try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                val date = inputFormat.parse(dateString)
                outputFormat.format(date ?: Date())
            } catch (e: Exception) {
                dateString
            }
        }
    }
}