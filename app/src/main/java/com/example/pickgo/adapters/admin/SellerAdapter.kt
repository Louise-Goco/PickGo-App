package com.example.pickgo.adapters.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.pickgo.R
import com.example.pickgo.databinding.ItemSellerBinding
import com.example.pickgo.databinding.ItemSectionHeaderBinding
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
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val VIEW_TYPE_HEADER = 0
    private val VIEW_TYPE_SELLER = 1

    private var items: List<Any> = emptyList() // Can be String (header) or AdminSeller

    fun submitList(newItems: List<AdminSeller>) {
        // Group by status and create sectioned list
        val sectionedList = mutableListOf<Any>()
        
        val activeSellers = newItems.filter { it.sellerStatus == "active" }
        val pendingSellers = newItems.filter { it.sellerStatus == "pending" }
        val rejectedSellers = newItems.filter { it.sellerStatus == "rejected" }
        val suspendedSellers = newItems.filter { it.sellerStatus == "suspended" }
        
        if (activeSellers.isNotEmpty()) {
            sectionedList.add("ACTIVE")
            sectionedList.addAll(activeSellers)
        }
        
        if (pendingSellers.isNotEmpty()) {
            sectionedList.add("PENDING")
            sectionedList.addAll(pendingSellers)
        }
        
        if (rejectedSellers.isNotEmpty()) {
            sectionedList.add("REJECTED")
            sectionedList.addAll(rejectedSellers)
        }
        
        if (suspendedSellers.isNotEmpty()) {
            sectionedList.add("SUSPENDED")
            sectionedList.addAll(suspendedSellers)
        }
        
        items = sectionedList
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (items[position] is String) VIEW_TYPE_HEADER else VIEW_TYPE_SELLER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_HEADER) {
            val binding = ItemSectionHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            SectionHeaderViewHolder(binding)
        } else {
            val binding = ItemSellerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            SellerViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is SectionHeaderViewHolder) {
            holder.bind(items[position] as String)
        } else if (holder is SellerViewHolder) {
            holder.bind(items[position] as AdminSeller)
        }
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

    inner class SectionHeaderViewHolder(private val binding: ItemSectionHeaderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(title: String) {
            binding.sectionTitle.text = title
            
            // Count items in this section
            val count = (items as List<Any>).filter { 
                it is AdminSeller && (it as AdminSeller).sellerStatus == title.lowercase() 
            }.size
            binding.sectionCount.text = "$count seller(s)"
            
            // Set color based on status
            val colorRes = when (title) {
                "ACTIVE" -> R.color.success_text
                "PENDING" -> R.color.warning_text
                "REJECTED" -> R.color.error_text
                "SUSPENDED" -> R.color.error_text
                else -> R.color.text_primary
            }
            binding.sectionTitle.setTextColor(binding.root.context.getColor(colorRes))
        }
    }
}