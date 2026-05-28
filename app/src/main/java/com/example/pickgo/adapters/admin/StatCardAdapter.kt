package com.example.pickgo.adapters.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.pickgo.R
import com.example.pickgo.activities.admin.AdminDashboardActivity.StatCard
import com.example.pickgo.databinding.ItemStatCardBinding

class StatCardAdapter : RecyclerView.Adapter<StatCardAdapter.StatCardViewHolder>() {

    private var items: List<StatCard> = emptyList()

    fun submitList(newItems: List<StatCard>) {
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

    class StatCardViewHolder(private val binding: ItemStatCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(stat: StatCard) {
            binding.statTitle.text = stat.title
            binding.statValue.text = stat.value
            Glide.with(binding.root.context)
                .load(stat.iconRes)
                .into(binding.statIcon)
        }
    }
}