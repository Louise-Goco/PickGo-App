package com.example.pickgo.activities.rider

import android.app.Dialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.pickgo.R
import com.google.android.material.snackbar.Snackbar
import com.example.pickgo.adapters.rider.DeliveryTripAdapter
import com.example.pickgo.databinding.ActivityRiderDashboardBinding
import com.example.pickgo.models.rider.DeliveryOrder
import com.example.pickgo.models.rider.DeliveryRequest
import com.example.pickgo.models.rider.Rider
import com.example.pickgo.utils.FirebaseManager
import com.example.pickgo.utils.PriceFormatter
import com.example.pickgo.utils.SessionManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RiderDashboardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRiderDashboardBinding
    private lateinit var firebaseManager: FirebaseManager
    private lateinit var sessionManager: SessionManager
    private lateinit var deliveryAdapter: DeliveryTripAdapter
    private var currentRider: Rider? = null
    private var riderId: String = ""
    private var isOnline = false
    private var hasActiveOrder = false
    private var pollingJob: kotlinx.coroutines.Job? = null
    private var pendingRequest: DeliveryRequest? = null
    private var requestDialog: Dialog? = null
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRiderDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseManager = FirebaseManager()
        sessionManager = SessionManager(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_menu)

        setupDrawer()
        setupRecyclerView()
        loadRiderData()
        setupClickListeners()
        startRequestPolling()
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
                R.id.nav_earnings -> {
                    startActivity(android.content.Intent(this, RiderEarningsActivity::class.java))
                    binding.drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_requests -> {
                    startActivity(android.content.Intent(this, DeliveryRequestsActivity::class.java))
                    binding.drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_reviews -> {
                    startActivity(android.content.Intent(this, RiderReviewsActivity::class.java))
                    binding.drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_profile -> {
                    startActivity(android.content.Intent(this, RiderProfileActivity::class.java))
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

    private fun setupRecyclerView() {
        deliveryAdapter = DeliveryTripAdapter(
            onAcceptClick = { order ->
                acceptOrder(order)
            }
        )
        binding.contentRecycler.layoutManager = LinearLayoutManager(this)
        binding.contentRecycler.adapter = deliveryAdapter
    }

    private fun setupClickListeners() {
        binding.earningsCard.setOnClickListener {
            startActivity(android.content.Intent(this, RiderEarningsActivity::class.java))
        }

        binding.statusSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                goOnline()
            } else {
                goOffline()
            }
        }

        binding.setupBankBtn.setOnClickListener {
            startActivity(android.content.Intent(this, RiderProfileActivity::class.java))
        }
    }

    private fun loadRiderData() {
        lifecycleScope.launch {
            showLoading(true)
            try {
                val user = firebaseManager.getCurrentUser()
                user?.let {
                    currentRider = firebaseManager.getRiderByEmail(it.email)
                    currentRider?.let { rider ->
                        riderId = rider.id
                        updateUI(rider)
                        loadDashboardData()
                    }
                }
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Error loading data: ${e.message}", Snackbar.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun updateUI(rider: Rider) {
        binding.riderName.text = "${rider.firstName} ${rider.lastName}"
        binding.riderRating.text = String.format("%.1f ★", rider.riderRating)
        isOnline = rider.riderStatus == "active"
        binding.statusSwitch.isChecked = isOnline
        binding.statusTitle.text = if (isOnline) "You are Online" else "You are Offline"
        binding.statusSubtitle.text = if (isOnline) "Receiving delivery requests" else "Toggle to start working"

        // Check bank info
        val hasBankInfo = rider.bankName.isNotBlank() && rider.bankAccountNumber.isNotBlank()
        binding.bankWarning.visibility = if (hasBankInfo) View.GONE else View.VISIBLE

        // Setup station spinner
        setupStationSpinner(rider)
    }

    private fun setupStationSpinner(rider: Rider) {
        lifecycleScope.launch {
            val cities = firebaseManager.getActiveMerchantCities()
            val cityList = cities.toMutableList()
            if (rider.stationCity.isNotBlank() && !cityList.contains(rider.stationCity)) {
                cityList.add(rider.stationCity)
                cityList.sort()
            }

            val adapter = ArrayAdapter(this@RiderDashboardActivity, android.R.layout.simple_spinner_item, cityList as List<String>)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.stationSpinner.adapter = adapter

            val position = cityList.indexOf(rider.stationCity)
            if (position >= 0) {
                binding.stationSpinner.setSelection(position)
            }

            binding.stationSpinner.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val selectedCity = parent?.getItemAtPosition(position) as? String
                    if (selectedCity != null && selectedCity != rider.stationCity) {
                        updateStationCity(selectedCity)
                    }
                }
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            })

            if (rider.stationCity.isBlank()) {
                binding.stationWarning.visibility = View.VISIBLE
            } else {
                binding.stationWarning.visibility = View.GONE
                binding.stationBadge.text = "📍 ${rider.stationCity}"
                binding.stationBadge.visibility = View.VISIBLE
            }
        }
    }

    private fun updateStationCity(city: String) {
        lifecycleScope.launch {
            try {
                firebaseManager.updateRider(riderId, mapOf("stationCity" to city))
                // Reload rider data to get updated stationCity
                loadRiderData()
                if (city.isNotBlank()) {
                    binding.stationWarning.visibility = View.GONE
                    binding.stationBadge.text = "📍 $city"
                    binding.stationBadge.visibility = View.VISIBLE
                }
                loadDashboardData()
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Error updating station: ${e.message}", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadDashboardData() {
        lifecycleScope.launch {
            try {
                val stats = firebaseManager.getRiderTodayStats(riderId)
                binding.todayTrips.text = (stats["deliveries"] as? Int ?: 0).toString()
                binding.todayEarnings.text = PriceFormatter.format(stats["earnings"] as? Double ?: 0.0)

                val activeOrders = firebaseManager.getRiderActiveDeliveries(riderId)
                hasActiveOrder = activeOrders.isNotEmpty()

                if (activeOrders.isNotEmpty()) {
                    binding.sectionTitle.text = "Active Deliveries"
                    deliveryAdapter.submitList(activeOrders)
                    binding.emptyState.visibility = View.GONE
                    binding.contentRecycler.visibility = View.VISIBLE
                } else if (isOnline && currentRider?.stationCity?.isNotBlank() == true) {
                    binding.sectionTitle.text = "Available Delivery Trips"
                    val availableTrips = firebaseManager.getAvailableDeliveryTrips(riderId, currentRider?.stationCity)
                    if (availableTrips.isNotEmpty()) {
                        deliveryAdapter.submitList(availableTrips)
                        binding.emptyState.visibility = View.GONE
                        binding.contentRecycler.visibility = View.VISIBLE
                    } else {
                        showEmptyState("No available trips", "No delivery requests at the moment")
                    }
                } else {
                    showEmptyState(
                        if (!isOnline) "You are offline" else "No active deliveries",
                        if (!isOnline) "Go online to receive delivery requests" else "Complete deliveries to see more"
                    )
                }
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Error loading data: ${e.message}", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun showEmptyState(title: String, subtitle: String) {
        binding.emptyStateTitle.text = title
        binding.emptyStateSubtitle.text = subtitle
        binding.emptyState.visibility = View.VISIBLE
        binding.contentRecycler.visibility = View.GONE
    }

    private fun goOnline() {
        val rider = currentRider
        if (rider == null) return

        if (rider.bankName.isBlank() || rider.bankAccountNumber.isBlank()) {
            binding.statusSwitch.isChecked = false
            Snackbar.make(binding.root, "Please complete your Bank Account information first", Snackbar.LENGTH_LONG)
                .setAction("Setup") {
                    startActivity(android.content.Intent(this, RiderProfileActivity::class.java))
                }.show()
            return
        }

        if (rider.stationCity.isBlank()) {
            binding.statusSwitch.isChecked = false
            Snackbar.make(binding.root, "Please select your waiting station city", Snackbar.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                firebaseManager.updateRider(riderId, mapOf("riderStatus" to "active"))
                // Reload to get updated status
                loadRiderData()
                isOnline = true
                binding.statusTitle.text = "You are Online"
                binding.statusSubtitle.text = "Receiving delivery requests"
                loadDashboardData()
                Snackbar.make(binding.root, "You are now online", Snackbar.LENGTH_SHORT).show()
            } catch (e: Exception) {
                binding.statusSwitch.isChecked = false
                Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun goOffline() {
        lifecycleScope.launch {
            try {
                firebaseManager.updateRider(riderId, mapOf("riderStatus" to "offline"))
                // Reload to get updated status
                loadRiderData()
                isOnline = false
                binding.statusTitle.text = "You are Offline"
                binding.statusSubtitle.text = "Toggle to start working"
                loadDashboardData()
                Snackbar.make(binding.root, "You are now offline", Snackbar.LENGTH_SHORT).show()
            } catch (e: Exception) {
                binding.statusSwitch.isChecked = true
                Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun acceptOrder(order: DeliveryOrder) {
        lifecycleScope.launch {
            try {
                val result = firebaseManager.acceptDeliveryOrder(riderId, order.id)
                if (result) {
                    Snackbar.make(binding.root, "Order accepted! Navigate to pickup.", Snackbar.LENGTH_SHORT).show()
                    loadDashboardData()
                } else {
                    Snackbar.make(binding.root, "Order no longer available", Snackbar.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun startRequestPolling() {
        pollingJob = lifecycleScope.launch {
            while (true) {
                if (isOnline && !hasActiveOrder && currentRider?.stationCity?.isNotBlank() == true) {
                    checkForNewRequests()
                }
                delay(5000) // Check every 5 seconds
            }
        }
    }

    private fun checkForNewRequests() {
        lifecycleScope.launch {
            try {
                val request = firebaseManager.fetchNextDeliveryRequest(riderId, currentRider?.stationCity)
                if (request != null && pendingRequest?.id != request.id) {
                    pendingRequest = request
                    showRequestDialog(request)
                }
            } catch (e: Exception) {
                // Silent fail
            }
        }
    }

    private fun showRequestDialog(request: DeliveryRequest) {
        requestDialog?.dismiss()

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_delivery_request, null)
        val pickupText = dialogView.findViewById<TextView>(R.id.pickupText)
        val pickupAddressText = dialogView.findViewById<TextView>(R.id.pickupAddressText)
        val deliveryAddressText = dialogView.findViewById<TextView>(R.id.deliveryAddressText)
        val earningsText = dialogView.findViewById<TextView>(R.id.earningsText)
        val acceptBtn = dialogView.findViewById<Button>(R.id.acceptBtn)
        val rejectBtn = dialogView.findViewById<Button>(R.id.rejectBtn)
        val dismissBtn = dialogView.findViewById<Button>(R.id.dismissBtn)

        pickupText.text = request.merchantName
        pickupAddressText.text = request.pickupAddress
        deliveryAddressText.text = request.deliveryAddress
        earningsText.text = PriceFormatter.format(request.earnings)

        requestDialog = Dialog(this, android.R.style.Theme_DeviceDefault_Light_Dialog).apply {
            setContentView(dialogView)
            setCancelable(false)
            window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
            window?.setGravity(Gravity.CENTER)
        }

        acceptBtn.setOnClickListener {
            requestDialog?.dismiss()
            lifecycleScope.launch {
                val result = firebaseManager.acceptDeliveryOrder(riderId, request.id)
                if (result) {
                    Snackbar.make(binding.root, "Order accepted! Navigate to pickup.", Snackbar.LENGTH_SHORT).show()
                    loadDashboardData()
                } else {
                    Snackbar.make(binding.root, "Order no longer available", Snackbar.LENGTH_SHORT).show()
                }
            }
        }

        rejectBtn.setOnClickListener {
            requestDialog?.dismiss()
            pendingRequest = null
        }

        dismissBtn.setOnClickListener {
            requestDialog?.dismiss()
            pendingRequest = null
        }

        requestDialog?.show()
        handler.postDelayed({
            requestDialog?.dismiss()
            pendingRequest = null
        }, 30000) // Auto-dismiss after 30 seconds
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

    override fun onDestroy() {
        super.onDestroy()
        pollingJob?.cancel()
        handler.removeCallbacksAndMessages(null)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            binding.drawerLayout.openDrawer(androidx.core.view.GravityCompat.START)
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}