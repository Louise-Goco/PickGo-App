package com.example.pickgo.activities.customer

import android.os.Bundle
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.example.pickgo.R
import com.example.pickgo.adapters.CategoryChipAdapter
import com.example.pickgo.adapters.ItemAdapter
import com.example.pickgo.databinding.ActivityBrowseItemsBinding
import com.example.pickgo.models.Category
import com.example.pickgo.models.Item
import com.example.pickgo.utils.CartManager
import com.example.pickgo.utils.FirebaseManager
import com.example.pickgo.utils.SessionManager
import kotlinx.coroutines.launch

class BrowseItemsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBrowseItemsBinding
    private lateinit var firebaseManager: FirebaseManager
    private lateinit var cartManager: CartManager
    private lateinit var sessionManager: SessionManager
    private lateinit var itemAdapter: ItemAdapter
    private lateinit var categoryAdapter: CategoryChipAdapter
    private var allItems: List<Item> = emptyList()
    private var categories: List<Category> = emptyList()
    private var currentCategory: String? = null
    private var currentQuery: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBrowseItemsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseManager = FirebaseManager()
        cartManager = CartManager(this)
        sessionManager = SessionManager(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Browse Items"

        setupRecyclerViews()
        setupSpinners()
        setupSearch()
        loadData()

        binding.searchButton.setOnClickListener {
            performSearch()
        }
    }

    private fun setupRecyclerViews() {
        itemAdapter = ItemAdapter { item ->
            addToCart(item)
        }

        categoryAdapter = CategoryChipAdapter { category ->
            currentCategory = category
            performSearch()
        }

        binding.itemsRecycler.layoutManager = GridLayoutManager(this, 2)
        binding.itemsRecycler.adapter = itemAdapter

        binding.categoryChipsRecycler.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this, androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false)
        binding.categoryChipsRecycler.adapter = categoryAdapter
    }

    private fun setupSpinners() {
        val cuisines = listOf("Any Cuisine", "Filipino", "American", "Italian", "Japanese", "Chinese", "Mexican")
        val cuisineAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, cuisines)
        (binding.cuisineSpinner as? android.widget.AutoCompleteTextView)?.setAdapter(cuisineAdapter)
    }

    private fun setupSearch() {
        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentQuery = s.toString()
                performSearch()
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun loadData() {
        lifecycleScope.launch {
            showLoading(true)
            try {
                allItems = firebaseManager.getAvailableItems()
                categories = firebaseManager.getCategories()

                categoryAdapter.submitList(categories)
                performSearch()
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Error loading items: ${e.message}", Snackbar.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun performSearch() {
        val filtered = allItems.filter { item ->
            var matches = true

            if (currentCategory != null && currentCategory!!.isNotEmpty()) {
                matches = matches && item.itemCategory == currentCategory
            }

            if (currentQuery.isNotEmpty()) {
                matches = matches && (item.itemName.contains(currentQuery, ignoreCase = true) ||
                        item.itemDescription.contains(currentQuery, ignoreCase = true))
            }

            matches
        }

        itemAdapter.submitList(filtered)

        if (filtered.isEmpty()) {
            binding.emptyState.visibility = View.VISIBLE
            binding.itemsRecycler.visibility = View.GONE
        } else {
            binding.emptyState.visibility = View.GONE
            binding.itemsRecycler.visibility = View.VISIBLE
        }
    }

    private fun addToCart(item: Item) {
        val session = sessionManager.getSession()
        if (session == null) {
            Snackbar.make(binding.root, "Please login to add items to cart", Snackbar.LENGTH_LONG)
                .setAction("Login") {
                    // Navigate to login
                }.show()
            return
        }

        val cartItem = com.example.pickgo.models.CartItem(
            itemId = item.id,
            itemName = item.itemName,
            itemPrice = item.itemPrice,
            itemImage = item.itemImage,
            merchantName = item.merchantName,
            merchantId = item.merchantId
        )
        cartManager.addItem(cartItem)
        Snackbar.make(binding.root, "${item.itemName} added to cart", Snackbar.LENGTH_SHORT).show()
        invalidateOptionsMenu()
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu?): Boolean {
        menuInflater.inflate(R.menu.customer_menu, menu)
        val itemCount = cartManager.getItemCount()
        if (itemCount > 0) {
            menu?.findItem(R.id.action_cart)?.title = "Cart ($itemCount)"
        }
        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_cart -> {
                startActivity(android.content.Intent(this, CartActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}