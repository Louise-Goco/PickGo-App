package com.example.pickgo.adapters

import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.pickgo.R
import com.example.pickgo.models.Category
import com.google.android.material.chip.Chip

class CategoryChipAdapter(
    private val onCategoryClick: (String?) -> Unit
) : RecyclerView.Adapter<CategoryChipAdapter.CategoryViewHolder>() {

    private var categories: List<Category> = emptyList()

    fun submitList(newCategories: List<Category>) {
        categories = newCategories
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val chip = Chip(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(16, 8, 16, 8)
            isCheckable = true
        }
        return CategoryViewHolder(chip)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(categories[position])
    }

    override fun getItemCount(): Int = categories.size

    inner class CategoryViewHolder(private val chip: Chip) : RecyclerView.ViewHolder(chip) {
        fun bind(category: Category) {
            chip.text = category.name
            chip.isChecked = false
            
            chip.setOnClickListener {
                onCategoryClick(category.name)
            }
        }
    }
}
