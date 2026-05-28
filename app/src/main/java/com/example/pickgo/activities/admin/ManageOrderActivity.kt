package com.example.pickgo.activities.admin

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.example.pickgo.R
import com.example.pickgo.adapters.admin.OrderAdapter
import com.example.pickgo.databinding.ActivityManageOrdersBinding
import com.example.pickgo.models.admin.AdminOrder
import com.example.pickgo.utils.FirebaseManager
import com.example.pickgo.utils.SessionManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ManageOrdersActivity : AppCompatActivity() {
    private lateinit var binding: ActivityManageOrdersBinding
    private lateinit var firebaseManager: FirebaseManager
    private lateinit var sessionManager: SessionManager
    private lateinit var orderAdapter: OrderAdapter
    private var allOrders: List<AdminOrder> = emptyList()
    private var filteredOrders: List<AdminOrder> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageOrdersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseManager = FirebaseManager()
        sessionManager = SessionManager(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Order Monitoring"

        setupRecyclerView()
        setupFilters()
        loadOrders()
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        orderAdapter = OrderAdapter(
            onStatusChange = { order, newStatus ->
                updateOrderStatus(order, newStatus)
            },
            onViewDetails = { order ->
                viewOrderDetails(order)
            }
        )
        binding.ordersRecycler.layoutManager = LinearLayoutManager(this)
        binding.ordersRecycler.adapter = orderAdapter
    }

    private fun setupFilters() {
        // Setup status filter spinner
        val statuses = listOf("All Statuses", "pending", "preparing", "ready_for_pickup", "on_the_way", "delivered", "cancelled")
        val statusAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, statuses)
        binding.statusSpinner.adapter = statusAdapter
    }

    private fun setupClickListeners() {
        binding.applyFiltersBtn.setOnClickListener {
            applyFilters()
        }

        binding.resetFiltersBtn.setOnClickListener {
            resetFilters()
        }

        binding.searchInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applyFilters()
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun loadOrders() {
        lifecycleScope.launch {
            showLoading(true)
            try {
                allOrders = firebaseManager.getAllOrdersWithDetails()
                allOrders = allOrders.sortedByDescending { it.orderDate }
                filteredOrders = allOrders
                orderAdapter.submitList(filteredOrders)

                if (filteredOrders.isEmpty()) {
                    binding.emptyState.visibility = View.VISIBLE
                    binding.ordersRecycler.visibility = View.GONE
                } else {
                    binding.emptyState.visibility = View.GONE
                    binding.ordersRecycler.visibility = View.VISIBLE
                }

                updateStats()
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Error loading orders: ${e.message}", Snackbar.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun applyFilters() {
        val searchQuery = binding.searchInput.text.toString().trim().lowercase()
        val status = binding.statusSpinner.selectedItem.toString()

        filteredOrders = allOrders.filter { order ->
            var matches = true

            if (searchQuery.isNotEmpty()) {
                matches = matches && (
                        order.orderId.lowercase().contains(searchQuery) ||
                                order.customerEmail.lowercase().contains(searchQuery) ||
                                order.merchantName.lowercase().contains(searchQuery)
                        )
            }

            if (status != "All Statuses") {
                matches = matches && order.orderStatus == status
            }

            matches
        }

        orderAdapter.submitList(filteredOrders)

        if (filteredOrders.isEmpty()) {
            binding.emptyState.visibility = View.VISIBLE
            binding.ordersRecycler.visibility = View.GONE
        } else {
            binding.emptyState.visibility = View.GONE
            binding.ordersRecycler.visibility = View.VISIBLE
        }
    }

    private fun resetFilters() {
        binding.searchInput.text?.clear()
        binding.statusSpinner.setSelection(0)
        filteredOrders = allOrders
        orderAdapter.submitList(filteredOrders)

        if (filteredOrders.isEmpty()) {
            binding.emptyState.visibility = View.VISIBLE
            binding.ordersRecycler.visibility = View.GONE
        } else {
            binding.emptyState.visibility = View.GONE
            binding.ordersRecycler.visibility = View.VISIBLE
        }
    }

    private fun updateStats() {
        val totalOrders = allOrders.size
        val pendingOrders = allOrders.count { it.orderStatus == "pending" }
        val preparingOrders = allOrders.count { it.orderStatus == "preparing" }
        val deliveredOrders = allOrders.count { it.orderStatus == "delivered" }
        val cancelledOrders = allOrders.count { it.orderStatus == "cancelled" }

        binding.totalOrdersValue.text = totalOrders.toString()
        binding.pendingValue.text = pendingOrders.toString()
        binding.preparingValue.text = preparingOrders.toString()
        binding.deliveredValue.text = deliveredOrders.toString()
        binding.cancelledValue.text = cancelledOrders.toString()
    }

    private fun updateOrderStatus(order: AdminOrder, newStatus: String) {
        AlertDialog.Builder(this)
            .setTitle("Update Order Status")
            .setMessage("Change order #${order.orderId} status to ${newStatus.replace("_", " ")}?")
            .setPositiveButton("Update") { _, _ ->
                performStatusUpdate(order, newStatus)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performStatusUpdate(order: AdminOrder, newStatus: String) {
        lifecycleScope.launch {
            showLoading(true)
            try {
                firebaseManager.updateOrderStatus(order.id, newStatus)
                Snackbar.make(binding.root, "Order #${order.orderId} status updated to ${newStatus.replace("_", " ")}", Snackbar.LENGTH_SHORT).show()
                loadOrders()
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun viewOrderDetails(order: AdminOrder) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_order_details, null)

        val orderIdText = dialogView.findViewById<android.widget.TextView>(R.id.orderIdText)
        val customerNameText = dialogView.findViewById<android.widget.TextView>(R.id.customerNameText)
        val customerEmailText = dialogView.findViewById<android.widget.TextView>(R.id.customerEmailText)
        val merchantNameText = dialogView.findViewById<android.widget.TextView>(R.id.merchantNameText)
        val riderNameText = dialogView.findViewById<android.widget.TextView>(R.id.riderNameText)
        val orderTotalText = dialogView.findViewById<android.widget.TextView>(R.id.orderTotalText)
        val paymentMethodText = dialogView.findViewById<android.widget.TextView>(R.id.paymentMethodText)
        val deliveryAddressText = dialogView.findViewById<android.widget.TextView>(R.id.deliveryAddressText)
        val orderDateText = dialogView.findViewById<android.widget.TextView>(R.id.orderDateText)
        val statusBadge = dialogView.findViewById<android.widget.TextView>(R.id.statusBadge)

        orderIdText.text = order.orderId
        customerNameText.text = order.customerName
        customerEmailText.text = order.customerEmail
        merchantNameText.text = order.merchantName
        riderNameText.text = order.riderName ?: "Not assigned"
        orderTotalText.text = "₱${String.format("%.2f", order.orderTotal)}"
        paymentMethodText.text = order.paymentMethod
        deliveryAddressText.text = order.deliveryAddress
        orderDateText.text = formatDate(order.orderDate)

        when (order.orderStatus) {
            "pending", "preparing" -> statusBadge.setBackgroundColor(getColor(R.color.warning_bg))
            "on_the_way" -> statusBadge.setBackgroundColor(getColor(R.color.primary_blue_light))
            "delivered" -> statusBadge.setBackgroundColor(getColor(R.color.success_bg))
            "cancelled" -> statusBadge.setBackgroundColor(getColor(R.color.error_bg))
        }
        statusBadge.text = order.orderStatus.replace("_", " ").uppercase()

        AlertDialog.Builder(this)
            .setTitle("Order Details")
            .setView(dialogView)
            .setPositiveButton("Close", null)
            .show()
    }

    private fun formatDate(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            dateString
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