package com.example.pickgo.activities.seller

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.example.pickgo.R
import com.example.pickgo.activities.customer.CustomerDashboardActivity
import com.example.pickgo.adapters.seller.OrderAdapter
import com.example.pickgo.adapters.seller.StatCardAdapter
import com.example.pickgo.adapters.seller.StatCardItem
import com.example.pickgo.databinding.ActivitySellerDashboardBinding
import com.example.pickgo.models.Merchant
import com.example.pickgo.models.seller.Seller
import com.example.pickgo.models.seller.SellerOrder
import com.example.pickgo.utils.FirebaseManager
import com.example.pickgo.utils.PriceFormatter
import com.example.pickgo.utils.SessionManager
import kotlinx.coroutines.launch

class SellerDashboardActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySellerDashboardBinding
    private lateinit var firebaseManager: FirebaseManager
    private lateinit var sessionManager: SessionManager
    private lateinit var orderAdapter: OrderAdapter
    private lateinit var statAdapter: StatCardAdapter
    private var currentSeller: Seller? = null
    private var currentMerchant: Merchant? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySellerDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseManager = FirebaseManager()
        sessionManager = SessionManager(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_menu)

        setupDrawer()
        setupRecyclerViews()
        loadSellerData()
        setupClickListeners()
    }

    private fun setupDrawer() {
        val toggle = ActionBarDrawerToggle(
            this, binding.drawerLayout, binding.toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        binding.navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_dashboard -> {
                    binding.drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_analytics -> {
                    startActivity(Intent(this, SellerAnalyticsActivity::class.java))
                    binding.drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_products -> {
                    startActivity(Intent(this, ManageItemsActivity::class.java))
                    binding.drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_orders -> {
                    startActivity(Intent(this, ManageOrdersActivity::class.java))
                    binding.drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_payouts -> {
                    startActivity(Intent(this, SellerPayoutsActivity::class.java))
                    binding.drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_reviews -> {
                    startActivity(Intent(this, SellerReviewsActivity::class.java))
                    binding.drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, StoreProfileActivity::class.java))
                    binding.drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_logout -> {
                    logout()
                    true
                }
                else -> false
            }
            true
        }
    }

    private fun setupRecyclerViews() {
        orderAdapter = OrderAdapter { order ->
            val intent = Intent(this, ManageOrdersActivity::class.java)
            intent.putExtra("selected_order_id", order.id)
            startActivity(intent)
        }

        statAdapter = StatCardAdapter()

        binding.recentOrdersRecycler.layoutManager = LinearLayoutManager(this)
        binding.recentOrdersRecycler.adapter = orderAdapter

        binding.statsRecycler.layoutManager = GridLayoutManager(this, 2)
        binding.statsRecycler.adapter = statAdapter
    }

    private fun setupClickListeners() {
        binding.switchToCustomerBtn.setOnClickListener {
            startActivity(Intent(this, CustomerDashboardActivity::class.java))
            finish()
        }
    }

    private fun loadSellerData() {
        lifecycleScope.launch {
            showLoading(true)
            try {
                val user = firebaseManager.getCurrentUser()
                user?.let {
                    currentSeller = firebaseManager.getSellerByEmail(it.email)
                    currentSeller?.let { seller ->
                        currentMerchant = firebaseManager.getMerchantById(seller.merchantId)
                        updateUI()
                        loadStats()
                        loadRecentOrders()
                    }
                }
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Error loading data: ${e.message}", Snackbar.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun updateUI() {
        currentMerchant?.let { merchant ->
            binding.storeNameText.text = merchant.merchName
            supportActionBar?.title = merchant.merchName
        }

        currentSeller?.let { seller ->
            binding.welcomeText.text = "Welcome back, ${seller.firstName}!"
        }

        binding.storeStatusText.text = "Your store is currently active and visible to customers"
    }

    private fun loadStats() {
        lifecycleScope.launch {
            try {
                val sellerId = currentSeller?.id ?: return@launch
                val orders = firebaseManager.getOrderItems(sellerId)
                val items = firebaseManager.getSellerItemsAsSellerItems(sellerId)
                val payouts = firebaseManager.getSellerPayouts(sellerId)

                val todaysOrders = orders.filter { isToday(it.orderDate) }.size
                val totalRevenue = orders.filter { it.orderStatus == "delivered" }
                    .sumOf { it.orderTotal }
                val activeListings = items.filter { it.itemStatus == "available" }.size
                val pendingPayouts = payouts.filter { it.payoutStatus == "pending" }
                    .sumOf { it.amount }
                val storeRating = currentSeller?.sellerRating ?: 0.0

                val stats = listOf(
                    StatCardItem("Today's Orders", todaysOrders.toString(), R.drawable.ic_orders),
                    StatCardItem("Total Revenue", PriceFormatter.format(totalRevenue), R.drawable.ic_wallet),
                    StatCardItem("Active Listings", activeListings.toString(), R.drawable.ic_products),
                    StatCardItem("Pending Payouts", PriceFormatter.format(pendingPayouts), R.drawable.ic_payment),
                    StatCardItem("Store Rating", String.format("%.1f ★", storeRating), R.drawable.ic_star)
                )

                statAdapter.submitList(stats)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    private fun loadRecentOrders() {
        lifecycleScope.launch {
            try {
                val sellerId = currentSeller?.id ?: return@launch
                val orders = firebaseManager.getOrderItems(sellerId)
                val recentOrders = orders.sortedByDescending { it.orderDate }.take(5)

                orderAdapter.submitList(recentOrders)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    private fun isToday(dateString: String): Boolean {
        return try {
            val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
            dateString.startsWith(today)
        } catch (e: Exception) {
            false
        }
    }

    private fun logout() {
        lifecycleScope.launch {
            firebaseManager.logout()
            sessionManager.clearSession()
            finishAffinity()
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            binding.drawerLayout.openDrawer(androidx.core.view.GravityCompat.START)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    data class StatCard(val title: String, val value: String, val iconRes: Int)
}