package com.example.pickgo.activities.admin

import android.os.Bundle
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.example.pickgo.adapters.admin.CustomerAdapter
import com.example.pickgo.databinding.ActivityManageCustomersBinding
import com.example.pickgo.databinding.DialogAddCustomerBinding
import com.example.pickgo.models.admin.AdminCustomer
import com.example.pickgo.utils.FirebaseManager
import com.example.pickgo.utils.PasswordHasher
import com.example.pickgo.utils.SessionManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ManageCustomersActivity : AppCompatActivity() {
    private lateinit var binding: ActivityManageCustomersBinding
    private lateinit var firebaseManager: FirebaseManager
    private lateinit var sessionManager: SessionManager
    private lateinit var customerAdapter: CustomerAdapter
    private var allCustomers: List<AdminCustomer> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageCustomersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseManager = FirebaseManager(this)
        sessionManager = SessionManager(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Customer Management"

        setupRecyclerView()
        setupSearch()
        loadCustomers()

        binding.addCustomerBtn.setOnClickListener {
            showAddCustomerDialog()
        }
    }

    private fun setupRecyclerView() {
        customerAdapter = CustomerAdapter(
            onEdit = { customer ->
                showEditCustomerDialog(customer)
            },
            onDelete = { customer ->
                confirmDelete(customer)
            },
            onSuspend = { customer ->
                updateCustomerStatus(customer, "suspended")
            },
            onActivate = { customer ->
                updateCustomerStatus(customer, "active")
            }
        )
        binding.customersRecycler.layoutManager = LinearLayoutManager(this)
        binding.customersRecycler.adapter = customerAdapter
    }

    private fun setupSearch() {
        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterCustomers(s.toString())
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun loadCustomers() {
        lifecycleScope.launch {
            showLoading(true)
            try {
                allCustomers = firebaseManager.getAllCustomers()
                allCustomers = allCustomers.sortedByDescending { it.createdAt }
                customerAdapter.submitList(allCustomers)

                if (allCustomers.isEmpty()) {
                    binding.emptyState.visibility = View.VISIBLE
                    binding.customersRecycler.visibility = View.GONE
                } else {
                    binding.emptyState.visibility = View.GONE
                    binding.customersRecycler.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Error loading customers: ${e.message}", Snackbar.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun filterCustomers(query: String) {
        if (query.isEmpty()) {
            customerAdapter.submitList(allCustomers)
            return
        }
        val filtered = allCustomers.filter {
            "${it.firstName} ${it.lastName}".contains(query, ignoreCase = true) ||
                    it.email.contains(query, ignoreCase = true)
        }
        customerAdapter.submitList(filtered)
    }

    private fun showAddCustomerDialog() {
        val dialogBinding = DialogAddCustomerBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setCancelable(true)
            .create()

        dialogBinding.dialogTitle.text = "Add New Customer"
        dialogBinding.passwordLayout.visibility = View.VISIBLE

        dialogBinding.submitBtn.setOnClickListener {
            val firstName = dialogBinding.firstNameInput.text.toString().trim()
            val lastName = dialogBinding.lastNameInput.text.toString().trim()
            val email = dialogBinding.emailInput.text.toString().trim()
            val phone = dialogBinding.phoneInput.text.toString().trim()
            val password = dialogBinding.passwordInput.text.toString()

            if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty()) {
                Snackbar.make(binding.root, "Please fill all fields", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                showLoading(true)
                try {
                    val hashedPassword = PasswordHasher.hashPassword(password)
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    val customer = AdminCustomer(
                        firstName = firstName,
                        lastName = lastName,
                        email = email,
                        phoneNumber = phone,
                        accountStatus = "active",
                        isVerified = false,
                        createdAt = dateFormat.format(Date())
                    )
                    firebaseManager.createCustomer(customer, hashedPassword)
                    Snackbar.make(binding.root, "Customer created successfully", Snackbar.LENGTH_SHORT).show()
                    dialog.dismiss()
                    loadCustomers()
                } catch (e: Exception) {
                    Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
                } finally {
                    showLoading(false)
                }
            }
        }

        dialog.show()
    }

    private fun showEditCustomerDialog(customer: AdminCustomer) {
        val dialogBinding = DialogAddCustomerBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setCancelable(true)
            .create()

        dialogBinding.dialogTitle.text = "Edit Customer"
        dialogBinding.passwordLayout.visibility = View.GONE
        dialogBinding.firstNameInput.setText(customer.firstName)
        dialogBinding.lastNameInput.setText(customer.lastName)
        dialogBinding.emailInput.setText(customer.email)
        dialogBinding.phoneInput.setText(customer.phoneNumber)

        dialogBinding.submitBtn.setOnClickListener {
            val firstName = dialogBinding.firstNameInput.text.toString().trim()
            val lastName = dialogBinding.lastNameInput.text.toString().trim()
            val email = dialogBinding.emailInput.text.toString().trim()
            val phone = dialogBinding.phoneInput.text.toString().trim()

            if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || phone.isEmpty()) {
                Snackbar.make(binding.root, "Please fill all fields", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                showLoading(true)
                try {
                    firebaseManager.updateCustomer(customer.id, mapOf(
                        "firstName" to firstName,
                        "lastName" to lastName,
                        "email" to email,
                        "phoneNumber" to phone
                    ))
                    Snackbar.make(binding.root, "Customer updated successfully", Snackbar.LENGTH_SHORT).show()
                    dialog.dismiss()
                    loadCustomers()
                } catch (e: Exception) {
                    Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
                } finally {
                    showLoading(false)
                }
            }
        }

        dialog.show()
    }

    private fun updateCustomerStatus(customer: AdminCustomer, newStatus: String) {
        lifecycleScope.launch {
            showLoading(true)
            try {
                firebaseManager.updateCustomer(customer.id, mapOf("accountStatus" to newStatus))
                Snackbar.make(binding.root, "Customer ${if (newStatus == "active") "activated" else "suspended"}", Snackbar.LENGTH_SHORT).show()
                loadCustomers()
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun confirmDelete(customer: AdminCustomer) {
        AlertDialog.Builder(this)
            .setTitle("Delete Customer")
            .setMessage("Are you sure you want to delete ${customer.firstName} ${customer.lastName}?")
            .setPositiveButton("Delete") { _, _ ->
                deleteCustomer(customer)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteCustomer(customer: AdminCustomer) {
        lifecycleScope.launch {
            showLoading(true)
            try {
                firebaseManager.deleteCustomer(customer.id)
                Snackbar.make(binding.root, "Customer deleted", Snackbar.LENGTH_SHORT).show()
                loadCustomers()
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