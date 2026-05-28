package com.example.pickgo.activities.seller

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.example.pickgo.R
import com.example.pickgo.adapters.seller.PayoutHistoryAdapter
import com.example.pickgo.adapters.seller.RecentEarningsAdapter
import com.example.pickgo.databinding.ActivitySellerPayoutsBinding
import com.example.pickgo.models.seller.PayoutRequest
import com.example.pickgo.utils.FirebaseManager
import com.example.pickgo.utils.PriceFormatter
import com.example.pickgo.utils.SessionManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class SellerPayoutsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySellerPayoutsBinding
    private lateinit var firebaseManager: FirebaseManager
    private lateinit var sessionManager: SessionManager
    private lateinit var payoutHistoryAdapter: PayoutHistoryAdapter
    private lateinit var recentEarningsAdapter: RecentEarningsAdapter
    private var sellerId: String = ""
    private var availableBalance: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySellerPayoutsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseManager = FirebaseManager()
        sessionManager = SessionManager(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Earnings & Payouts"

        setupRecyclerViews()
        loadSellerData()
        setupClickListeners()
    }

    private fun setupRecyclerViews() {
        payoutHistoryAdapter = PayoutHistoryAdapter()
        recentEarningsAdapter = RecentEarningsAdapter()

        binding.payoutHistoryRecycler.layoutManager = LinearLayoutManager(this)
        binding.payoutHistoryRecycler.adapter = payoutHistoryAdapter

        binding.recentEarningsRecycler.layoutManager = LinearLayoutManager(this)
        binding.recentEarningsRecycler.adapter = recentEarningsAdapter
    }

    private fun setupClickListeners() {
        binding.withdrawBtn.setOnClickListener {
            requestPayout()
        }
    }

    private fun loadSellerData() {
        lifecycleScope.launch {
            showLoading(true)
            try {
                val user = firebaseManager.getCurrentUser()
                val seller = user?.let { firebaseManager.getSellerByEmail(it.email) }
                sellerId = seller?.id ?: return@launch

                loadBalanceAndEarnings()
                loadPayoutHistory()
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Error loading data: ${e.message}", Snackbar.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun loadBalanceAndEarnings() {
        lifecycleScope.launch {
            try {
                val orders = firebaseManager.getOrderItems(sellerId)
                val payouts = firebaseManager.getSellerPayouts(sellerId)

                val totalEarned = orders.filter { it.orderStatus == "delivered" }
                    .sumOf { it.orderTotal }
                val totalRequested = payouts.filter { it.payoutStatus != "rejected" }
                    .sumOf { it.amount }
                availableBalance = totalEarned - totalRequested

                binding.balanceValue.text = PriceFormatter.format(availableBalance)
                binding.totalEarned.text = PriceFormatter.format(totalEarned)

                // Set max amount for withdrawal
                binding.amountInput.setText("")

                // Load recent earnings (last 10 delivered orders)
                val recentOrders = orders
                    .filter { it.orderStatus == "delivered" }
                    .sortedByDescending { it.orderDate }
                    .take(10)
                recentEarningsAdapter.submitList(recentOrders)

            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    private fun loadPayoutHistory() {
        lifecycleScope.launch {
            try {
                val payouts = firebaseManager.getSellerPayouts(sellerId)
                // Convert SellerPayout to PayoutRequest for adapter
                val payoutRequests = payouts.map { payout ->
                    PayoutRequest(
                        id = payout.id,
                        userId = payout.sellerId,
                        userType = "seller",
                        amount = payout.amount,
                        bankName = payout.bankName,
                        accountNumber = payout.accountNumber,
                        accountName = payout.accountName,
                        payoutStatus = payout.payoutStatus,
                        requestDate = payout.requestDate
                    )
                }
                val sortedPayouts = payoutRequests.sortedByDescending { it.requestDate }
                payoutHistoryAdapter.submitList(sortedPayouts)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    private fun requestPayout() {
        val amountStr = binding.amountInput.text.toString().trim()
        val bankName = binding.bankNameInput.text.toString().trim()
        val accountName = binding.accountNameInput.text.toString().trim()
        val accountNumber = binding.accountNumberInput.text.toString().trim()

        if (amountStr.isEmpty()) {
            Snackbar.make(binding.root, "Please enter amount", Snackbar.LENGTH_SHORT).show()
            return
        }

        val amount = amountStr.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            Snackbar.make(binding.root, "Please enter a valid amount", Snackbar.LENGTH_SHORT).show()
            return
        }

        if (amount > availableBalance) {
            Snackbar.make(binding.root, "Insufficient balance", Snackbar.LENGTH_SHORT).show()
            return
        }

        if (bankName.isEmpty()) {
            Snackbar.make(binding.root, "Please enter bank name", Snackbar.LENGTH_SHORT).show()
            return
        }

        if (accountName.isEmpty()) {
            Snackbar.make(binding.root, "Please enter account name", Snackbar.LENGTH_SHORT).show()
            return
        }

        if (accountNumber.isEmpty()) {
            Snackbar.make(binding.root, "Please enter account number", Snackbar.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            showLoading(true)
            try {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val payoutRequest = PayoutRequest(
                    userId = sellerId,
                    userType = "seller",
                    amount = amount,
                    bankName = bankName,
                    accountName = accountName,
                    accountNumber = accountNumber,
                    payoutStatus = "pending",
                    requestDate = dateFormat.format(Date())
                )

                firebaseManager.createPayoutRequest(payoutRequest)

                // Clear form
                binding.amountInput.text?.clear()
                binding.bankNameInput.text?.clear()
                binding.accountNameInput.text?.clear()
                binding.accountNumberInput.text?.clear()

                Snackbar.make(binding.root, "Payout request submitted successfully!", Snackbar.LENGTH_SHORT).show()

                // Refresh data
                loadBalanceAndEarnings()
                loadPayoutHistory()
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Error submitting request: ${e.message}", Snackbar.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.withdrawBtn.isEnabled = !show
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}