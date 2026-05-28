package com.example.pickgo.adapters.seller

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.pickgo.R
import com.example.pickgo.databinding.ItemStatCardBinding

data class StatCardItem(
    val title: String,
    val value: String,
    val iconRes: Int
)

class StatCardAdapter : RecyclerView.Adapter<StatCardAdapter.StatCardViewHolder>() {

    private var items: List<StatCardItem> = emptyList()

    fun submitList(newItems: List<StatCardItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StatCardViewHolder {
        val binding = ItemStatCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return StatCardViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StatCardViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class StatCardViewHolder(private val binding: ItemStatCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: StatCardItem) {
            binding.statTitle.text = item.title
            binding.statValue.text = item.value
            Glide.with(binding.root.context)
                .load(item.iconRes)
                .into(binding.statIcon)
        }
    }
}