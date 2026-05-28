package com.example.pickgo.activities.seller

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.example.pickgo.R
import com.example.pickgo.adapters.seller.SellerItemAdapter
import com.example.pickgo.databinding.ActivityManageItemsBinding
import com.example.pickgo.models.seller.SellerItem
import com.example.pickgo.utils.FirebaseManager
import com.example.pickgo.utils.SessionManager
import kotlinx.coroutines.launch

class ManageItemsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityManageItemsBinding
    private lateinit var firebaseManager: FirebaseManager
    private lateinit var sessionManager: SessionManager
    private lateinit var itemAdapter: SellerItemAdapter
    private var sellerId: String = ""
    private var editingItemId: String? = null
    private var selectedImageUri: Uri? = null
    private var existingImageUrl: String? = null
    private var categories: List<String> = emptyList()

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedImageUri = result.data?.data
            selectedImageUri?.let { uri ->
                Glide.with(this).load(uri).into(binding.imagePreview)
                binding.imagePreview.visibility = View.VISIBLE
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageItemsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseManager = FirebaseManager()
        sessionManager = SessionManager(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Manage Products"

        setupRecyclerView()
        setupSpinners()
        loadCategories()
        loadSellerData()
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        itemAdapter = SellerItemAdapter(
            onEdit = { item ->
                editItem(item)
            },
            onDelete = { item ->
                confirmDelete(item)
            }
        )
        binding.productsRecycler.layoutManager = LinearLayoutManager(this)
        binding.productsRecycler.adapter = itemAdapter
    }

    private fun setupSpinners() {
        val statuses = listOf("available", "pending", "out_of_stock")
        val statusAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, statuses)
        (binding.statusSpinner as? android.widget.AutoCompleteTextView)?.setAdapter(statusAdapter)
    }

    private fun loadCategories() {
        lifecycleScope.launch {
            try {
                categories = firebaseManager.getCategoryNames()
                val categoryAdapter = ArrayAdapter(this@ManageItemsActivity, android.R.layout.simple_dropdown_item_1line, categories)
                (binding.categorySpinner as? android.widget.AutoCompleteTextView)?.setAdapter(categoryAdapter)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    private fun loadSellerData() {
        lifecycleScope.launch {
            showLoading(true)
            try {
                val user = firebaseManager.getCurrentUser()
                val seller = user?.let { firebaseManager.getSellerByEmail(it.email) }
                sellerId = seller?.id ?: return@launch

                loadItems()
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Error loading data: ${e.message}", Snackbar.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun loadItems() {
        lifecycleScope.launch {
            try {
                val items = firebaseManager.getSellerItemsAsSellerItems(sellerId)
                itemAdapter.submitList(items)

                if (items.isEmpty()) {
                    binding.emptyState.visibility = View.VISIBLE
                    binding.productsRecycler.visibility = View.GONE
                } else {
                    binding.emptyState.visibility = View.GONE
                    binding.productsRecycler.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Error loading items: ${e.message}", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupClickListeners() {
        binding.saveItemBtn.setOnClickListener {
            saveItem()
        }

        binding.cancelEditBtn.setOnClickListener {
            resetForm()
        }

        binding.selectImageBtn.setOnClickListener {
            openImagePicker()
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }

    private fun saveItem() {
        val itemName = binding.itemNameInput.text.toString().trim()
        val category = (binding.categorySpinner as? android.widget.AutoCompleteTextView)?.text.toString()
        val priceStr = binding.priceInput.text.toString().trim()
        val description = binding.descriptionInput.text.toString().trim()
        val status = (binding.statusSpinner as? android.widget.AutoCompleteTextView)?.text.toString()

        if (itemName.isEmpty()) {
            Snackbar.make(binding.root, "Please enter item name", Snackbar.LENGTH_SHORT).show()
            return
        }

        if (category.isEmpty()) {
            Snackbar.make(binding.root, "Please select a category", Snackbar.LENGTH_SHORT).show()
            return
        }

        val price = priceStr.toDoubleOrNull()
        if (price == null || price <= 0) {
            Snackbar.make(binding.root, "Please enter a valid price", Snackbar.LENGTH_SHORT).show()
            return
        }

        if (selectedImageUri == null && editingItemId == null) {
            Snackbar.make(binding.root, "Please select an image", Snackbar.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            showLoading(true)
            try {
                var imageUrl = existingImageUrl
                if (selectedImageUri != null) {
                    imageUrl = firebaseManager.uploadItemImage(sellerId, selectedImageUri!!)
                }

                val item = SellerItem(
                    id = editingItemId ?: "",
                    sellerId = sellerId,
                    itemName = itemName,
                    itemDescription = description,
                    itemPrice = price,
                    itemCategory = category,
                    itemImage = imageUrl,
                    itemStatus = status.ifEmpty { "available" }
                )

                if (editingItemId != null) {
                    firebaseManager.updateItem(editingItemId!!, mapOf(
                        "itemId" to item.itemId,
                        "sellerId" to item.sellerId,
                        "itemName" to item.itemName,
                        "itemDescription" to item.itemDescription,
                        "itemPrice" to item.itemPrice,
                        "itemCategory" to item.itemCategory,
                        "itemImage" to (item.itemImage ?: ""),
                        "itemStatus" to item.itemStatus
                    ))
                    Snackbar.make(binding.root, "Item updated successfully", Snackbar.LENGTH_SHORT).show()
                } else {
                    firebaseManager.addItemAsSellerItem(item)
                    Snackbar.make(binding.root, "Item added successfully", Snackbar.LENGTH_SHORT).show()
                }

                resetForm()
                loadItems()
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Error saving item: ${e.message}", Snackbar.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun editItem(item: SellerItem) {
        editingItemId = item.id
        existingImageUrl = item.itemImage

        binding.formTitle.text = "Edit Product"
        binding.itemNameInput.setText(item.itemName)
        (binding.categorySpinner as? android.widget.AutoCompleteTextView)?.setText(item.itemCategory, false)
        binding.priceInput.setText(item.itemPrice.toString())
        binding.descriptionInput.setText(item.itemDescription)
        (binding.statusSpinner as? android.widget.AutoCompleteTextView)?.setText(item.itemStatus, false)

        item.itemImage?.let { imageUrl ->
            Glide.with(this).load(imageUrl).into(binding.imagePreview)
            binding.imagePreview.visibility = View.VISIBLE
        }

        binding.cancelEditBtn.visibility = View.VISIBLE
        binding.saveItemBtn.text = "Update Product"

        // Scroll to form
        binding.formCard.requestFocus()
        // Scroll removed - nestedScrollView not available
    }

    private fun confirmDelete(item: SellerItem) {
        AlertDialog.Builder(this)
            .setTitle("Delete Item")
            .setMessage("Are you sure you want to delete '${item.itemName}'?")
            .setPositiveButton("Delete") { _, _ ->
                deleteItem(item)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteItem(item: SellerItem) {
        lifecycleScope.launch {
            showLoading(true)
            try {
                firebaseManager.deleteItem(item.id)
                Snackbar.make(binding.root, "Item deleted", Snackbar.LENGTH_SHORT).show()
                loadItems()
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Error deleting item: ${e.message}", Snackbar.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun resetForm() {
        editingItemId = null
        existingImageUrl = null
        selectedImageUri = null

        binding.formTitle.text = "Add New Product"
        binding.itemNameInput.text?.clear()
        (binding.categorySpinner as? android.widget.AutoCompleteTextView)?.text?.clear()
        binding.priceInput.text?.clear()
        binding.descriptionInput.text?.clear()
        (binding.statusSpinner as? android.widget.AutoCompleteTextView)?.setText("available", false)
        binding.imagePreview.visibility = View.GONE
        binding.cancelEditBtn.visibility = View.GONE
        binding.saveItemBtn.text = "Save Product"
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.saveItemBtn.isEnabled = !show
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}