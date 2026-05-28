package com.example.pickgo.activities.seller


import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.example.pickgo.R
import com.example.pickgo.adapters.seller.SellerOrderAdapter
import com.example.pickgo.databinding.ActivityManageOrdersBinding
import com.example.pickgo.models.seller.SellerOrder
import com.example.pickgo.utils.FirebaseManager
import com.example.pickgo.utils.SessionManager
import kotlinx.coroutines.launch

class ManageOrdersActivity : AppCompatActivity() {
    private lateinit var binding: ActivityManageOrdersBinding
    private lateinit var firebaseManager: FirebaseManager
    private lateinit var sessionManager: SessionManager
    private lateinit var orderAdapter: SellerOrderAdapter
    private var sellerId: String = ""
    private var selectedOrderId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageOrdersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseManager = FirebaseManager()
        sessionManager = SessionManager(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Manage Orders"

        selectedOrderId = intent.getStringExtra("selected_order_id")

        setupRecyclerView()
        loadSellerData()
    }

    private fun setupRecyclerView() {
        orderAdapter = SellerOrderAdapter(
            onConfirm = { order ->
                confirmOrder(order)
            },
            onReject = { order ->
                rejectOrder(order)
            },
            onDispatch = { order ->
                dispatchOrder(order)
            }
        )
        binding.ordersRecycler.layoutManager = LinearLayoutManager(this)
        binding.ordersRecycler.adapter = orderAdapter
    }

    private fun loadSellerData() {
        lifecycleScope.launch {
            showLoading(true)
            try {
                val user = firebaseManager.getCurrentUser()
                val seller = user?.let { firebaseManager.getSellerByEmail(it.email) }
                sellerId = seller?.id ?: return@launch

                loadOrders()
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Error loading data: ${e.message}", Snackbar.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun loadOrders() {
        lifecycleScope.launch {
            try {
                val orders = firebaseManager.getOrderItems(sellerId)

                // Enrich orders with customer names and items
                val enrichedOrders = orders.map { order ->
                    val customer = firebaseManager.getUserById(order.customerId)
                    order.copy(
                        customerName = "${customer?.firstName ?: ""} ${customer?.lastName ?: ""}".trim()
                    )
                }

                val sortedOrders = enrichedOrders.sortedByDescending { it.orderDate }
                orderAdapter.submitList(sortedOrders)

                // Scroll to selected order if specified
                selectedOrderId?.let { orderId ->
                    val position = sortedOrders.indexOfFirst { it.id == orderId }
                    if (position >= 0) {
                        binding.ordersRecycler.smoothScrollToPosition(position)
                    }
                }

                if (sortedOrders.isEmpty()) {
                    binding.emptyState.visibility = View.VISIBLE
                    binding.ordersRecycler.visibility = View.GONE
                } else {
                    binding.emptyState.visibility = View.GONE
                    binding.ordersRecycler.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Error loading orders: ${e.message}", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun confirmOrder(order: SellerOrder) {
        AlertDialog.Builder(this)
            .setTitle("Confirm Order")
            .setMessage("Confirm this order and start preparing?")
            .setPositiveButton("Confirm") { _, _ ->
                updateOrderStatus(order, "preparing")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun rejectOrder(order: SellerOrder) {
        AlertDialog.Builder(this)
            .setTitle("Reject Order")
            .setMessage("Are you sure you want to reject this order?")
            .setPositiveButton("Reject") { _, _ ->
                updateOrderStatus(order, "cancelled")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun dispatchOrder(order: SellerOrder) {
        AlertDialog.Builder(this)
            .setTitle("Ready for Dispatch")
            .setMessage("Mark this order as ready for pickup? The rider will be notified.")
            .setPositiveButton("Yes, Ready") { _, _ ->
                updateOrderStatus(order, "ready_for_pickup")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateOrderStatus(order: SellerOrder, newStatus: String) {
        lifecycleScope.launch {
            showLoading(true)
            try {
                firebaseManager.updateOrderStatus(order.id, newStatus)
                Snackbar.make(binding.root, "Order #${order.orderId} ${
                    when (newStatus) {
                        "preparing" -> "confirmed and preparing"
                        "cancelled" -> "cancelled"
                        "ready_for_pickup" -> "marked as ready for pickup"
                        else -> "updated"
                    }
                }", Snackbar.LENGTH_SHORT).show()
                loadOrders()
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Error updating order: ${e.message}", Snackbar.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
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