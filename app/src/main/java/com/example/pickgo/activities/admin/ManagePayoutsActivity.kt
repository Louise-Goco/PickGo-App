package com.example.pickgo.activities.admin

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.example.pickgo.R
import com.example.pickgo.adapters.admin.PayoutAdapter
import com.example.pickgo.databinding.ActivityManagePayoutsBinding
import com.example.pickgo.models.admin.AdminPayout
import com.example.pickgo.utils.FirebaseManager
import com.example.pickgo.utils.PriceFormatter
import com.example.pickgo.utils.SessionManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ManagePayoutsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityManagePayoutsBinding
    private lateinit var firebaseManager: FirebaseManager
    private lateinit var sessionManager: SessionManager
    private lateinit var payoutAdapter: PayoutAdapter
    private var allPayouts: List<AdminPayout> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManagePayoutsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseManager = FirebaseManager(this)
        sessionManager = SessionManager(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Payout Management"

        setupRecyclerView()
        loadPayouts()
        setupTabListeners()
    }

    private fun setupRecyclerView() {
        payoutAdapter = PayoutAdapter(
            onApprove = { payout ->
                approvePayout(payout)
            },
            onProcess = { payout ->
                processPayout(payout)
            },
            onReject = { payout ->
                rejectPayout(payout)
            }
        )
        binding.payoutsRecycler.layoutManager = LinearLayoutManager(this)
        binding.payoutsRecycler.adapter = payoutAdapter
    }

    private fun setupTabListeners() {
        binding.tabPending.setOnClickListener {
            selectTab(0)
            filterPayouts("pending")
        }
        binding.tabApproved.setOnClickListener {
            selectTab(1)
            filterPayouts("approved")
        }
        binding.tabProcessed.setOnClickListener {
            selectTab(2)
            filterPayouts("processed")
        }
        binding.tabRejected.setOnClickListener {
            selectTab(3)
            filterPayouts("rejected")
        }
        binding.tabAll.setOnClickListener {
            selectTab(4)
            filterPayouts("all")
        }
    }

    private fun selectTab(index: Int) {
        val tabs = listOf(
            binding.tabPending, binding.tabApproved,
            binding.tabProcessed, binding.tabRejected, binding.tabAll
        )
        tabs.forEachIndexed { i, tab ->
            tab.isSelected = i == index
            tab.setTextColor(if (i == index) getColor(R.color.accent_color) else getColor(R.color.text_secondary))
        }
    }

    private fun loadPayouts() {
        lifecycleScope.launch {
            showLoading(true)
            try {
                allPayouts = firebaseManager.getAllPayouts()
                allPayouts = allPayouts.sortedByDescending { it.requestDate }
                filterPayouts("pending")

                updateStats()
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Error loading payouts: ${e.message}", Snackbar.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun filterPayouts(status: String) {
        val filtered = when (status) {
            "all" -> allPayouts
            else -> allPayouts.filter { it.payoutStatus == status }
        }
        payoutAdapter.submitList(filtered)

        if (filtered.isEmpty()) {
            binding.emptyState.visibility = View.VISIBLE
            binding.payoutsRecycler.visibility = View.GONE
            binding.emptyStateText.text = when (status) {
                "pending" -> "No pending payout requests"
                "approved" -> "No approved payouts"
                "processed" -> "No processed payouts"
                "rejected" -> "No rejected payouts"
                else -> "No payout requests"
            }
        } else {
            binding.emptyState.visibility = View.GONE
            binding.payoutsRecycler.visibility = View.VISIBLE
        }
    }

    private fun updateStats() {
        val pendingTotal = allPayouts.filter { it.payoutStatus == "pending" }.sumOf { it.amount }
        val approvedTotal = allPayouts.filter { it.payoutStatus == "approved" }.sumOf { it.amount }
        val processedTotal = allPayouts.filter { it.payoutStatus == "processed" }.sumOf { it.amount }

        binding.pendingTotalValue.text = PriceFormatter.format(pendingTotal)
        binding.approvedTotalValue.text = PriceFormatter.format(approvedTotal)
        binding.processedTotalValue.text = PriceFormatter.format(processedTotal)

        binding.pendingCountValue.text = allPayouts.count { it.payoutStatus == "pending" }.toString()
    }

    private fun approvePayout(payout: AdminPayout) {
        AlertDialog.Builder(this)
            .setTitle("Approve Payout")
            .setMessage("Approve payout of ${PriceFormatter.format(payout.amount)} to ${payout.userName}?")
            .setPositiveButton("Approve") { _, _ ->
                updatePayoutStatus(payout, "approved")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun processPayout(payout: AdminPayout) {
        AlertDialog.Builder(this)
            .setTitle("Process Payout")
            .setMessage("Mark payout of ${PriceFormatter.format(payout.amount)} to ${payout.userName} as processed?")
            .setPositiveButton("Process") { _, _ ->
                updatePayoutStatus(payout, "processed")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun rejectPayout(payout: AdminPayout) {
        val input = android.widget.EditText(this).apply {
            hint = "Reason for rejection"
            inputType = android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
        }

        AlertDialog.Builder(this)
            .setTitle("Reject Payout")
            .setMessage("Reject payout of ${PriceFormatter.format(payout.amount)} to ${payout.userName}?")
            .setView(input)
            .setPositiveButton("Reject") { _, _ ->
                val reason = input.text.toString().trim()
                updatePayoutStatus(payout, "rejected", reason)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updatePayoutStatus(payout: AdminPayout, status: String, reason: String = "") {
        lifecycleScope.launch {
            showLoading(true)
            try {
                val updates = mutableMapOf<String, Any>("Payout_Status" to status)
                if (status == "processed") {
                    updates["Processed_Date"] = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                }
                if (reason.isNotEmpty()) {
                    updates["Rejection_Reason"] = reason
                }

                firebaseManager.updatePayout(payout.id, updates)
                Snackbar.make(binding.root, "Payout ${if (status == "processed") "marked as processed" else status}", Snackbar.LENGTH_SHORT).show()
                loadPayouts()
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
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