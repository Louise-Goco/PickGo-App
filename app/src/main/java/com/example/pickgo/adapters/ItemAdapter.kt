package com.example.pickgo.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.pickgo.R
import com.example.pickgo.databinding.ItemCardBinding
import com.example.pickgo.models.Item
import com.example.pickgo.utils.PriceFormatter

class ItemAdapter(
    private val onItemClick: (Item) -> Unit
) : RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {

    private var items: List<Item> = emptyList()

    fun submitList(newItems: List<Item>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding = ItemCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ItemViewHolder(private val binding: ItemCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Item) {
            binding.itemName.text = item.itemName
            binding.merchantName.text = item.merchantName
            binding.itemPrice.text = PriceFormatter.format(item.itemPrice)

            Glide.with(binding.root.context)
                .load(item.itemImage)
                .placeholder(R.drawable.placeholder_food)
                .error(R.drawable.placeholder_food)
                .into(binding.itemImage)

            binding.addToCartButton.setOnClickListener {
                onItemClick(item)
            }
        }
    }
}