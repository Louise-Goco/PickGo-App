package com.example.pickgo.activities.admin

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.example.pickgo.R
import com.example.pickgo.adapters.admin.PromoCodeAdapter
import com.example.pickgo.databinding.ActivitySystemSettingsBinding
import com.example.pickgo.models.admin.PromoCode
import com.example.pickgo.models.admin.SystemSettings
import com.example.pickgo.utils.FirebaseManager
import com.example.pickgo.utils.SessionManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class SystemSettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySystemSettingsBinding
    private lateinit var firebaseManager: FirebaseManager
    private lateinit var sessionManager: SessionManager
    private lateinit var promoAdapter: PromoCodeAdapter
    private var currentSettings = SystemSettings()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySystemSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseManager = FirebaseManager(this)
        sessionManager = SessionManager(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "System Settings"

        setupRecyclerView()
        loadSettings()
        loadPromoCodes()
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        promoAdapter = PromoCodeAdapter(
            onDelete = { promo ->
                deletePromoCode(promo)
            }
        )
        binding.promosRecycler.layoutManager = LinearLayoutManager(this)
        binding.promosRecycler.adapter = promoAdapter
    }

    private fun setupClickListeners() {
        binding.addPromoBtn.setOnClickListener {
            showAddPromoDialog()
        }

        binding.saveSettingsBtn.setOnClickListener {
            saveSettings()
        }
    }

    private fun loadSettings() {
        lifecycleScope.launch {
            try {
                currentSettings = firebaseManager.getSystemSettings()
                binding.deliveryFeeInput.setText(currentSettings.deliveryFee.toString())
                binding.serviceFeeInput.setText(currentSettings.serviceFee.toString())
                binding.taxRateInput.setText(currentSettings.taxRate.toString())
                binding.codSwitch.isChecked = currentSettings.paymentCodEnabled
                binding.gcashSwitch.isChecked = currentSettings.paymentGcashEnabled
                binding.cardSwitch.isChecked = currentSettings.paymentCardEnabled
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Error loading settings: ${e.message}", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadPromoCodes() {
        lifecycleScope.launch {
            try {
                val promos = firebaseManager.getAllPromoCodes()
                promoAdapter.submitList(promos.sortedByDescending { it.createdAt })
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    private fun saveSettings() {
        val deliveryFee = binding.deliveryFeeInput.text.toString().toDoubleOrNull() ?: 0.0
        val serviceFee = binding.serviceFeeInput.text.toString().toDoubleOrNull() ?: 0.0
        val taxRate = binding.taxRateInput.text.toString().toDoubleOrNull() ?: 0.0

        val settings = SystemSettings(
            deliveryFee = deliveryFee,
            serviceFee = serviceFee,
            taxRate = taxRate,
            paymentCodEnabled = binding.codSwitch.isChecked,
            paymentGcashEnabled = binding.gcashSwitch.isChecked,
            paymentCardEnabled = binding.cardSwitch.isChecked
        )

        lifecycleScope.launch {
            showLoading(true)
            try {
                firebaseManager.updateSystemSettings(settings)
                Snackbar.make(binding.root, "Settings saved successfully", Snackbar.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Error saving settings: ${e.message}", Snackbar.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun showAddPromoDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_promo, null)
        val codeInput = dialogView.findViewById<TextInputEditText>(R.id.codeInput)
        val typeSpinner = dialogView.findViewById<Spinner>(R.id.typeSpinner)
        val valueInput = dialogView.findViewById<TextInputEditText>(R.id.valueInput)
        val expiryInput = dialogView.findViewById<TextInputEditText>(R.id.expiryInput)
        val limitInput = dialogView.findViewById<TextInputEditText>(R.id.limitInput)

        val types = listOf("percentage", "fixed")
        val typeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, types)
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        typeSpinner.adapter = typeAdapter

        expiryInput.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    val date = "$year-${month + 1}-$day"
                    expiryInput.setText(date)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        AlertDialog.Builder(this)
            .setTitle("Add Promo Code")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val code = codeInput.text.toString().trim().uppercase()
                val type = types[typeSpinner.selectedItemPosition]
                val value = valueInput.text.toString().toDoubleOrNull() ?: 0.0
                val expiryDate = expiryInput.text.toString()
                val limit = limitInput.text.toString().toIntOrNull() ?: 100

                if (code.isEmpty() || value <= 0 || expiryDate.isEmpty()) {
                    Snackbar.make(binding.root, "Please fill all fields", Snackbar.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                lifecycleScope.launch {
                    try {
                        val promoCode = PromoCode(
                            code = code,
                            discountType = type,
                            discountValue = value,
                            expiryDate = expiryDate,
                            usageLimit = limit,
                            currentUsage = 0,
                            createdAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                        )
                        firebaseManager.createPromoCode(promoCode)
                        Snackbar.make(binding.root, "Promo code added", Snackbar.LENGTH_SHORT).show()
                        loadPromoCodes()
                    } catch (e: Exception) {
                        Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deletePromoCode(promo: PromoCode) {
        AlertDialog.Builder(this)
            .setTitle("Delete Promo Code")
            .setMessage("Are you sure you want to delete '${promo.code}'?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    try {
                        firebaseManager.deletePromoCode(promo.id)
                        Snackbar.make(binding.root, "Promo code deleted", Snackbar.LENGTH_SHORT).show()
                        loadPromoCodes()
                    } catch (e: Exception) {
                        Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.saveSettingsBtn.isEnabled = !show
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}