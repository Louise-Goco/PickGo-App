package com.example.pickgo.adapters.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.pickgo.R
import com.example.pickgo.databinding.ItemPromoBinding
import com.example.pickgo.models.admin.PromoCode
import com.example.pickgo.utils.PriceFormatter
import java.text.SimpleDateFormat
import java.util.*

class PromoCodeAdapter(
    private val onDelete: (PromoCode) -> Unit
) : RecyclerView.Adapter<PromoCodeAdapter.PromoViewHolder>() {

    private var items: List<PromoCode> = emptyList()

    fun submitList(newItems: List<PromoCode>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PromoViewHolder {
        val binding = ItemPromoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PromoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PromoViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class PromoViewHolder(private val binding: ItemPromoBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(promo: PromoCode) {
            binding.promoCode.text = promo.code
            binding.usageInfo.text = "${promo.currentUsage} / ${promo.usageLimit} uses"

            val discountText = if (promo.discountType == "percentage") {
                "${promo.discountValue}% OFF"
            } else {
                "${PriceFormatter.format(promo.discountValue)} OFF"
            }
            binding.discountValue.text = discountText
            binding.expiryDate.text = formatDate(promo.expiryDate)

            // Check if expired
            val isExpired = isExpired(promo.expiryDate)
            if (isExpired) {
                binding.expiryDate.setTextColor(android.graphics.Color.parseColor("#ef4444"))
            } else {
                binding.expiryDate.setTextColor(android.graphics.Color.parseColor("#64748b"))
            }

            binding.deleteBtn.setOnClickListener {
                onDelete(promo)
            }
        }

        private fun formatDate(dateString: String): String {
            return try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                val date = inputFormat.parse(dateString)
                outputFormat.format(date ?: Date())
            } catch (e: Exception) {
                dateString
            }
        }

        private fun isExpired(dateString: String): Boolean {
            return try {
                val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val expiryDate = format.parse(dateString)
                val today = Date()
                expiryDate != null && expiryDate.before(today)
            } catch (e: Exception) {
                false
            }
        }
    }
}