package com.example.pickgo.activities.customer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.example.pickgo.R
import com.example.pickgo.databinding.ActivityTrackOrderBinding
import com.example.pickgo.models.Order
import com.example.pickgo.models.OrderStatus
import com.example.pickgo.utils.FirebaseManager
import com.example.pickgo.utils.PriceFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class TrackOrderActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTrackOrderBinding
    private lateinit var firebaseManager: FirebaseManager
    private var order: Order? = null
    private var orderId: String = ""
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrackOrderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseManager = FirebaseManager(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Track Order"

        orderId = intent.getStringExtra("order_id") ?: ""

        loadOrder()
        startOrderStatusSimulation()
    }

    private fun loadOrder() {
        lifecycleScope.launch {
            showLoading(true)
            try {
                order = firebaseManager.getOrderById(orderId)
                order?.let {
                    displayOrderInfo(it)
                    updateTimeline(it.orderStatus)
                }
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Error loading order: ${e.message}", Snackbar.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun displayOrderInfo(order: Order) {
        binding.orderIdText.text = "Order #${order.orderId}"
        binding.orderDateText.text = dateFormat.format(order.orderDate)
        binding.merchantName.text = order.merchantName

        val itemsText = order.items.joinToString("\n") {
            "${it.quantity}x ${it.foodName} - ${PriceFormatter.format(it.subtotal)}"
        }
        binding.orderItemsText.text = itemsText
        binding.deliveryAddressText.text = order.deliveryAddress
        binding.paymentMethodText.text = when (order.paymentMethod) {
            "COD" -> "Cash on Delivery"
            "Card" -> "Credit/Debit Card"
            "GCash" -> "GCash / E-Wallet"
            else -> order.paymentMethod
        }
        binding.orderTotalText.text = PriceFormatter.format(order.orderTotal)
    }

    private fun updateTimeline(status: String) {
        val orderStatus = OrderStatus.fromString(status)

        // Reset all steps
        resetTimelineSteps()

        when (orderStatus) {
            OrderStatus.PENDING -> {
                binding.step1Completed.visibility = View.VISIBLE
                binding.step1Active.visibility = View.GONE
                binding.step2Active.visibility = View.GONE
                binding.step3Active.visibility = View.GONE
                binding.step4Active.visibility = View.GONE
                binding.statusText.text = "Order Confirmed"
                binding.statusSubtext.text = "Your order has been confirmed and is being processed"
            }
            OrderStatus.PREPARING -> {
                binding.step1Completed.visibility = View.VISIBLE
                binding.step2Active.visibility = View.VISIBLE
                binding.statusText.text = "Preparing Your Order"
                binding.statusSubtext.text = "The store is preparing your delicious meal"
            }
            OrderStatus.READY_FOR_PICKUP -> {
                binding.step1Completed.visibility = View.VISIBLE
                binding.step2Completed.visibility = View.VISIBLE
                binding.step3Active.visibility = View.VISIBLE
                binding.statusText.text = "Ready for Pickup"
                binding.statusSubtext.text = "Your order is ready and waiting for the rider"
            }
            OrderStatus.ON_THE_WAY -> {
                binding.step1Completed.visibility = View.VISIBLE
                binding.step2Completed.visibility = View.VISIBLE
                binding.step3Completed.visibility = View.VISIBLE
                binding.step4Active.visibility = View.VISIBLE
                binding.statusText.text = "Out for Delivery"
                binding.statusSubtext.text = "Your rider is on the way to your location"
            }
            OrderStatus.DELIVERED -> {
                binding.step1Completed.visibility = View.VISIBLE
                binding.step2Completed.visibility = View.VISIBLE
                binding.step3Completed.visibility = View.VISIBLE
                binding.step4Completed.visibility = View.VISIBLE
                binding.statusText.text = "Order Delivered"
                binding.statusSubtext.text = "Enjoy your meal!"

                binding.reviewButton.visibility = View.VISIBLE
                binding.reviewButton.setOnClickListener {
                    val intent = Intent(this, ReviewOrderActivity::class.java)
                    intent.putExtra("order_id", orderId)
                    startActivity(intent)
                }
            }
            OrderStatus.CANCELLED -> {
                binding.statusText.text = "Order Cancelled"
                binding.statusSubtext.text = "This order has been cancelled"
            }
        }

        updateEta(orderStatus)
    }

    private fun resetTimelineSteps() {
        binding.step1Completed.visibility = View.GONE
        binding.step1Active.visibility = View.GONE
        binding.step2Completed.visibility = View.GONE
        binding.step2Active.visibility = View.GONE
        binding.step3Completed.visibility = View.GONE
        binding.step3Active.visibility = View.GONE
        binding.step4Completed.visibility = View.GONE
        binding.step4Active.visibility = View.GONE
        binding.reviewButton.visibility = View.GONE
    }

    private fun updateEta(status: OrderStatus) {
        val etaText = when (status) {
            OrderStatus.PENDING, OrderStatus.PREPARING -> "Estimated: 25-35 min"
            OrderStatus.READY_FOR_PICKUP -> "Estimated: 15-20 min"
            OrderStatus.ON_THE_WAY -> "Estimated: 10-15 min"
            OrderStatus.DELIVERED -> "Delivered"
            else -> "Status unavailable"
        }
        binding.etaText.text = etaText
    }

    private fun startOrderStatusSimulation() {
        // For demo purposes - in production, you'd use Firebase Realtime Database or FCM
        lifecycleScope.launch {
            delay(30000) // 30 seconds
            // Simulate status update
            order?.let {
                if (it.orderStatus == "pending") {
                    updateOrderStatus("preparing")
                }
            }
        }

        lifecycleScope.launch {
            delay(90000) // 90 seconds
            order?.let {
                if (it.orderStatus == "preparing") {
                    updateOrderStatus("ready_for_pickup")
                }
            }
        }

        lifecycleScope.launch {
            delay(120000) // 2 minutes
            order?.let {
                if (it.orderStatus == "ready_for_pickup") {
                    updateOrderStatus("on_the_way")
                }
            }
        }

        lifecycleScope.launch {
            delay(180000) // 3 minutes
            order?.let {
                if (it.orderStatus == "on_the_way") {
                    updateOrderStatus("delivered")
                }
            }
        }
    }

    private fun updateOrderStatus(newStatus: String) {
        lifecycleScope.launch {
            try {
                firebaseManager.updateOrderStatus(orderId, newStatus)
                // order object is immutable, just update the UI
                updateTimeline(newStatus)
            } catch (e: Exception) {
                // Handle error silently
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