package com.example.pickgo.adapters.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.pickgo.R
import com.example.pickgo.databinding.ItemProductBinding
import com.example.pickgo.models.admin.AdminProduct
import com.example.pickgo.utils.PriceFormatter

class ProductAdapter(
    private val onApprove: (AdminProduct) -> Unit,
    private val onReject: (AdminProduct) -> Unit,
    private val onView: (AdminProduct) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    private var items: List<AdminProduct> = emptyList()

    fun submitList(newItems: List<AdminProduct>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ProductViewHolder(private val binding: ItemProductBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(product: AdminProduct) {
            binding.productName.text = product.itemName
            binding.merchantName.text = product.merchantName
            binding.categoryName.text = product.categoryName
            binding.productPrice.text = PriceFormatter.format(product.itemPrice)
            binding.productDate.text = formatDate(product.createdAt)

            Glide.with(binding.root.context)
                .load(product.itemImage)
                .placeholder(R.drawable.placeholder_food)
                .error(R.drawable.placeholder_food)
                .into(binding.productImage)

            // Set status badge
            when (product.itemStatus) {
                "pending" -> {
                    binding.statusBadge.text = "PENDING"
                    binding.statusBadge.setBackgroundResource(R.drawable.badge_status_processing)
                }
                "available" -> {
                    binding.statusBadge.text = "APPROVED"
                    binding.statusBadge.setBackgroundResource(R.drawable.badge_status_completed)
                }
                "rejected" -> {
                    binding.statusBadge.text = "REJECTED"
                    binding.statusBadge.setBackgroundResource(R.drawable.badge_status_cancelled)
                }
            }

            // Show/hide action buttons based on status
            if (product.itemStatus == "pending") {
                binding.approveBtn.visibility = android.view.View.VISIBLE
                binding.rejectBtn.visibility = android.view.View.VISIBLE
            } else {
                binding.approveBtn.visibility = android.view.View.GONE
                binding.rejectBtn.visibility = android.view.View.GONE
            }

            binding.approveBtn.setOnClickListener {
                onApprove(product)
            }

            binding.rejectBtn.setOnClickListener {
                onReject(product)
            }

            binding.root.setOnClickListener {
                onView(product)
            }
        }

        private fun formatDate(dateString: String): String {
            return try {
                val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                val outputFormat = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                val date = inputFormat.parse(dateString)
                outputFormat.format(date ?: java.util.Date())
            } catch (e: Exception) {
                dateString
            }
        }
    }
}