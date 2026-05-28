package com.example.pickgo.adapters.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.pickgo.R
import com.example.pickgo.databinding.ItemCustomerBinding
import com.example.pickgo.models.admin.AdminCustomer

class CustomerAdapter(
    private val onEdit: (AdminCustomer) -> Unit,
    private val onDelete: (AdminCustomer) -> Unit,
    private val onSuspend: (AdminCustomer) -> Unit,
    private val onActivate: (AdminCustomer) -> Unit
) : RecyclerView.Adapter<CustomerAdapter.CustomerViewHolder>() {

    private var items: List<AdminCustomer> = emptyList()

    fun submitList(newItems: List<AdminCustomer>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomerViewHolder {
        val binding = ItemCustomerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CustomerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CustomerViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class CustomerViewHolder(private val binding: ItemCustomerBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(customer: AdminCustomer) {
            val fullName = "${customer.firstName} ${customer.lastName}"
            val initials = "${customer.firstName.firstOrNull()}${customer.lastName.firstOrNull()}".uppercase()

            binding.customerName.text = fullName
            binding.customerEmail.text = customer.email
            binding.customerPhone.text = customer.phoneNumber
            binding.customerInitials.text = initials

            // Status badge - commented out, not in layout
            // when (customer.accountStatus) {
            //     "active" -> {
            //         binding.statusBadge.text = "ACTIVE"
            //         binding.statusBadge.setBackgroundResource(R.drawable.badge_status_complete)
            //     }
            //     "suspended" -> {
            //         binding.statusBadge.text = "SUSPENDED"
            //         binding.statusBadge.setBackgroundResource(R.drawable.badge_status_cancelled)
            //     }
            //     else -> {
            //         binding.statusBadge.text = customer.accountStatus.uppercase()
            //         binding.statusBadge.setBackgroundResource(R.drawable.badge_status_processing)
            //     }
            // }

            // Verified badge - commented out, not in layout
            // if (customer.isVerified) {
            //     binding.verifiedBadge.visibility = android.view.View.VISIBLE
            // } else {
            //     binding.verifiedBadge.visibility = android.view.View.GONE
            // }

            // Action buttons
            binding.editBtn.setOnClickListener { onEdit(customer) }
            binding.deleteBtn.setOnClickListener { onDelete(customer) }

            when (customer.accountStatus) {
                "active" -> {
                    binding.suspendBtn.visibility = android.view.View.VISIBLE
                    binding.activateBtn.visibility = android.view.View.GONE
                    binding.suspendBtn.setOnClickListener { onSuspend(customer) }
                }
                "suspended" -> {
                    binding.suspendBtn.visibility = android.view.View.GONE
                    binding.activateBtn.visibility = android.view.View.VISIBLE
                    binding.activateBtn.setOnClickListener { onActivate(customer) }
                }
                else -> {
                    binding.suspendBtn.visibility = android.view.View.GONE
                    binding.activateBtn.visibility = android.view.View.GONE
                }
            }
        }
    }
}
