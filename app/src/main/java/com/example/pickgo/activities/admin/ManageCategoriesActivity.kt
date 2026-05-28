package com.example.pickgo.activities.admin

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.example.pickgo.R
import com.example.pickgo.adapters.admin.CategoryAdapter
import com.example.pickgo.databinding.ActivityManageCategoriesBinding
import com.example.pickgo.models.admin.AdminCategory
import com.example.pickgo.utils.FirebaseManager
import com.example.pickgo.utils.SessionManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ManageCategoriesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityManageCategoriesBinding
    private lateinit var firebaseManager: FirebaseManager
    private lateinit var sessionManager: SessionManager
    private lateinit var categoryAdapter: CategoryAdapter
    private var editingCategoryId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageCategoriesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseManager = FirebaseManager()
        sessionManager = SessionManager(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Category Management"

        setupRecyclerView()
        loadCategories()
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        categoryAdapter = CategoryAdapter(
            onEdit = { category ->
                editCategory(category)
            },
            onDelete = { category ->
                confirmDelete(category)
            }
        )
        binding.categoriesRecycler.layoutManager = LinearLayoutManager(this)
        binding.categoriesRecycler.adapter = categoryAdapter
    }

    private fun setupClickListeners() {
        binding.submitBtn.setOnClickListener {
            saveCategory()
        }

        binding.cancelEditBtn.setOnClickListener {
            resetForm()
        }
    }

    private fun loadCategories() {
        lifecycleScope.launch {
            showLoading(true)
            try {
                val categories = firebaseManager.getAllCategories()
                categoryAdapter.submitList(categories)

                if (categories.isEmpty()) {
                    binding.emptyState.visibility = View.VISIBLE
                    binding.categoriesRecycler.visibility = View.GONE
                } else {
                    binding.emptyState.visibility = View.GONE
                    binding.categoriesRecycler.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Error loading categories: ${e.message}", Snackbar.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun saveCategory() {
        val name = binding.categoryNameInput.text.toString().trim()
        val description = binding.descriptionInput.text.toString().trim()

        if (name.isEmpty()) {
            Snackbar.make(binding.root, "Please enter category name", Snackbar.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            showLoading(true)
            try {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val category = AdminCategory(
                    id = editingCategoryId ?: "",
                    categoryName = name,
                    categoryDescription = description,
                    createdAt = dateFormat.format(Date())
                )

                if (editingCategoryId != null) {
                    firebaseManager.updateCategory(category)
                    Snackbar.make(binding.root, "Category updated", Snackbar.LENGTH_SHORT).show()
                } else {
                    firebaseManager.createCategory(category)
                    Snackbar.make(binding.root, "Category created", Snackbar.LENGTH_SHORT).show()
                }
                resetForm()
                loadCategories()
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun editCategory(category: AdminCategory) {
        editingCategoryId = category.id
        binding.categoryNameInput.setText(category.categoryName)
        binding.descriptionInput.setText(category.categoryDescription)
        binding.formTitle.text = "Edit Category"
        binding.submitBtn.text = "Update Category"
        binding.cancelEditBtn.visibility = View.VISIBLE
    }

    private fun resetForm() {
        editingCategoryId = null
        binding.categoryNameInput.text?.clear()
        binding.descriptionInput.text?.clear()
        binding.formTitle.text = "Create New Category"
        binding.submitBtn.text = "Create Category"
        binding.cancelEditBtn.visibility = View.GONE
    }

    private fun confirmDelete(category: AdminCategory) {
        AlertDialog.Builder(this)
            .setTitle("Delete Category")
            .setMessage("Are you sure you want to delete '${category.categoryName}'? Items in this category will be uncategorized.")
            .setPositiveButton("Delete") { _, _ ->
                deleteCategory(category)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteCategory(category: AdminCategory) {
        lifecycleScope.launch {
            showLoading(true)
            try {
                firebaseManager.deleteCategory(category.id)
                Snackbar.make(binding.root, "Category deleted", Snackbar.LENGTH_SHORT).show()
                if (editingCategoryId == category.id) resetForm()
                loadCategories()
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Error: Category may be in use", Snackbar.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.submitBtn.isEnabled = !show
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}