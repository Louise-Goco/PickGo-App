package com.example.pickgo.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.pickgo.R
import com.example.pickgo.databinding.StoreCardBinding
import com.example.pickgo.models.Merchant

class StoreAdapter(
    private val onStoreClick: (Merchant) -> Unit
) : RecyclerView.Adapter<StoreAdapter.StoreViewHolder>() {

    private var stores: List<Merchant> = emptyList()

    fun submitList(newStores: List<Merchant>) {
        stores = newStores
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoreViewHolder {
        val binding = StoreCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return StoreViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StoreViewHolder, position: Int) {
        holder.bind(stores[position])
    }

    override fun getItemCount(): Int = stores.size

    inner class StoreViewHolder(private val binding: StoreCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(store: Merchant) {
            binding.storeName.text = store.merchName
            binding.storeType.text = store.merchType
            binding.storeRating.text = String.format("%.1f", store.rating)

            Glide.with(binding.root.context)
                .load(store.merchLogo)
                .placeholder(R.drawable.placeholder_store)
                .error(R.drawable.placeholder_store)
                .into(binding.storeLogo)

            binding.root.setOnClickListener {
                onStoreClick(store)
            }
        }
    }
}