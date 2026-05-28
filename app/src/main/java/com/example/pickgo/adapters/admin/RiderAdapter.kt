package com.example.pickgo.adapters.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.pickgo.R
import com.example.pickgo.databinding.ItemRiderBinding
import com.example.pickgo.models.admin.AdminRider

class RiderAdapter(
    private val onEdit: (AdminRider) -> Unit,
    private val onApprove: (AdminRider) -> Unit,
    private val onReject: (AdminRider) -> Unit,
    private val onSuspend: (AdminRider) -> Unit,
    private val onActivate: (AdminRider) -> Unit,
    private val onVerify: (AdminRider) -> Unit,
    private val onDelete: (AdminRider) -> Unit,
    private val onViewDocuments: (AdminRider) -> Unit
) : RecyclerView.Adapter<RiderAdapter.RiderViewHolder>() {

    private var items: List<AdminRider> = emptyList()

    fun submitList(newItems: List<AdminRider>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RiderViewHolder {
        val binding = ItemRiderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RiderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RiderViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class RiderViewHolder(private val binding: ItemRiderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(rider: AdminRider) {
            val fullName = "${rider.firstName} ${rider.lastName}"
            val initials = "${rider.firstName.firstOrNull()}${rider.lastName.firstOrNull()}".uppercase()

            binding.riderName.text = fullName
            binding.riderEmail.text = rider.email
            binding.riderPhone.text = rider.phoneNumber
            binding.riderInitials.text = initials
            binding.vehicleType.text = rider.vehicleType
            binding.plateNumber.text = rider.plateNumber
            binding.riderRating.text = String.format("%.1f ★", rider.riderRating)
            binding.deliveryCount.text = "${rider.totalDeliveries} Deliveries"

            // Status badge
            when (rider.riderStatus) {
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
                    binding.statusBadge.text = rider.riderStatus.uppercase()
                    binding.statusBadge.setBackgroundResource(R.drawable.badge_status_processing)
                }
            }

            // Verified badge
            if (rider.isVerified) {
                binding.verifiedBadge.visibility = android.view.View.VISIBLE
            } else {
                binding.verifiedBadge.visibility = android.view.View.GONE
            }

            // Document buttons
            binding.viewDocsBtn.setOnClickListener {
                onViewDocuments(rider)
            }

            // Action buttons visibility based on status
            binding.editBtn.setOnClickListener { onEdit(rider) }
            binding.deleteBtn.setOnClickListener { onDelete(rider) }

            when (rider.riderStatus) {
                "pending" -> {
                    binding.approveBtn.visibility = android.view.View.VISIBLE
                    binding.rejectBtn.visibility = android.view.View.VISIBLE
                    binding.suspendBtn.visibility = android.view.View.GONE
                    binding.activateBtn.visibility = android.view.View.GONE
                    binding.verifyBtn.visibility = android.view.View.VISIBLE

                    binding.approveBtn.setOnClickListener { onApprove(rider) }
                    binding.rejectBtn.setOnClickListener { onReject(rider) }
                    binding.verifyBtn.setOnClickListener { onVerify(rider) }
                }
                "active" -> {
                    binding.approveBtn.visibility = android.view.View.GONE
                    binding.rejectBtn.visibility = android.view.View.GONE
                    binding.suspendBtn.visibility = android.view.View.VISIBLE
                    binding.activateBtn.visibility = android.view.View.GONE
                    binding.verifyBtn.visibility = if (!rider.isVerified) android.view.View.VISIBLE else android.view.View.GONE

                    binding.suspendBtn.setOnClickListener { onSuspend(rider) }
                    binding.verifyBtn.setOnClickListener { onVerify(rider) }
                }
                "suspended", "rejected" -> {
                    binding.approveBtn.visibility = android.view.View.GONE
                    binding.rejectBtn.visibility = android.view.View.GONE
                    binding.suspendBtn.visibility = android.view.View.GONE
                    binding.activateBtn.visibility = android.view.View.VISIBLE
                    binding.verifyBtn.visibility = if (!rider.isVerified) android.view.View.VISIBLE else android.view.View.GONE

                    binding.activateBtn.setOnClickListener { onActivate(rider) }
                    binding.verifyBtn.setOnClickListener { onVerify(rider) }
                }
                else -> {
                    binding.approveBtn.visibility = android.view.View.GONE
                    binding.rejectBtn.visibility = android.view.View.GONE
                    binding.suspendBtn.visibility = android.view.View.GONE
                    binding.activateBtn.visibility = android.view.View.GONE
                    binding.verifyBtn.visibility = android.view.View.GONE
                }
            }
        }
    }
}