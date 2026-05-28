package com.example.pickgo.adapters.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.pickgo.R
import com.example.pickgo.databinding.ItemSellerBinding
import com.example.pickgo.models.admin.AdminSeller
import java.text.SimpleDateFormat
import java.util.*

class SellerAdapter(
    private val onEdit: (AdminSeller) -> Unit,
    private val onApprove: (AdminSeller) -> Unit,
    private val onReject: (AdminSeller) -> Unit,
    private val onSuspend: (AdminSeller) -> Unit,
    private val onActivate: (AdminSeller) -> Unit,
    private val onDelete: (AdminSeller) -> Unit,
    private val onViewDocuments: (AdminSeller) -> Unit
) : RecyclerView.Adapter<SellerAdapter.SellerViewHolder>() {

    private var items: List<AdminSeller> = emptyList()

    fun submitList(newItems: List<AdminSeller>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SellerViewHolder {
        val binding = ItemSellerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SellerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SellerViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class SellerViewHolder(private val binding: ItemSellerBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(seller: AdminSeller) {
            val fullName = "${seller.firstName} ${seller.lastName}"
            val initials = "${seller.firstName.firstOrNull()}${seller.lastName.firstOrNull()}".uppercase()

            binding.sellerName.text = fullName
            binding.sellerEmail.text = seller.email
            binding.sellerInitials.text = initials
            binding.storeName.text = seller.merchantName
            binding.storeType.text = seller.merchantType
            binding.sellerRating.text = String.format("%.1f ★", seller.sellerRating)
            binding.joinDate.text = formatDate(seller.createdAt)

            // Status badge
            when (seller.sellerStatus) {
                "active" -> {
                    binding.statusBadge.text = "ACTIVE"
                    binding.statusBadge.setBackgroundResource(R.drawable.badge_status_completed)
                }
                "pending" -> {
                    binding.statusBadge.text = "PENDING"
                    binding.statusBadge.setBackgroundResource(R.drawable.badge_status_processing)
                }
                "suspended" -> {
                    binding.statusBadge.text = "SUSPENDED"
                    binding.statusBadge.setBackgroundResource(R.drawable.badge_status_cancelled)
                }
                "rejected" -> {
                    binding.statusBadge.text = "REJECTED"
                    binding.statusBadge.setBackgroundResource(R.drawable.badge_status_cancelled)
                }
                else -> {
                    binding.statusBadge.text = seller.sellerStatus.uppercase()
                    binding.statusBadge.setBackgroundResource(R.drawable.badge_status_processing)
                }
            }

            // Document button
            binding.viewDocsBtn.setOnClickListener {
                onViewDocuments(seller)
            }

            // Action buttons
            binding.editBtn.setOnClickListener { onEdit(seller) }
            binding.deleteBtn.setOnClickListener { onDelete(seller) }

            when (seller.sellerStatus) {
                "pending" -> {
                    binding.approveBtn.visibility = android.view.View.VISIBLE
                    binding.rejectBtn.visibility = android.view.View.VISIBLE
                    binding.suspendBtn.visibility = android.view.View.GONE
                    binding.activateBtn.visibility = android.view.View.GONE

                    binding.approveBtn.setOnClickListener { onApprove(seller) }
                    binding.rejectBtn.setOnClickListener { onReject(seller) }
                }
                "active" -> {
                    binding.approveBtn.visibility = android.view.View.GONE
                    binding.rejectBtn.visibility = android.view.View.GONE
                    binding.suspendBtn.visibility = android.view.View.VISIBLE
                    binding.activateBtn.visibility = android.view.View.GONE

                    binding.suspendBtn.setOnClickListener { onSuspend(seller) }
                }
                "suspended", "rejected" -> {
                    binding.approveBtn.visibility = android.view.View.GONE
                    binding.rejectBtn.visibility = android.view.View.GONE
                    binding.suspendBtn.visibility = android.view.View.GONE
                    binding.activateBtn.visibility = android.view.View.VISIBLE

                    binding.activateBtn.setOnClickListener { onActivate(seller) }
                }
                else -> {
                    binding.approveBtn.visibility = android.view.View.GONE
                    binding.rejectBtn.visibility = android.view.View.GONE
                    binding.suspendBtn.visibility = android.view.View.GONE
                    binding.activateBtn.visibility = android.view.View.GONE
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