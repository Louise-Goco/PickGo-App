package com.example.pickgo.activities.admin

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.example.pickgo.R
import com.example.pickgo.activities.LoginActivity
import com.example.pickgo.adapters.admin.AdminSectionAdapter
import com.example.pickgo.adapters.admin.StatCardAdapter
import com.example.pickgo.databinding.ActivityAdminDashboardBinding
import com.example.pickgo.models.admin.AdminSeller
import com.example.pickgo.models.admin.AdminRider
import com.example.pickgo.models.admin.AdminProduct
import com.example.pickgo.utils.FirebaseManager
import com.example.pickgo.utils.PriceFormatter
import com.example.pickgo.utils.SessionManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AdminDashboardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAdminDashboardBinding
    private lateinit var firebaseManager: FirebaseManager
    private lateinit var sessionManager: SessionManager
    private lateinit var statAdapter: StatCardAdapter
    private lateinit var sectionAdapter: AdminSectionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseManager = FirebaseManager(this)
        sessionManager = SessionManager(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_menu)

        setupDrawer()
        setupRecyclerViews()
        loadAdminData()
        loadDashboardStats()
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
                R.id.nav_customers -> {
                    startActivity(Intent(this, ManageCustomersActivity::class.java))
                    binding.drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_sellers -> {
                    startActivity(Intent(this, ManageSellersActivity::class.java))
                    binding.drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_riders -> {
                    startActivity(Intent(this, ManageRidersActivity::class.java))
                    binding.drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_products -> {
                    startActivity(Intent(this, ManageProductsActivity::class.java))
                    binding.drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_orders -> {
                    startActivity(Intent(this, ManageOrdersActivity::class.java))
                    binding.drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_categories -> {
                    startActivity(Intent(this, ManageCategoriesActivity::class.java))
                    binding.drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_payouts -> {
                    startActivity(Intent(this, ManagePayoutsActivity::class.java))
                    binding.drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, SystemSettingsActivity::class.java))
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
        statAdapter = StatCardAdapter()
        binding.statsRecycler.layoutManager = GridLayoutManager(this, 2)
        binding.statsRecycler.adapter = statAdapter

        val sections = listOf(
            AdminSection("Customer Management", "View, edit, and manage all customer accounts", R.drawable.ic_users) {
                startActivity(Intent(this, ManageCustomersActivity::class.java))
            },
            AdminSection("Seller Management", "Approve new seller applications and manage merchants", R.drawable.ic_store) {
                startActivity(Intent(this, ManageSellersActivity::class.java))
            },
            AdminSection("Rider Management", "Verify rider documents and monitor performance", R.drawable.ic_rider) {
                startActivity(Intent(this, ManageRidersActivity::class.java))
            },
            AdminSection("Product Management", "Review and moderate product submissions", R.drawable.ic_products) {
                startActivity(Intent(this, ManageProductsActivity::class.java))
            },
            AdminSection("Order Monitoring", "Track all orders across the platform", R.drawable.ic_orders) {
                startActivity(Intent(this, ManageOrdersActivity::class.java))
            },
            AdminSection("Category Management", "Organize food and store types", R.drawable.ic_category) {
                startActivity(Intent(this, ManageCategoriesActivity::class.java))
            },
            AdminSection("Payout Management", "Handle financial requests and disbursements", R.drawable.ic_wallet) {
                startActivity(Intent(this, ManagePayoutsActivity::class.java))
            },
            AdminSection("System Settings", "Configure platform-wide settings", R.drawable.ic_settings) {
                startActivity(Intent(this, SystemSettingsActivity::class.java))
            }
        )

        sectionAdapter = AdminSectionAdapter(sections)
        binding.adminSectionsRecycler.layoutManager = GridLayoutManager(this, 2)
        binding.adminSectionsRecycler.adapter = sectionAdapter
    }

    private fun loadAdminData() {
        lifecycleScope.launch {
            try {
                val user = firebaseManager.getCurrentUser()
                val adminName = user?.firstName ?: "Admin"
                binding.welcomeText.text = "Welcome back, $adminName"
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    private fun loadDashboardStats() {
        lifecycleScope.launch {
            showLoading(true)
            try {
                val users = firebaseManager.getAllUsers()
                val sellers = firebaseManager.getAllSellers()
                val riders = firebaseManager.getAllRiders()
                val products = firebaseManager.getAllProducts()
                val orders = firebaseManager.getAllOrdersWithDetails()

                val totalUsers = users.size + sellers.size + riders.size
                val pendingApprovals = users.count { !it.isVerified } +
                        sellers.count { it.sellerStatus == "pending" } +
                        riders.count { it.riderStatus == "pending" } +
                        products.count { it.itemStatus == "pending" }
                val activeSellers = sellers.count { it.sellerStatus == "active" }
                val activeRiders = riders.count { it.riderStatus == "active" }

                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val todayRevenue = orders.filter {
                    it.orderStatus == "delivered" && it.orderDate.startsWith(today)
                }.sumOf { it.orderTotal }

                val stats = listOf(
                    StatCard("Total Users", totalUsers.toString(), R.drawable.ic_users),
                    StatCard("Pending Approvals", pendingApprovals.toString(), R.drawable.ic_pending),
                    StatCard("Active Sellers", activeSellers.toString(), R.drawable.ic_store),
                    StatCard("Active Riders", activeRiders.toString(), R.drawable.ic_rider),
                    StatCard("Today's Revenue", PriceFormatter.format(todayRevenue), R.drawable.ic_money)
                )

                statAdapter.submitList(stats)

                // Show pending alerts
                showPendingAlerts(sellers, riders, products)

            } catch (e: Exception) {
                Snackbar.make(binding.root, "Error loading stats: ${e.message}", Snackbar.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun showPendingAlerts(sellers: List<AdminSeller>, riders: List<AdminRider>, products: List<AdminProduct>) {
        binding.alertContainer.removeAllViews()

        val pendingSellers = sellers.filter { it.sellerStatus == "pending" }
        val pendingRiders = riders.filter { it.riderStatus == "pending" }
        val pendingProducts = products.filter { it.itemStatus == "pending" }

        if (pendingSellers.isNotEmpty() || pendingRiders.isNotEmpty() || pendingProducts.isNotEmpty()) {
            val alertView = layoutInflater.inflate(R.layout.item_alert, null)
            val alertText = alertView.findViewById<TextView>(R.id.alertText)
            val alertSubtext = alertView.findViewById<TextView>(R.id.alertSubtext)

            val alerts = mutableListOf<String>()
            if (pendingSellers.isNotEmpty()) alerts.add("${pendingSellers.size} new seller application(s)")
            if (pendingRiders.isNotEmpty()) alerts.add("${pendingRiders.size} new rider application(s)")
            if (pendingProducts.isNotEmpty()) alerts.add("${pendingProducts.size} new product submission(s)")

            alertText.text = alerts.joinToString(", ")
            alertSubtext.text = "Requires your attention"

            binding.alertContainer.addView(alertView)
        }
    }

    private fun logout() {
        lifecycleScope.launch {
            firebaseManager.logout()
            sessionManager.clearSession()
            val intent = Intent(this@AdminDashboardActivity, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
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
    data class AdminSection(val title: String, val description: String, val iconRes: Int, val onClick: () -> Unit)
}