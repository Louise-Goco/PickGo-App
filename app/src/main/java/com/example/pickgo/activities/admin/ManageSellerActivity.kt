package com.example.pickgo.activities.admin

import android.os.Bundle
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.example.pickgo.adapters.admin.SellerAdapter
import com.example.pickgo.databinding.ActivityManageSellersBinding
import com.example.pickgo.databinding.DialogEditSellerBinding
import com.example.pickgo.models.admin.AdminSeller
import com.example.pickgo.utils.DocumentViewer
import com.example.pickgo.utils.FirebaseManager
import com.example.pickgo.utils.SessionManager
import kotlinx.coroutines.launch

class ManageSellersActivity : AppCompatActivity() {
    private lateinit var binding: ActivityManageSellersBinding
    private lateinit var firebaseManager: FirebaseManager
    private lateinit var sessionManager: SessionManager
    private lateinit var sellerAdapter: SellerAdapter
    private var allSellers: List<AdminSeller> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageSellersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseManager = FirebaseManager()
        sessionManager = SessionManager(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Seller Management"

        setupRecyclerView()
        setupSearch()
        loadSellers()
    }

    private fun setupRecyclerView() {
        sellerAdapter = SellerAdapter(
            onEdit = { seller ->
                showEditSellerDialog(seller)
            },
            onApprove = { seller ->
                approveSeller(seller)
            },
            onReject = { seller ->
                rejectSeller(seller)
            },
            onSuspend = { seller ->
                suspendSeller(seller)
            },
            onActivate = { seller ->
                activateSeller(seller)
            },
            onDelete = { seller ->
                confirmDelete(seller)
            },
            onViewDocuments = { seller ->
                viewDocuments(seller)
            }
        )
        binding.sellersRecycler.layoutManager = LinearLayoutManager(this)
        binding.sellersRecycler.adapter = sellerAdapter
    }

