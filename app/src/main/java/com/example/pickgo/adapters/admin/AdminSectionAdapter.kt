package com.example.pickgo.adapters.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.pickgo.R
import com.example.pickgo.activities.admin.AdminDashboardActivity.AdminSection
import com.example.pickgo.databinding.ItemAdminSectionBinding

class AdminSectionAdapter(
    private val sections: List<AdminSection>
) : RecyclerView.Adapter<AdminSectionAdapter.AdminSectionViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdminSectionViewHolder {
        val binding = ItemAdminSectionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AdminSectionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AdminSectionViewHolder, position: Int) {
        holder.bind(sections[position])
    }

    override fun getItemCount(): Int = sections.size

    class AdminSectionViewHolder(private val binding: ItemAdminSectionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(section: AdminSection) {
            binding.sectionTitle.text = section.title
            binding.sectionDescription.text = section.description
            binding.sectionIcon.setImageResource(section.iconRes)

            binding.root.setOnClickListener {
                section.onClick()
            }
        }
    }
}