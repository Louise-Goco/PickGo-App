package com.example.pickgo.activities.rider

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.example.pickgo.R
import com.example.pickgo.adapters.rider.PayoutHistoryAdapter
import com.example.pickgo.adapters.rider.TripHistoryAdapter
import com.example.pickgo.databinding.ActivityRiderEarningsBinding
import com.example.pickgo.models.rider.PayoutRequest
import com.example.pickgo.utils.FirebaseManager
import com.example.pickgo.utils.PriceFormatter
import com.example.pickgo.utils.SessionManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class RiderEarningsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRiderEarningsBinding
    private lateinit var firebaseManager: FirebaseManager
    private lateinit var sessionManager: SessionManager
    private lateinit var tripHistoryAdapter: TripHistoryAdapter
    private lateinit var payoutHistoryAdapter: PayoutHistoryAdapter
    private var riderId: String = ""
    private var withdrawableBalance: Double = 0.0
    private var riderName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRiderEarningsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseManager = FirebaseManager()
        sessionManager = SessionManager(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Earnings Dashboard"

        setupRecyclerViews()
        loadRiderData()
        setupClickListeners()
    }

    private fun setupRecyclerViews() {
        tripHistoryAdapter = TripHistoryAdapter()
        payoutHistoryAdapter = PayoutHistoryAdapter()

        binding.tripHistoryRecycler.layoutManager = LinearLayoutManager(this)
        binding.tripHistoryRecycler.adapter = tripHistoryAdapter

        binding.payoutHistoryRecycler.layoutManager = LinearLayoutManager(this)
        binding.payoutHistoryRecycler.adapter = payoutHistoryAdapter
    }

    private fun setupClickListeners() {
        binding.withdrawButton.setOnClickListener {
            showWithdrawDialog()
        }
    }

    private fun loadRiderData() {
        lifecycleScope.launch {
            showLoading(true)
            try {
                val user = firebaseManager.getCurrentUser()
                user?.let {
                    val rider = firebaseManager.getRiderByEmail(it.email)
                    rider?.let { r ->
                        riderId = r.id
                        riderName = "${r.firstName} ${r.lastName}"
                        loadEarningsData()
                    }
                }
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Error loading data: ${e.message}", Snackbar.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun loadEarningsData() {
        lifecycleScope.launch {
            try {
                val earnings = firebaseManager.getRiderEarningsData(riderId)
                withdrawableBalance = earnings.withdrawableBalance
                binding.balanceValue.text = PriceFormatter.format(earnings.withdrawableBalance)
                tripHistoryAdapter.submitList(earnings.trips)
                payoutHistoryAdapter.submitList(earnings.payouts)
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Error loading earnings: ${e.message}", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun showWithdrawDialog() {
        if (withdrawableBalance <= 0) {
            Snackbar.make(binding.root, "No funds available for withdrawal", Snackbar.LENGTH_SHORT).show()
            return
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_withdraw, null)
        val amountInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.amountInput)
        val bankSpinner = dialogView.findViewById<android.widget.Spinner>(R.id.bankSpinner)
        val accountNumberInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.accountNumberInput)
        val accountNameInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.accountNameInput)

        val banks = listOf("GCash", "Maya", "BDO Unibank", "BPI", "UnionBank", "Metrobank")
        val bankAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, banks)
        bankAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        bankSpinner.adapter = bankAdapter

        AlertDialog.Builder(this)
            .setTitle("Withdraw Funds")
            .setView(dialogView)
            .setPositiveButton("Withdraw") { _, _ ->
                val amountStr = amountInput.text.toString()
                val amount = amountStr.toDoubleOrNull()
                if (amount == null || amount <= 0) {
                    Snackbar.make(binding.root, "Please enter a valid amount", Snackbar.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (amount > withdrawableBalance) {
                    Snackbar.make(binding.root, "Amount exceeds available balance", Snackbar.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val bankName = banks[bankSpinner.selectedItemPosition]
                val accountNumber = accountNumberInput.text.toString()
                val accountName = accountNameInput.text.toString()

                if (accountNumber.isBlank() || accountName.isBlank()) {
                    Snackbar.make(binding.root, "Please enter account details", Snackbar.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                requestPayout(amount, bankName, accountNumber, accountName)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun requestPayout(amount: Double, bankName: String, accountNumber: String, accountName: String) {
        lifecycleScope.launch {
            showLoading(true)
            try {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val payoutRequest = PayoutRequest(
                    userId = riderId,
                    userType = "rider",
                    amount = amount,
                    bankName = bankName,
                    accountNumber = accountNumber,
                    accountName = accountName,
                    payoutStatus = "pending",
                    requestDate = dateFormat.format(Date())
                )
                firebaseManager.createPayoutRequest(payoutRequest)
                Snackbar.make(binding.root, "Payout request submitted successfully", Snackbar.LENGTH_SHORT).show()
                loadEarningsData()
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.withdrawButton.isEnabled = !show
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}