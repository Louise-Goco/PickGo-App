package com.example.pickgo.activities.customer

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.example.pickgo.R
import com.example.pickgo.adapters.OrderAdapter
import com.example.pickgo.databinding.ActivityMyOrdersBinding
import com.example.pickgo.models.Order
import com.example.pickgo.models.OrderStatus
import com.example.pickgo.utils.FirebaseManager
import com.example.pickgo.utils.SessionManager
import kotlinx.coroutines.launch

class MyOrdersActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMyOrdersBinding
    private lateinit var firebaseManager: FirebaseManager
    private lateinit var sessionManager: SessionManager
    private var activeOrders: List<Order> = emptyList()
    private var historyOrders: List<Order> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyOrdersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseManager = FirebaseManager()
        sessionManager = SessionManager(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "My Orders"

        setupViewPager()
        loadOrders()

        if (intent.getStringExtra("review_success") == "success") {
            Snackbar.make(binding.root, "Thank you for your feedback!", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun setupViewPager() {
        val adapter = OrdersPagerAdapter(this)
        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = if (position == 0) "Active Orders" else "Order History"
        }.attach()
    }

    private fun loadOrders() {
        lifecycleScope.launch {
            showLoading(true)
            try {
                val user = firebaseManager.getCurrentUser()
                user?.let {
                    val allOrders = firebaseManager.getCustomerOrders(it.id)

                    activeOrders = allOrders.filter { order ->
                        order.orderStatus !in listOf("delivered", "cancelled")
                    }

                    historyOrders = allOrders.filter { order ->
                        order.orderStatus in listOf("delivered", "cancelled")
                    }

                    updateOrderCounts()
                    (binding.viewPager.adapter as? OrdersPagerAdapter)?.updateData(activeOrders, historyOrders)
                }
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Error loading orders: ${e.message}", Snackbar.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun updateOrderCounts() {
        binding.tabLayout.getTabAt(0)?.text = "Active (${activeOrders.size})"
        binding.tabLayout.getTabAt(1)?.text = "History (${historyOrders.size})"
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    override fun onResume() {
        super.onResume()
        loadOrders()
    }

    inner class OrdersPagerAdapter(activity: AppCompatActivity) : androidx.viewpager2.adapter.FragmentStateAdapter(activity) {
        private var activeList: List<Order> = emptyList()
        private var historyList: List<Order> = emptyList()

        fun updateData(active: List<Order>, history: List<Order>) {
            activeList = active
            historyList = history
            notifyDataSetChanged()
        }

        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): androidx.fragment.app.Fragment {
            return OrdersListFragment.newInstance(
                if (position == 0) activeList else historyList,
                position == 0
            )
        }
    }
}

class OrdersListFragment : androidx.fragment.app.Fragment() {
    private lateinit var adapter: OrderAdapter
    private var orders: List<Order> = emptyList()
    private var isActive: Boolean = true

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: android.os.Bundle?
    ): android.view.View {
        val recyclerView = androidx.recyclerview.widget.RecyclerView(requireContext()).apply {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
            adapter = OrderAdapter { order ->
                val intent = Intent(context, TrackOrderActivity::class.java)
                intent.putExtra("order_id", order.orderId)
                startActivity(intent)
            }.also { adapter = it }
        }

        adapter.submitList(orders)

        if (orders.isEmpty()) {
            val emptyView = android.widget.TextView(requireContext()).apply {
                text = if (isActive) "No active orders" else "No order history"
                textSize = 16f
                setTextColor(requireContext().getColor(R.color.text_secondary))
                gravity = android.view.Gravity.CENTER
                setPadding(0, 100, 0, 100)
            }

            val container = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                addView(recyclerView)
                addView(emptyView)
            }

            recyclerView.visibility = android.view.View.GONE
            return container
        }

        return recyclerView
    }

    companion object {
        fun newInstance(orders: List<Order>, isActive: Boolean): OrdersListFragment {
            return OrdersListFragment().apply {
                this.orders = orders
                this.isActive = isActive
            }
        }
    }
}