package com.example.pickgo.activities.customer

import android.content.Intent
import android.os.Bundle
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.example.pickgo.adapters.StoreAdapter
import com.example.pickgo.databinding.ActivityBrowseStoresBinding
import com.example.pickgo.models.Merchant
import com.example.pickgo.utils.FirebaseManager
import kotlinx.coroutines.launch

class BrowseStoresActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBrowseStoresBinding
    private lateinit var firebaseManager: FirebaseManager
    private lateinit var storeAdapter: StoreAdapter
    private var allStores: List<Merchant> = emptyList()
    private var currentType: String = ""
    private var currentQuery: String = ""
    private var currentSort: String = "recommended"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBrowseStoresBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseManager = FirebaseManager(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Browse Stores"

        setupRecyclerView()
        setupSpinners()
        setupSearch()
        loadData()

        binding.searchButton.setOnClickListener {
            performSearchAndFilter()
        }
    }

    private fun setupRecyclerView() {
        storeAdapter = StoreAdapter { store ->
            val intent = Intent(this, ViewStoreActivity::class.java)
            intent.putExtra("merchant_id", store.id)
            intent.putExtra("merchant_name", store.merchName)
            startActivity(intent)
        }

        binding.storesRecycler.layoutManager = GridLayoutManager(this, 2)
        binding.storesRecycler.adapter = storeAdapter
    }

    private fun setupSpinners() {
        val types = listOf("Any Type", "Restaurant", "Fast Food", "Cafe / Coffee Shop", "Bakery", "Market / Groceries")
        val typeAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, types)
        (binding.typeSpinner as? android.widget.AutoCompleteTextView)?.setAdapter(typeAdapter)

        val sorts = listOf("Recommended", "Highest Rated", "Fastest Delivery", "Nearest")
        val sortAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, sorts)
        (binding.sortSpinner as? android.widget.AutoCompleteTextView)?.setAdapter(sortAdapter)

        (binding.typeSpinner as? android.widget.AutoCompleteTextView)?.setOnItemClickListener { _, _, position, _ ->
            currentType = if (position == 0) "" else types[position]
            performSearchAndFilter()
        }

        (binding.sortSpinner as? android.widget.AutoCompleteTextView)?.setOnItemClickListener { _, _, position, _ ->
            currentSort = when (position) {
                0 -> "recommended"
                1 -> "rating"
                2 -> "delivery_time"
                else -> "distance"
            }
            performSearchAndFilter()
        }
    }

    private fun setupSearch() {
        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentQuery = s.toString()
                performSearchAndFilter()
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun loadData() {
        lifecycleScope.launch {
            showLoading(true)
            try {
                allStores = firebaseManager.getActiveMerchants()
                performSearchAndFilter()
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Error loading stores: ${e.message}", Snackbar.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun performSearchAndFilter() {
        var filtered = allStores

        if (currentType.isNotEmpty()) {
            filtered = filtered.filter { it.merchType.equals(currentType, ignoreCase = true) }
        }

        if (currentQuery.isNotEmpty()) {
            filtered = filtered.filter {
                it.merchName.contains(currentQuery, ignoreCase = true) ||
                        (it.merchDescription?.contains(currentQuery, ignoreCase = true) ?: false)
            }
        }

        when (currentSort) {
            "rating" -> filtered = filtered.sortedByDescending { it.rating }
            else -> filtered = filtered.shuffled()
        }

        storeAdapter.submitList(filtered)

        if (filtered.isEmpty()) {
            binding.emptyState.visibility = View.VISIBLE
            binding.storesRecycler.visibility = View.GONE
        } else {
            binding.emptyState.visibility = View.GONE
            binding.storesRecycler.visibility = View.VISIBLE
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}