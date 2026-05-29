package com.example.pickgo.activities.admin


import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.example.pickgo.R
import com.example.pickgo.adapters.admin.ProductAdapter
import com.example.pickgo.databinding.ActivityManageProductsBinding
import com.example.pickgo.models.admin.AdminProduct
import com.example.pickgo.utils.FirebaseManager
import com.example.pickgo.utils.PriceFormatter
import com.example.pickgo.utils.SessionManager
import kotlinx.coroutines.launch

class ManageProductsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityManageProductsBinding
    private lateinit var firebaseManager: FirebaseManager
    private lateinit var sessionManager: SessionManager
    private lateinit var productAdapter: ProductAdapter
    private var allProducts: List<AdminProduct> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageProductsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseManager = FirebaseManager(this)
        sessionManager = SessionManager(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Product Management"

        setupRecyclerView()
        loadProducts()
    }

    private fun setupRecyclerView() {
        productAdapter = ProductAdapter(
            onApprove = { product ->
                approveProduct(product)
            },
            onReject = { product ->
                rejectProduct(product)
            },
            onView = { product ->
                viewProductDetails(product)
            }
        )
        binding.productsRecycler.layoutManager = LinearLayoutManager(this)
        binding.productsRecycler.adapter = productAdapter
    }

    private fun loadProducts() {
        lifecycleScope.launch {
            showLoading(true)
            try {
                allProducts = firebaseManager.getAllProducts()
                // Show pending products first
                val sorted = allProducts.sortedByDescending {
                    when (it.itemStatus) {
                        "pending" -> 3
                        "available" -> 2
                        else -> 1
                    }
                }
                productAdapter.submitList(sorted)

                if (allProducts.isEmpty()) {
                    binding.emptyState.visibility = View.VISIBLE
                    binding.productsRecycler.visibility = View.GONE
                } else {
                    binding.emptyState.visibility = View.GONE
                    binding.productsRecycler.visibility = View.VISIBLE
                }

                updatePendingCount()
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Error loading products: ${e.message}", Snackbar.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun updatePendingCount() {
        val pendingCount = allProducts.count { it.itemStatus == "pending" }
        if (pendingCount > 0) {
            binding.pendingBadge.text = pendingCount.toString()
            binding.pendingBadge.visibility = View.VISIBLE
        } else {
            binding.pendingBadge.visibility = View.GONE
        }
    }

    private fun approveProduct(product: AdminProduct) {
        AlertDialog.Builder(this)
            .setTitle("Approve Product")
            .setMessage("Approve '${product.itemName}' for sale on the platform?")
            .setPositiveButton("Approve") { _, _ ->
                updateProductStatus(product, "available")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun rejectProduct(product: AdminProduct) {
        val input = android.widget.EditText(this).apply {
            hint = "Reason for rejection (optional)"
            inputType = android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
            setSingleLine(false)
        }

        AlertDialog.Builder(this)
            .setTitle("Reject Product")
            .setMessage("Reject '${product.itemName}'?")
            .setView(input)
            .setPositiveButton("Reject") { _, _ ->
                val reason = input.text.toString().trim()
                updateProductStatus(product, "rejected", reason)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateProductStatus(product: AdminProduct, status: String, reason: String = "") {
        lifecycleScope.launch {
            showLoading(true)
            try {
                firebaseManager.updateProductStatus(product.id, status)
                val message = when (status) {
                    "available" -> "Product approved and now available for sale"
                    "rejected" -> "Product rejected${if (reason.isNotEmpty()) ": $reason" else ""}"
                    else -> "Product updated"
                }
                Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
                loadProducts()
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun viewProductDetails(product: AdminProduct) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_product_details, null)

        val productImage = dialogView.findViewById<com.google.android.material.imageview.ShapeableImageView>(R.id.productImage)
        val productName = dialogView.findViewById<android.widget.TextView>(R.id.productName)
        val productPrice = dialogView.findViewById<android.widget.TextView>(R.id.productPrice)
        val merchantName = dialogView.findViewById<android.widget.TextView>(R.id.merchantName)
        val categoryName = dialogView.findViewById<android.widget.TextView>(R.id.categoryName)
        val productDesc = dialogView.findViewById<android.widget.TextView>(R.id.productDescription)
        val statusBadge = dialogView.findViewById<android.widget.TextView>(R.id.statusBadge)

        productName.text = product.itemName
        productPrice.text = PriceFormatter.format(product.itemPrice)
        merchantName.text = product.merchantName
        categoryName.text = product.categoryName
        productDesc.text = product.itemDescription.ifEmpty { "No description provided" }

        when (product.itemStatus) {
            "pending" -> statusBadge.setTextColor(getColor(R.color.warning_text))
            "available" -> statusBadge.setTextColor(getColor(R.color.success_text))
            "rejected" -> statusBadge.setTextColor(getColor(R.color.error_text))
        }
        statusBadge.text = product.itemStatus.uppercase()

        Glide.with(this)
            .load(product.itemImage)
            .placeholder(R.drawable.placeholder_food)
            .error(R.drawable.placeholder_food)
            .into(productImage)

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Close", null)
            .show()
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}