    private fun setupSearch() {
        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterSellers(s.toString())
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun loadSellers() {
        lifecycleScope.launch {
            showLoading(true)
            try {
                allSellers = firebaseManager.getAllSellersWithMerchants()
                allSellers = allSellers.sortedByDescending { it.createdAt }
                sellerAdapter.submitList(allSellers)

                if (allSellers.isEmpty()) {
                    binding.emptyState.visibility = View.VISIBLE
                    binding.sellersRecycler.visibility = View.GONE
                } else {
                    binding.emptyState.visibility = View.GONE
                    binding.sellersRecycler.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Error loading sellers: ${e.message}", Snackbar.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun filterSellers(query: String) {
        if (query.isEmpty()) {
            sellerAdapter.submitList(allSellers)
            return
        }
        val filtered = allSellers.filter {
            "${it.firstName} ${it.lastName}".contains(query, ignoreCase = true) ||
                    it.email.contains(query, ignoreCase = true) ||
                    it.merchantName.contains(query, ignoreCase = true)
        }
        sellerAdapter.submitList(filtered)
    }

    private fun showEditSellerDialog(seller: AdminSeller) {
        val dialogBinding = DialogEditSellerBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
            .setTitle("Edit Seller Details")
            .setView(dialogBinding.root)
            .setPositiveButton("Save") { _, _ ->
                saveSellerChanges(seller, dialogBinding)
            }
            .setNegativeButton("Cancel", null)
            .create()

        // Populate fields
        dialogBinding.firstNameInput.setText(seller.firstName)
        dialogBinding.lastNameInput.setText(seller.lastName)
        dialogBinding.emailInput.setText(seller.email)
        dialogBinding.phoneInput.setText(seller.phoneNumber)
        dialogBinding.storeNameInput.setText(seller.merchantName)

        // Setup store type spinner
        val storeTypes = listOf("Restaurant", "Fast Food", "Cafe", "Grocery", "Pharmacy", "Bakery")
        val typeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, storeTypes)
        dialogBinding.storeTypeSpinner.adapter = typeAdapter
        val typePosition = storeTypes.indexOf(seller.merchantType)
        if (typePosition >= 0) {
            dialogBinding.storeTypeSpinner.setSelection(typePosition)
        }

        dialog.show()
    }

    private fun saveSellerChanges(seller: AdminSeller, binding: DialogEditSellerBinding) {
        val firstName = binding.firstNameInput.text.toString().trim()
        val lastName = binding.lastNameInput.text.toString().trim()
        val email = binding.emailInput.text.toString().trim()
        val phone = binding.phoneInput.text.toString().trim()
        val storeName = binding.storeNameInput.text.toString().trim()
        val storeType = binding.storeTypeSpinner.selectedItem.toString()

        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || phone.isEmpty() || storeName.isEmpty()) {
            Snackbar.make(this.binding.root, "Please fill all fields", Snackbar.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            showLoading(true)
            try {
                firebaseManager.updateSeller(seller.id, mapOf(
                    "Sellr_Fname" to firstName,
                    "Sellr_Lname" to lastName,
                    "Sellr_Email" to email,
                    "Sellr_PhoneNumber" to phone
                ))
                firebaseManager.updateMerchant(seller.merchantId, mapOf(
                    "Merch_Name" to storeName,
                    "Merch_Type" to storeType
                ))
                Snackbar.make(this@ManageSellersActivity.binding.root, "Seller updated successfully", Snackbar.LENGTH_SHORT).show()
                loadSellers()
            } catch (e: Exception) {
                Snackbar.make(this@ManageSellersActivity.binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun approveSeller(seller: AdminSeller) {
        AlertDialog.Builder(this)
            .setTitle("Approve Seller")
            .setMessage("Approve ${seller.firstName} ${seller.lastName} as a seller?")
            .setPositiveButton("Approve") { _, _ ->
                updateSellerStatus(seller, "active", "active")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun rejectSeller(seller: AdminSeller) {
        AlertDialog.Builder(this)
            .setTitle("Reject Application")
            .setMessage("Reject ${seller.firstName} ${seller.lastName}'s application?")
            .setPositiveButton("Reject") { _, _ ->
                updateSellerStatus(seller, "rejected", "closed")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun suspendSeller(seller: AdminSeller) {
        AlertDialog.Builder(this)
            .setTitle("Suspend Seller")
            .setMessage("Suspend ${seller.firstName} ${seller.lastName}'s store?")
            .setPositiveButton("Suspend") { _, _ ->
                updateSellerStatus(seller, "suspended", "suspended")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun activateSeller(seller: AdminSeller) {
        AlertDialog.Builder(this)
            .setTitle("Activate Seller")
            .setMessage("Activate ${seller.firstName} ${seller.lastName}'s store?")
            .setPositiveButton("Activate") { _, _ ->
                updateSellerStatus(seller, "active", "active")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateSellerStatus(seller: AdminSeller, sellerStatus: String, merchantStatus: String) {
        lifecycleScope.launch {
            showLoading(true)
            try {
                firebaseManager.updateSeller(seller.id, mapOf("Sellr_Status" to sellerStatus))
                firebaseManager.updateMerchant(seller.merchantId, mapOf("Merch_Status" to merchantStatus))
                Snackbar.make(binding.root, "Seller ${if (sellerStatus == "active") "approved" else sellerStatus}", Snackbar.LENGTH_SHORT).show()
                loadSellers()
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun confirmDelete(seller: AdminSeller) {
        AlertDialog.Builder(this)
            .setTitle("Delete Seller")
            .setMessage("Are you sure you want to delete ${seller.firstName} ${seller.lastName} and their store? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteSeller(seller)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteSeller(seller: AdminSeller) {
        lifecycleScope.launch {
            showLoading(true)
            try {
                firebaseManager.deleteSeller(seller.id)
                firebaseManager.deleteMerchant(seller.merchantId)
                Snackbar.make(binding.root, "Seller deleted", Snackbar.LENGTH_SHORT).show()
                loadSellers()
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun viewDocuments(seller: AdminSeller) {
        val items = mutableListOf<Pair<String, String?>>()
        if (!seller.govIdUrl.isNullOrBlank()) {
            items.add("Government ID" to seller.govIdUrl)
        }
        if (!seller.birCertUrl.isNullOrBlank()) {
            items.add("BIR Certificate" to seller.birCertUrl)
        }

        if (items.isEmpty()) {
            Snackbar.make(binding.root, "No documents uploaded", Snackbar.LENGTH_SHORT).show()
            return
        }

        DocumentViewer.showDocumentDialog(this, items)
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}