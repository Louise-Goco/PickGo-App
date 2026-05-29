package com.example.pickgo.activities.seller

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.example.pickgo.R
import com.example.pickgo.adapters.seller.BestSellerAdapter
import com.example.pickgo.databinding.ActivitySellerAnalyticsBinding
import com.example.pickgo.models.seller.BestSellerItem
import com.example.pickgo.utils.FirebaseManager
import com.example.pickgo.utils.PriceFormatter
import com.example.pickgo.utils.SessionManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class SellerAnalyticsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySellerAnalyticsBinding
    private lateinit var firebaseManager: FirebaseManager
    private lateinit var sessionManager: SessionManager
    private lateinit var bestSellerAdapter: BestSellerAdapter
    private var sellerId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySellerAnalyticsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseManager = FirebaseManager(this)
        sessionManager = SessionManager(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Sales Analytics"

        setupRecyclerViews()
        loadAnalytics()
    }

    private fun setupRecyclerViews() {
        bestSellerAdapter = BestSellerAdapter()
        binding.bestSellersRecycler.layoutManager = LinearLayoutManager(this)
        binding.bestSellersRecycler.adapter = bestSellerAdapter
    }

    private fun loadAnalytics() {
        lifecycleScope.launch {
            showLoading(true)
            try {
                val user = firebaseManager.getCurrentUser()
                val seller = user?.let { firebaseManager.getSellerByEmail(it.email) }
                sellerId = seller?.id ?: return@launch

                val orders = firebaseManager.getOrderItems(sellerId)
                val orderItems = firebaseManager.getAllOrderItems(sellerId)

                val deliveredOrders = orders.filter { it.orderStatus == "delivered" }
                val totalRevenue = deliveredOrders.sumOf { it.orderTotal }
                val totalOrders = orders.size
                val cancelledOrders = orders.filter { it.orderStatus == "cancelled" }.size
                val successRate = if (totalOrders > 0) ((totalOrders - cancelledOrders) * 100 / totalOrders) else 0

                binding.totalRevenue.text = PriceFormatter.format(totalRevenue)
                binding.successRate.text = "$successRate%"
                binding.totalOrders.text = totalOrders.toString()

                // Load revenue trend
                loadRevenueTrend(deliveredOrders)

                // Load best sellers
                loadBestSellers(deliveredOrders, orderItems)

            } catch (e: Exception) {
                Snackbar.make(binding.root, "Error loading analytics: ${e.message}", Snackbar.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun loadRevenueTrend(deliveredOrders: List<com.example.pickgo.models.seller.SellerOrder>) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val trendData = mutableMapOf<String, Double>()

        // Initialize last 7 days
        val calendar = Calendar.getInstance()
        for (i in 6 downTo 0) {
            val date = dateFormat.format(calendar.time)
            trendData[date] = 0.0
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }

        // Aggregate revenue by date
        deliveredOrders.forEach { order ->
            val orderDate = order.orderDate.take(10)
            if (trendData.containsKey(orderDate)) {
                trendData[orderDate] = trendData[orderDate]!! + order.orderTotal
            }
        }

        // Find max revenue for scaling
        val maxRevenue = trendData.values.maxOrNull() ?: 1.0

        // Create chart bars
        binding.trendChartContainer.removeAllViews()

        trendData.forEach { (date, revenue) ->
            val barWrapper = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    1f
                )
                gravity = android.view.Gravity.BOTTOM
            }

            val revenueText = android.widget.TextView(this).apply {
                text = "₱${revenue.toInt()}"
                textSize = 10f
                setTextColor(getColor(R.color.text_secondary))
                gravity = android.view.Gravity.CENTER
            }
            barWrapper.addView(revenueText)

            val barHeight = ((revenue / maxRevenue) * 160).toInt()
            val bar = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    barHeight.coerceAtLeast(4)
                )
                setBackgroundColor(getColor(R.color.accent_color))
            }
            barWrapper.addView(bar)

            val dayLabel = android.widget.TextView(this).apply {
                text = try {
                    dayFormat.format(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date) ?: Date())
                } catch (e: Exception) {
                    date
                }
                textSize = 10f
                setTextColor(getColor(R.color.text_secondary))
                gravity = android.view.Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = 8 }
            }
            barWrapper.addView(dayLabel)

            binding.trendChartContainer.addView(barWrapper)
        }
    }

    private fun loadBestSellers(
        deliveredOrders: List<com.example.pickgo.models.seller.SellerOrder>,
        allOrderItems: List<com.example.pickgo.models.seller.SellerOrder>
    ) {
        val itemSales = mutableMapOf<String, BestSellerItem>()

        // Aggregate items from all delivered orders
        deliveredOrders.forEach { order ->
            order.items.forEach { orderItem ->
                val existing = itemSales[orderItem.foodName]
                if (existing != null) {
                    itemSales[orderItem.foodName] = existing.copy(
                        quantitySold = existing.quantitySold + orderItem.quantity,
                        totalSales = existing.totalSales + orderItem.subtotal
                    )
                } else {
                    itemSales[orderItem.foodName] = BestSellerItem(
                        itemName = orderItem.foodName,
                        quantitySold = orderItem.quantity,
                        totalSales = orderItem.subtotal
                    )
                }
            }
        }

        val bestSellers = itemSales.values.sortedByDescending { it.quantitySold }.take(5)
        bestSellerAdapter.submitList(bestSellers)
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}