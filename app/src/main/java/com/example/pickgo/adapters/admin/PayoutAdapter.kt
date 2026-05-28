package com.example.pickgo.adapters.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.pickgo.R
import com.example.pickgo.databinding.ItemPayoutBinding
import com.example.pickgo.models.admin.AdminPayout
import com.example.pickgo.utils.PriceFormatter
import java.text.SimpleDateFormat
import java.util.*

class PayoutAdapter(
    private val onApprove: (AdminPayout) -> Unit,
    private val onProcess: (AdminPayout) -> Unit,
    private val onReject: (AdminPayout) -> Unit
) : RecyclerView.Adapter<PayoutAdapter.PayoutViewHolder>() {

    private var items: List<AdminPayout> = emptyList()

    fun submitList(newItems: List<AdminPayout>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PayoutViewHolder {
        val binding = ItemPayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PayoutViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PayoutViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class PayoutViewHolder(private val binding: ItemPayoutBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(payout: AdminPayout) {
            binding.recipientName.text = payout.userName
            binding.userRole.text = payout.userType.uppercase()
            binding.bankInfo.text = payout.bankName
            binding.accountInfo.text = "#${payout.accountNumber} • ${payout.accountName}"
            binding.amount.text = PriceFormatter.format(payout.amount)
            binding.requestDate.text = formatDate(payout.requestDate)

            // Role badge color
            when (payout.userType.lowercase()) {
                "seller" -> binding.userRole.setBackgroundResource(R.drawable.badge_seller)
                "rider" -> binding.userRole.setBackgroundResource(R.drawable.badge_rider)
                else -> binding.userRole.setBackgroundResource(R.drawable.badge_status_processing)
            }

            // Status badge
            when (payout.payoutStatus) {
                "pending" -> {
                    binding.statusBadge.text = "PENDING"
                    binding.statusBadge.setBackgroundResource(R.drawable.badge_status_processing)
                    binding.approveBtn.visibility = android.view.View.VISIBLE
                    binding.rejectBtn.visibility = android.view.View.VISIBLE
                    binding.processBtn.visibility = android.view.View.GONE

                    binding.approveBtn.setOnClickListener { onApprove(payout) }
                    binding.rejectBtn.setOnClickListener { onReject(payout) }
                }
                "approved" -> {
                    binding.statusBadge.text = "APPROVED"
                    binding.statusBadge.setBackgroundResource(R.drawable.badge_status_delivery)
                    binding.approveBtn.visibility = android.view.View.GONE
                    binding.rejectBtn.visibility = android.view.View.GONE
                    binding.processBtn.visibility = android.view.View.VISIBLE

                    binding.processBtn.setOnClickListener { onProcess(payout) }
                }
                "processed" -> {
                    binding.statusBadge.text = "PROCESSED"
                    binding.statusBadge.setBackgroundResource(R.drawable.badge_status_completed)
                    binding.approveBtn.visibility = android.view.View.GONE
                    binding.rejectBtn.visibility = android.view.View.GONE
                    binding.processBtn.visibility = android.view.View.GONE
                }
                "rejected" -> {
                    binding.statusBadge.text = "REJECTED"
                    binding.statusBadge.setBackgroundResource(R.drawable.badge_status_cancelled)
                    binding.approveBtn.visibility = android.view.View.GONE
                    binding.rejectBtn.visibility = android.view.View.GONE
                    binding.processBtn.visibility = android.view.View.GONE
                }
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