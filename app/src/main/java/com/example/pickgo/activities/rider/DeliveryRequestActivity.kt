package com.example.pickgo.activities.rider

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.example.pickgo.R
import com.example.pickgo.adapters.rider.DeliveryTripAdapter
import com.example.pickgo.databinding.ActivityDeliveryRequestsBinding
import com.example.pickgo.models.rider.DeliveryOrder
import com.example.pickgo.utils.FirebaseManager
import com.example.pickgo.utils.SessionManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class DeliveryRequestsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDeliveryRequestsBinding
    private lateinit var firebaseManager: FirebaseManager
    private lateinit var sessionManager: SessionManager
    private lateinit var requestAdapter: DeliveryTripAdapter
    private var riderId: String = ""
    private var stationCity: String = ""
    private var pollingJob: kotlinx.coroutines.Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeliveryRequestsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseManager = FirebaseManager()
        sessionManager = SessionManager(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Delivery Requests"

        setupRecyclerView()
        loadRiderData()
        startPolling()

        binding.refreshButton.setOnClickListener {
            loadRequests()
        }
    }

    private fun setupRecyclerView() {
        requestAdapter = DeliveryTripAdapter { order ->
            acceptOrder(order)
        }
        binding.requestsRecycler.layoutManager = LinearLayoutManager(this)
        binding.requestsRecycler.adapter = requestAdapter
    }

    private fun loadRiderData() {
        lifecycleScope.launch {
            try {
                val user = firebaseManager.getCurrentUser()
                user?.let {
                    val rider = firebaseManager.getRiderByEmail(it.email)
                    rider?.let { r ->
                        riderId = r.id
                        stationCity = r.stationCity
                        loadRequests()
                    }
                }
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadRequests() {
        lifecycleScope.launch {
            showLoading(true)
            try {
                val requests = firebaseManager.getAvailableDeliveryTrips(riderId, stationCity)
                if (requests.isEmpty()) {
                    binding.emptyState.visibility = View.VISIBLE
                    binding.requestsRecycler.visibility = View.GONE
                    binding.emptyStateText.text = "No delivery requests available at the moment"
                } else {
                    binding.emptyState.visibility = View.GONE
                    binding.requestsRecycler.visibility = View.VISIBLE
                    requestAdapter.submitList(requests)
                }
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Error loading requests: ${e.message}", Snackbar.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun acceptOrder(order: DeliveryOrder) {
        lifecycleScope.launch {
            try {
                val result = firebaseManager.acceptDeliveryOrder(riderId, order.id)
                if (result) {
                    Snackbar.make(binding.root, "Order accepted! Navigate to pickup.", Snackbar.LENGTH_SHORT).show()
                    loadRequests()
                } else {
                    Snackbar.make(binding.root, "Order no longer available", Snackbar.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun startPolling() {
        pollingJob = lifecycleScope.launch {
            while (true) {
                delay(10000) // Refresh every 10 seconds
                if (stationCity.isNotBlank()) {
                    loadRequests()
                }
            }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.refreshButton.isEnabled = !show
    }

    override fun onDestroy() {
        super.onDestroy()
        pollingJob?.cancel()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}