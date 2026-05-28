package com.example.pickgo.adapters.seller

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.pickgo.R
import com.example.pickgo.databinding.ItemPayoutHistoryBinding
import com.example.pickgo.models.seller.SellerPayout
import com.example.pickgo.utils.PriceFormatter
import java.text.SimpleDateFormat
import java.util.*

class PayoutHistoryAdapter : RecyclerView.Adapter<PayoutHistoryAdapter.PayoutHistoryViewHolder>() {

    private var items: List<SellerPayout> = emptyList()
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    fun submitList(newItems: List<SellerPayout>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PayoutHistoryViewHolder {
        val binding = ItemPayoutHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PayoutHistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PayoutHistoryViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class PayoutHistoryViewHolder(private val binding: ItemPayoutHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(payout: SellerPayout) {
            binding.payoutDate.text = formatDate(payout.requestDate)
            binding.bankInfo.text = "${payout.bankName}\n${payout.accountName}"
            binding.payoutAmount.text = PriceFormatter.format(payout.amount)

            binding.payoutStatus.text = payout.payoutStatus.uppercase()
            when (payout.payoutStatus.lowercase()) {
                "pending" -> {
                    binding.payoutStatus.setBackgroundResource(R.drawable.badge_status_processing)
                    binding.payoutStatus.setTextColor(android.graphics.Color.parseColor("#854d0e"))
                }
                "approved" -> {
                    binding.payoutStatus.setBackgroundResource(R.drawable.badge_status_delivery)
                    binding.payoutStatus.setTextColor(android.graphics.Color.parseColor("#1e40af"))
                }
                "processed" -> {
                    binding.payoutStatus.setBackgroundResource(R.drawable.badge_status_completed)
                    binding.payoutStatus.setTextColor(android.graphics.Color.parseColor("#166534"))
                }
                "rejected" -> {
                    binding.payoutStatus.setBackgroundResource(R.drawable.badge_status_cancelled)
                    binding.payoutStatus.setTextColor(android.graphics.Color.parseColor("#991b1b"))
                }
            }
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