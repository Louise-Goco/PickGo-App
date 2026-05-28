package com.example.pickgo.adapters.seller

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.pickgo.R
import com.example.pickgo.databinding.ItemSellerProductBinding
import com.example.pickgo.models.seller.SellerItem
import com.example.pickgo.utils.PriceFormatter

class SellerItemAdapter(
    private val onEdit: (SellerItem) -> Unit,
    private val onDelete: (SellerItem) -> Unit
) : RecyclerView.Adapter<SellerItemAdapter.SellerItemViewHolder>() {

    private var items: List<SellerItem> = emptyList()

    fun submitList(newItems: List<SellerItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SellerItemViewHolder {
        val binding = ItemSellerProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SellerItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SellerItemViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class SellerItemViewHolder(private val binding: ItemSellerProductBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SellerItem) {
            binding.itemName.text = item.itemName
            binding.itemPrice.text = PriceFormatter.format(item.itemPrice)
            binding.itemDescription.text = item.itemDescription.ifEmpty { "No description" }

            // Status badge
            when (item.itemStatus) {
                "available" -> {
                    binding.itemStatus.text = "AVAILABLE"
                    binding.itemStatus.setBackgroundResource(R.drawable.badge_status_completed)
                }
                "pending" -> {
                    binding.itemStatus.text = "PENDING"
                    binding.itemStatus.setBackgroundResource(R.drawable.badge_status_processing)
                }
                "out_of_stock" -> {
                    binding.itemStatus.text = "OUT OF STOCK"
                    binding.itemStatus.setBackgroundResource(R.drawable.badge_status_cancelled)
                }
                "rejected" -> {
                    binding.itemStatus.text = "REJECTED"
                    binding.itemStatus.setBackgroundResource(R.drawable.badge_status_cancelled)
                }
            }

            // Load image
            Glide.with(binding.root.context)
                .load(item.itemImage)
                .placeholder(R.drawable.placeholder_food)
                .error(R.drawable.placeholder_food)
                .into(binding.itemImage)

            // Buttons
            binding.editBtn.setOnClickListener { onEdit(item) }
            binding.deleteBtn.setOnClickListener { onDelete(item) }
        }
    }
}