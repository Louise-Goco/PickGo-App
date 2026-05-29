package com.example.pickgo.activities

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.pickgo.R
import com.example.pickgo.databinding.ActivityRiderRegisterBinding
import com.example.pickgo.models.RiderApplication
import com.example.pickgo.utils.FirebaseManager
import com.example.pickgo.utils.SessionManager
import kotlinx.coroutines.launch

class RiderRegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRiderRegisterBinding
    private lateinit var firebaseManager: FirebaseManager
    private lateinit var sessionManager: SessionManager
    
    private var licensePhotoUri: Uri? = null
    private var nbiClearanceUri: Uri? = null
    private var orUri: Uri? = null
    private var crUri: Uri? = null

    private val vehicleTypes = listOf("Motorcycle", "Bicycle", "E-Bike", "Car", "Van")

    private val licensePicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                persistUriPermission(uri)
                licensePhotoUri = uri
                binding.licenseStatus.text = "✓ Selected: ${getFileName(licensePhotoUri)}"
                binding.licenseStatus.visibility = View.VISIBLE
            }
        }
    }

    private val nbiPicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                persistUriPermission(uri)
                nbiClearanceUri = uri
                binding.nbiStatus.text = "✓ Selected: ${getFileName(nbiClearanceUri)}"
                binding.nbiStatus.visibility = View.VISIBLE
            }
        }
    }

    private val orPicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                persistUriPermission(uri)
                orUri = uri
                binding.orStatus.text = "✓ Selected: ${getFileName(orUri)}"
                binding.orStatus.visibility = View.VISIBLE
            }
        }
    }

    private val crPicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                persistUriPermission(uri)
                crUri = uri
                binding.crStatus.text = "✓ Selected: ${getFileName(crUri)}"
                binding.crStatus.visibility = View.VISIBLE
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRiderRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseManager = FirebaseManager(this)
        try {
            sessionManager = SessionManager(this)
        } catch (e: Exception) {
            // Session manager initialization failed - this is okay for registration
            android.util.Log.w("RiderRegister", "SessionManager init failed: ${e.message}")
        }
        setupVehicleTypeSpinner()
        setupClickListeners()
    }


    private fun setupVehicleTypeSpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, vehicleTypes)
        binding.vehicleTypeSpinner.setAdapter(adapter)
        binding.vehicleTypeSpinner.setText("Motorcycle", false)
    }

    private fun setupClickListeners() {
        binding.licenseUploadBtn.setOnClickListener {
            pickDocument(licensePicker, "Select Driver's License")
        }

        binding.nbiUploadBtn.setOnClickListener {
            pickDocument(nbiPicker, "Select NBI Clearance")
        }

        binding.orUploadBtn.setOnClickListener {
            pickDocument(orPicker, "Select Official Receipt (OR)")
        }

        binding.crUploadBtn.setOnClickListener {
            pickDocument(crPicker, "Select Certificate of Registration (CR)")
        }

        binding.submitBtn.setOnClickListener {
            performRegistration()
        }

        binding.signInBtn.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        binding.backHomeBtn.setOnClickListener {
            finish()
        }
    }

    private fun pickDocument(launcher: androidx.activity.result.ActivityResultLauncher<Intent>, title: String) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "application/pdf"))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        launcher.launch(intent)
    }

    private fun persistUriPermission(uri: Uri) {
        try {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) {
            // Some content providers do not support persistable permissions
        }
    }

    private fun performRegistration() {
        val firstName = binding.firstNameInput.text.toString().trim()
        val lastName = binding.lastNameInput.text.toString().trim()
        val email = binding.emailInput.text.toString().trim()
        val phone = binding.phoneInput.text.toString().trim()
        val password = binding.passwordInput.text.toString()
        val confirmPassword = binding.confirmPasswordInput.text.toString()
        val vehicleType = binding.vehicleTypeSpinner.text.toString().trim()
        val plateNumber = binding.plateNumberInput.text.toString().trim()
        val licenseNumber = binding.licenseNumberInput.text.toString().trim()
        val termsAccepted = binding.termsCheckbox.isChecked

        if (!validateInputs(firstName, lastName, email, phone, password, confirmPassword,
                vehicleType, plateNumber, licenseNumber, termsAccepted)) {
            return
        }

        binding.submitBtn.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE
        clearMessages()

        val application = RiderApplication(
            firstName = firstName,
            lastName = lastName,
            email = email,
            phoneNumber = phone,
            vehicleType = vehicleType,
            plateNumber = plateNumber,
            licenseNumber = licenseNumber
        )

        val files = mutableMapOf<String, Uri>()
        licensePhotoUri?.let { files["license_photo"] = it }
        nbiClearanceUri?.let { files["nbi"] = it }
        orUri?.let { files["or"] = it }
        crUri?.let { files["cr"] = it }

        lifecycleScope.launch {
            try {
                val result = firebaseManager.submitRiderApplication(application, files, password)
                if (result.isSuccess) {
                    showSuccess("Application submitted successfully! Please wait for approval.")
                    clearForm()
                } else {
                    showError(result.exceptionOrNull()?.message ?: "Registration failed.")
                }
            } catch (e: Exception) {
                showError("Error: ${e.message ?: "Please try again."}")
            } finally {
                binding.submitBtn.isEnabled = true
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun validateInputs(
        firstName: String, lastName: String, email: String, phone: String,
        password: String, confirmPassword: String, vehicleType: String,
        plateNumber: String, licenseNumber: String, termsAccepted: Boolean
    ): Boolean {
        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || phone.isEmpty()) {
            showError("Personal information fields are required.")
            return false
        }

        if (password.isEmpty() || confirmPassword.isEmpty()) {
            showError("Password fields are required.")
            return false
        }

        if (password != confirmPassword) {
            showError("Passwords do not match.")
            return false
        }

        if (password.length < 6) {
            showError("Password must be at least 6 characters.")
            return false
        }

        if (vehicleType.isEmpty()) {
            showError("Please select your vehicle type.")
            return false
        }

        if (plateNumber.isEmpty()) {
            showError("Plate number is required.")
            return false
        }

        if (licenseNumber.isEmpty()) {
            showError("License number is required.")
            return false
        }

        // Document uploads are now optional (Firebase Storage requires Blaze plan)
        // if (licensePhotoUri == null) {
        //     showError("Please upload your Driver's License photo.")
        //     return false
        // }

        // if (nbiClearanceUri == null) {
        //     showError("Please upload your NBI Clearance.")
        //     return false
        // }

        // if (orUri == null) {
        //     showError("Please upload your Official Receipt (OR).")
        //     return false
        // }

        // if (crUri == null) {
        //     showError("Please upload your Certificate of Registration (CR).")
        //     return false
        // }

        if (!termsAccepted) {
            showError("You must agree to the terms and conditions.")
            return false
        }

        return true
    }

    private fun showError(message: String) {
        binding.errorBox.visibility = View.VISIBLE
        binding.errorText.text = message
        binding.successBox.visibility = View.GONE
    }

    private fun showSuccess(message: String) {
        binding.successBox.visibility = View.VISIBLE
        binding.successText.text = message
        binding.errorBox.visibility = View.GONE
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun clearMessages() {
        binding.errorBox.visibility = View.GONE
        binding.successBox.visibility = View.GONE
    }

    private fun clearForm() {
        binding.firstNameInput.text?.clear()
        binding.lastNameInput.text?.clear()
        binding.emailInput.text?.clear()
        binding.phoneInput.text?.clear()
        binding.passwordInput.text?.clear()
        binding.confirmPasswordInput.text?.clear()
        binding.plateNumberInput.text?.clear()
        binding.licenseNumberInput.text?.clear()
        binding.termsCheckbox.isChecked = false
        binding.vehicleTypeSpinner.setText("Motorcycle", false)
        
        binding.licenseStatus.visibility = View.GONE
        binding.nbiStatus.visibility = View.GONE
        binding.orStatus.visibility = View.GONE
        binding.crStatus.visibility = View.GONE
        
        licensePhotoUri = null
        nbiClearanceUri = null
        orUri = null
        crUri = null
    }

    private fun getFileName(uri: Uri?): String {
        uri ?: return "Unknown"
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    return it.getString(nameIndex)
                }
            }
        }
        return "Document"
    }
}
