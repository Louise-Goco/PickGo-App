package com.example.pickgo.activities.customer

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import com.example.pickgo.activities.LoginActivity
import com.example.pickgo.activities.RiderRegisterActivity
import com.example.pickgo.activities.SellerRegisterActivity
import com.example.pickgo.R
import com.example.pickgo.adapters.ItemAdapter
import com.example.pickgo.adapters.OrderAdapter
import com.example.pickgo.adapters.StoreAdapter
import com.example.pickgo.databinding.ActivityCustomerDashboardBinding
import com.example.pickgo.models.Item
import com.example.pickgo.models.Merchant
import com.example.pickgo.models.Order
import com.example.pickgo.models.User
import com.example.pickgo.utils.CartManager
import com.example.pickgo.utils.FirebaseManager
import com.example.pickgo.utils.SessionManager
import kotlinx.coroutines.launch

class CustomerDashboardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCustomerDashboardBinding
    private lateinit var firebaseManager: FirebaseManager
    private lateinit var sessionManager: SessionManager
    private lateinit var cartManager: CartManager
    private lateinit var itemAdapter: ItemAdapter
    private lateinit var storeAdapter: StoreAdapter
    private lateinit var orderAdapter: OrderAdapter
    private var currentUser: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCustomerDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseManager = FirebaseManager()
        sessionManager = SessionManager(this)
        cartManager = CartManager(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(true)
        supportActionBar?.title = "PickGo"

        setupBottomNavigation()
        setupRecyclerViews()
        loadUserData()
        loadDashboardData()

        if (intent.getBooleanExtra("order_success", false)) {
            binding.successBanner.visibility = View.VISIBLE
            binding.successBanner.postDelayed({
                binding.successBanner.visibility = View.GONE
            }, 5000)
        }

        setupClickListeners()
        updateCartBadge()
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.selectedItemId = R.id.nav_home
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_browse -> {
                    startActivity(Intent(this, BrowseItemsActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_cart -> {
                    startActivity(Intent(this, CartActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_orders -> {
                    startActivity(Intent(this, MyOrdersActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                else -> false
            }
        }
    }

    private fun setupRecyclerViews() {
        itemAdapter = ItemAdapter { item ->
            val intent = Intent(this, ViewStoreActivity::class.java)
            intent.putExtra("merchant_id", item.merchantId)
            intent.putExtra("merchant_name", item.merchantName)
            startActivity(intent)
        }

        storeAdapter = StoreAdapter { store ->
            val intent = Intent(this, ViewStoreActivity::class.java)
            intent.putExtra("merchant_id", store.id)
            intent.putExtra("merchant_name", store.merchName)
            startActivity(intent)
        }

        orderAdapter = OrderAdapter { order ->
            val intent = Intent(this, TrackOrderActivity::class.java)
            intent.putExtra("order_id", order.orderId)
            startActivity(intent)
        }

        binding.featuredItemsRecycler.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.featuredItemsRecycler.adapter = itemAdapter

        binding.featuredStoresRecycler.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.featuredStoresRecycler.adapter = storeAdapter

        binding.recentOrdersRecycler.layoutManager = LinearLayoutManager(this)
        binding.recentOrdersRecycler.adapter = orderAdapter
    }

    private fun setupClickListeners() {
        binding.browseItemsCard.setOnClickListener {
            startActivity(Intent(this, BrowseItemsActivity::class.java))
        }

        binding.browseStoresCard.setOnClickListener {
            startActivity(Intent(this, BrowseStoresActivity::class.java))
        }

        binding.myOrdersCard.setOnClickListener {
            startActivity(Intent(this, MyOrdersActivity::class.java))
        }

        binding.seeAllFoods.setOnClickListener {
            startActivity(Intent(this, BrowseItemsActivity::class.java))
        }

        binding.seeAllStores.setOnClickListener {
            startActivity(Intent(this, BrowseStoresActivity::class.java))
        }

        binding.viewAllOrders.setOnClickListener {
            startActivity(Intent(this, MyOrdersActivity::class.java))
        }

        binding.becomeSellerCard.setOnClickListener {
            startActivity(Intent(this, SellerRegisterActivity::class.java))
        }

        binding.driveWithUsCard.setOnClickListener {
            startActivity(Intent(this, RiderRegisterActivity::class.java))
        }
    }

    private fun loadUserData() {
        lifecycleScope.launch {
            currentUser = firebaseManager.getCurrentUser()
            currentUser?.let { user ->
                val welcomeName = user.displayName?.takeIf { it.isNotEmpty() } ?: user.firstName
                binding.welcomeTitle.text = "Welcome, ${welcomeName ?: "there"}!"
                binding.welcomeSubtitle.text = "What would you like to order today?"
            }
        }
    }

    private fun loadDashboardData() {
        lifecycleScope.launch {
            showLoading(true)

            try {
                val items = firebaseManager.getFeaturedItems()
                itemAdapter.submitList(items.take(4))

                val stores = firebaseManager.getActiveMerchants()
                storeAdapter.submitList(stores.take(5))

                currentUser?.let { user ->
                    val orders = firebaseManager.getCustomerOrders(user.id)
                    orderAdapter.submitList(orders.take(3))
                }
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Error loading data: ${e.message}", Snackbar.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun updateCartBadge() {
        val itemCount = cartManager.getItemCount()
        if (itemCount > 0) {
            supportActionBar?.let { actionBar ->
                // Update cart badge in menu when menu is created
                invalidateOptionsMenu()
            }
        }
    }

    private fun showLoading(show: Boolean) {
        // progressBar not available in layout
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.customer_menu, menu)
        val cartItem = menu?.findItem(R.id.action_cart)
        val itemCount = cartManager.getItemCount()
        if (itemCount > 0) {
            cartItem?.title = "Cart ($itemCount)"
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_cart -> {
                startActivity(Intent(this, CartActivity::class.java))
                true
            }
            R.id.action_logout -> {
                lifecycleScope.launch {
                    firebaseManager.logout()
                    sessionManager.clearSession()
                    val intent = Intent(this@CustomerDashboardActivity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finishAffinity()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        loadDashboardData()
        updateCartBadge()
    }
}