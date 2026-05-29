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
import com.example.pickgo.R
import com.example.pickgo.adapters.admin.RiderAdapter
import com.example.pickgo.databinding.ActivityManageRidersBinding
import com.example.pickgo.databinding.DialogEditRiderBinding
import com.example.pickgo.models.admin.AdminRider
import com.example.pickgo.utils.DocumentViewer
import com.example.pickgo.utils.FirebaseManager
import com.example.pickgo.utils.SessionManager
import kotlinx.coroutines.launch

class ManageRidersActivity : AppCompatActivity() {
    private lateinit var binding: ActivityManageRidersBinding
    private lateinit var firebaseManager: FirebaseManager
    private lateinit var sessionManager: SessionManager
    private lateinit var riderAdapter: RiderAdapter
    private var allRiders: List<AdminRider> = emptyList()
    private var filteredRiders: List<AdminRider> = emptyList()
    private var currentFilter: String = "all"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageRidersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseManager = FirebaseManager(this)
        sessionManager = SessionManager(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Rider Management"

        setupRecyclerView()
        setupSearch()
        setupStatusFilters()
        loadRiders()
    }

    private fun setupRecyclerView() {
        riderAdapter = RiderAdapter(
            onEdit = { rider ->
                showEditRiderDialog(rider)
            },
            onApprove = { rider ->
                approveRider(rider)
            },
            onReject = { rider ->
                rejectRider(rider)
            },
            onSuspend = { rider ->
                suspendRider(rider)
            },
            onActivate = { rider ->
                activateRider(rider)
            },
            onVerify = { rider ->
                verifyDocuments(rider)
            },
            onDelete = { rider ->
                confirmDelete(rider)
            },
            onViewDocuments = { rider ->
                viewDocuments(rider)
            }
        )
        binding.ridersRecycler.layoutManager = LinearLayoutManager(this)
        binding.ridersRecycler.adapter = riderAdapter
    }

