package com.example.pickgo.adapters.seller

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.pickgo.R
import com.example.pickgo.databinding.ItemBestSellerBinding
import com.example.pickgo.models.seller.BestSellerItem
import com.example.pickgo.utils.PriceFormatter

class BestSellerAdapter : RecyclerView.Adapter<BestSellerAdapter.BestSellerViewHolder>() {

    private var items: List<BestSellerItem> = emptyList()

    fun submitList(newItems: List<BestSellerItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BestSellerViewHolder {
        val binding = ItemBestSellerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BestSellerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BestSellerViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class BestSellerViewHolder(private val binding: ItemBestSellerBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: BestSellerItem) {
            binding.itemName.text = item.itemName
            binding.quantitySold.text = "${item.quantitySold} sold"
            binding.totalSales.text = PriceFormatter.format(item.totalSales)
        }
    }
}