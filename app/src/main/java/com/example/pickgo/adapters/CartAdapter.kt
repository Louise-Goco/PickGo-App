package com.example.pickgo.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.pickgo.R
import com.example.pickgo.databinding.CartItemCardBinding
import com.example.pickgo.models.CartItem
import com.example.pickgo.utils.PriceFormatter

class CartAdapter(
    private val onQuantityChange: (String, Int) -> Unit,
    private val onRemove: (String) -> Unit
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    private var items: List<CartItem> = emptyList()

    fun submitList(newItems: List<CartItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val binding = CartItemCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CartViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class CartViewHolder(private val binding: CartItemCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: CartItem) {
            binding.itemName.text = item.itemName
            binding.merchantName.text = item.merchantName
            binding.itemPrice.text = PriceFormatter.format(item.itemPrice)
            binding.quantityText.text = item.quantity.toString()
            binding.lineTotal.text = PriceFormatter.format(item.lineTotal)

            Glide.with(binding.root.context)
                .load(item.itemImage)
                .placeholder(R.drawable.placeholder_food)
                .error(R.drawable.placeholder_food)
                .into(binding.itemImage)

            binding.decreaseButton.setOnClickListener {
                onQuantityChange(item.itemId, -1)
            }

            binding.increaseButton.setOnClickListener {
                onQuantityChange(item.itemId, 1)
            }

            binding.removeButton.setOnClickListener {
                onRemove(item.itemId)
            }
        }
    }
}
