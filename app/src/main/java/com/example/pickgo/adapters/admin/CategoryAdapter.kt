package com.example.pickgo.adapters.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.pickgo.R
import com.example.pickgo.databinding.ItemCategoryBinding
import com.example.pickgo.models.admin.AdminCategory

class CategoryAdapter(
    private val onEdit: (AdminCategory) -> Unit,
    private val onDelete: (AdminCategory) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    private var items: List<AdminCategory> = emptyList()

    fun submitList(newItems: List<AdminCategory>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class CategoryViewHolder(private val binding: ItemCategoryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(category: AdminCategory) {
            binding.categoryName.text = category.categoryName
            binding.categoryDescription.text = category.categoryDescription.ifEmpty { "No description" }

            binding.editBtn.setOnClickListener {
                onEdit(category)
            }

            binding.deleteBtn.setOnClickListener {
                onDelete(category)
            }
        }
    }
}