    private fun setupSearch() {
        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applyFilters()
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun setupStatusFilters() {
        binding.statusFilterGroup.setOnCheckedChangeListener { group, checkedId ->
            currentFilter = when (checkedId) {
                R.id.filterAllChip -> "all"
                R.id.filterPendingChip -> "pending"
                R.id.filterApprovedChip -> "active"
                R.id.filterRejectedChip -> "rejected"
                R.id.filterSuspendedChip -> "suspended"
                else -> "all"
            }
            applyFilters()
        }
    }

    private fun applyFilters() {
        val searchQuery = binding.searchInput.text.toString().trim()
        
        filteredRiders = allRiders.filter { rider ->
            var matchesStatus = when (currentFilter) {
                "all" -> true
                else -> rider.riderStatus == currentFilter
            }
            
            var matchesSearch = if (searchQuery.isEmpty()) {
                true
            } else {
                "${rider.firstName} ${rider.lastName}".contains(searchQuery, ignoreCase = true) ||
                    rider.email.contains(searchQuery, ignoreCase = true) ||
                    rider.plateNumber.contains(searchQuery, ignoreCase = true)
            }
            
            matchesStatus && matchesSearch
        }
        
        riderAdapter.submitList(filteredRiders)
        
        if (filteredRiders.isEmpty()) {
            binding.emptyState.visibility = View.VISIBLE
            binding.ridersRecycler.visibility = View.GONE
        } else {
            binding.emptyState.visibility = View.GONE
            binding.ridersRecycler.visibility = View.VISIBLE
        }
        
        updateStats()
    }

    private fun loadRiders() {
        lifecycleScope.launch {
            showLoading(true)
            try {
                allRiders = firebaseManager.getAllRidersWithDocuments()
                allRiders = allRiders.sortedByDescending { it.createdAt }
                filteredRiders = allRiders
                riderAdapter.submitList(filteredRiders)

                if (filteredRiders.isEmpty()) {
                    binding.emptyState.visibility = View.VISIBLE
                    binding.ridersRecycler.visibility = View.GONE
                } else {
                    binding.emptyState.visibility = View.GONE
                    binding.ridersRecycler.visibility = View.VISIBLE
                }
                
                updateStats()
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Error loading riders: ${e.message}", Snackbar.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun updateStats() {
        val totalCount = allRiders.size
        val activeCount = allRiders.count { it.riderStatus == "active" }
        val pendingCount = allRiders.count { it.riderStatus == "pending" }
        val suspendedCount = allRiders.count { it.riderStatus == "suspended" }
        
        binding.totalRiders.text = totalCount.toString()
        binding.activeRiders.text = activeCount.toString()
        binding.pendingRiders.text = pendingCount.toString()
    }

    private fun showEditRiderDialog(rider: AdminRider) {
        val dialogBinding = DialogEditRiderBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
            .setTitle("Edit Rider Details")
            .setView(dialogBinding.root)
            .setPositiveButton("Save") { _, _ ->
                saveRiderChanges(rider, dialogBinding)
            }
            .setNegativeButton("Cancel", null)
            .create()

        // Populate fields
        dialogBinding.firstNameInput.setText(rider.firstName)
        dialogBinding.lastNameInput.setText(rider.lastName)
        dialogBinding.emailInput.setText(rider.email)
        dialogBinding.phoneInput.setText(rider.phoneNumber)

        // Setup vehicle type spinner
        val vehicleTypes = listOf("Motorcycle", "Bicycle", "Car")
        val vehicleAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, vehicleTypes)
        dialogBinding.vehicleTypeSpinner.adapter = vehicleAdapter
        val vehiclePosition = vehicleTypes.indexOf(rider.vehicleType)
        if (vehiclePosition >= 0) {
            dialogBinding.vehicleTypeSpinner.setSelection(vehiclePosition)
        }

        dialogBinding.plateNumberInput.setText(rider.plateNumber)
        dialogBinding.licenseNumberInput.setText(rider.licenseNumber)

        dialog.show()
    }

    private fun saveRiderChanges(rider: AdminRider, binding: DialogEditRiderBinding) {
        val firstName = binding.firstNameInput.text.toString().trim()
        val lastName = binding.lastNameInput.text.toString().trim()
        val email = binding.emailInput.text.toString().trim()
        val phone = binding.phoneInput.text.toString().trim()
        val vehicleType = binding.vehicleTypeSpinner.selectedItem.toString()
        val plateNumber = binding.plateNumberInput.text.toString().trim()
        val licenseNumber = binding.licenseNumberInput.text.toString().trim()

        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || phone.isEmpty()) {
            Snackbar.make(this.binding.root, "Please fill all fields", Snackbar.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            showLoading(true)
            try {
                firebaseManager.updateRider(rider.id, mapOf(
                    "Rider_Fname" to firstName,
                    "Rider_Lname" to lastName,
                    "Rider_Email" to email,
                    "Rider_Phone" to phone,
                    "Rider_VehicleType" to vehicleType,
                    "Rider_PlateNumber" to plateNumber,
                    "Rider_LicenseNumber" to licenseNumber
                ))
                Snackbar.make(this@ManageRidersActivity.binding.root, "Rider updated successfully", Snackbar.LENGTH_SHORT).show()
                loadRiders()
            } catch (e: Exception) {
                Snackbar.make(this@ManageRidersActivity.binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun approveRider(rider: AdminRider) {
        AlertDialog.Builder(this)
            .setTitle("Approve Rider")
            .setMessage("Approve ${rider.firstName} ${rider.lastName} as a delivery rider?")
            .setPositiveButton("Approve") { _, _ ->
                updateRiderStatus(rider, "active", true)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun rejectRider(rider: AdminRider) {
        AlertDialog.Builder(this)
            .setTitle("Reject Application")
            .setMessage("Reject ${rider.firstName} ${rider.lastName}'s application?")
            .setPositiveButton("Reject") { _, _ ->
                updateRiderStatus(rider, "rejected", false)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun suspendRider(rider: AdminRider) {
        AlertDialog.Builder(this)
            .setTitle("Suspend Rider")
            .setMessage("Suspend ${rider.firstName} ${rider.lastName}?")
            .setPositiveButton("Suspend") { _, _ ->
                updateRiderStatus(rider, "suspended", rider.isVerified)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun activateRider(rider: AdminRider) {
        AlertDialog.Builder(this)
            .setTitle("Activate Rider")
            .setMessage("Activate ${rider.firstName} ${rider.lastName}?")
            .setPositiveButton("Activate") { _, _ ->
                updateRiderStatus(rider, "active", rider.isVerified)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun verifyDocuments(rider: AdminRider) {
        AlertDialog.Builder(this)
            .setTitle("Verify Documents")
            .setMessage("Mark ${rider.firstName} ${rider.lastName}'s documents as verified?")
            .setPositiveButton("Verify") { _, _ ->
                updateRiderStatus(rider, rider.riderStatus, true)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateRiderStatus(rider: AdminRider, status: String, verified: Boolean) {
        lifecycleScope.launch {
            showLoading(true)
            try {
                // Use the new updateRiderStatus method that updates both application and user
                val result = firebaseManager.updateRiderStatus(rider.id, status)
                if (result.isSuccess) {
                    Snackbar.make(binding.root, "Rider ${if (status == "active") "approved" else status}", Snackbar.LENGTH_SHORT).show()
                    loadRiders()
                } else {
                    Snackbar.make(binding.root, "Error: ${result.exceptionOrNull()?.message}", Snackbar.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun confirmDelete(rider: AdminRider) {
        AlertDialog.Builder(this)
            .setTitle("Delete Rider")
            .setMessage("Are you sure you want to delete ${rider.firstName} ${rider.lastName}?")
            .setPositiveButton("Delete") { _, _ ->
                deleteRider(rider)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteRider(rider: AdminRider) {
        lifecycleScope.launch {
            showLoading(true)
            try {
                firebaseManager.deleteRider(rider.id)
                Snackbar.make(binding.root, "Rider deleted", Snackbar.LENGTH_SHORT).show()
                loadRiders()
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun viewDocuments(rider: AdminRider) {
        val items = mutableListOf<Pair<String, String?>>()
        if (!rider.licensePhotoUrl.isNullOrBlank()) {
            items.add("Driver's License" to rider.licensePhotoUrl)
        }
        if (!rider.nbiUrl.isNullOrBlank()) {
            items.add("NBI Clearance" to rider.nbiUrl)
        }
        if (!rider.orUrl.isNullOrBlank()) {
            items.add("Official Receipt" to rider.orUrl)
        }
        if (!rider.crUrl.isNullOrBlank()) {
            items.add("Certificate of Registration" to rider.crUrl)
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