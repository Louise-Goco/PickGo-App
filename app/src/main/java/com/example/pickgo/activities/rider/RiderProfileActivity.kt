package com.example.pickgo.activities.rider

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.example.pickgo.R
import com.example.pickgo.databinding.ActivityRiderProfileBinding
import com.example.pickgo.utils.FirebaseManager
import com.example.pickgo.utils.SessionManager
import kotlinx.coroutines.launch

class RiderProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRiderProfileBinding
    private lateinit var firebaseManager: FirebaseManager
    private lateinit var sessionManager: SessionManager
    private var riderId: String = ""
    private var selectedPhotoUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedPhotoUri = result.data?.data
            selectedPhotoUri?.let { uri ->
                Glide.with(this).load(uri).circleCrop().into(binding.profilePhoto)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRiderProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseManager = FirebaseManager(this)
        sessionManager = SessionManager(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Profile Settings"

        setupSpinners()
        setupClickListeners()
        loadRiderData()
    }

    private fun setupSpinners() {
        val vehicleTypes = listOf("Bicycle", "Motorcycle", "Car")
        val vehicleAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, vehicleTypes)
        (binding.vehicleTypeSpinner as? android.widget.AutoCompleteTextView)?.setAdapter(vehicleAdapter)

        val banks = listOf("GCash", "Maya", "BDO Unibank", "BPI", "UnionBank", "Metrobank", "LandBank", "Security Bank", "RCBC", "PNB")
        val bankAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, banks)
        (binding.bankNameSpinner as? android.widget.AutoCompleteTextView)?.setAdapter(bankAdapter)
    }

    private fun setupClickListeners() {
        binding.changePhotoBtn.setOnClickListener {
            openImagePicker()
        }

        binding.saveButton.setOnClickListener {
            saveProfile()
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
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
                        binding.riderName.text = "${r.firstName} ${r.lastName}"
                        binding.riderEmail.text = r.email
                        (binding.vehicleTypeSpinner as? android.widget.AutoCompleteTextView)?.setText(r.vehicleType, false)
                        binding.plateNumberInput.setText(r.plateNumber)
                        (binding.bankNameSpinner as? android.widget.AutoCompleteTextView)?.setText(r.bankName, false)
                        binding.accountNumberInput.setText(r.bankAccountNumber)
                        binding.accountNameInput.setText(r.bankAccountName)

                        r.riderPhoto?.let { photoUrl ->
                            Glide.with(this@RiderProfileActivity)
                                .load(photoUrl.toString())
                                .placeholder(R.drawable.placeholder_profile)
                                .circleCrop()
                                .into(binding.profilePhoto)
                        }
                    }
                }
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Error loading profile: ${e.message}", Snackbar.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun saveProfile() {
        val vehicleType = (binding.vehicleTypeSpinner as? android.widget.AutoCompleteTextView)?.text.toString()
        val plateNumber = binding.plateNumberInput.text.toString().trim()
        val bankName = (binding.bankNameSpinner as? android.widget.AutoCompleteTextView)?.text.toString()
        val accountNumber = binding.accountNumberInput.text.toString().trim()
        val accountName = binding.accountNameInput.text.toString().trim()

        if (vehicleType.isEmpty()) {
            Snackbar.make(binding.root, "Please select vehicle type", Snackbar.LENGTH_SHORT).show()
            return
        }

        if (plateNumber.isEmpty()) {
            Snackbar.make(binding.root, "Please enter plate number", Snackbar.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            showLoading(true)
            try {
                val updates = mutableMapOf<String, Any>(
                    "vehicleType" to vehicleType,
                    "plateNumber" to plateNumber,
                    "bankName" to bankName,
                    "bankAccountNumber" to accountNumber,
                    "bankAccountName" to accountName
                )

                if (selectedPhotoUri != null) {
                    val photoUrl = firebaseManager.uploadRiderPhoto(riderId, selectedPhotoUri!!)
                    updates["riderPhoto"] = photoUrl
                }

                firebaseManager.updateRider(riderId, updates)
                Snackbar.make(binding.root, "Profile updated successfully", Snackbar.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.saveButton.isEnabled = !show
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}