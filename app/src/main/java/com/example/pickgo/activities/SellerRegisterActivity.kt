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
import com.example.pickgo.databinding.ActivitySellerRegisterBinding
import com.example.pickgo.models.SellerApplication
import com.example.pickgo.utils.FirebaseManager
import com.example.pickgo.utils.SessionManager
import kotlinx.coroutines.launch

class SellerRegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySellerRegisterBinding
    private lateinit var firebaseManager: FirebaseManager
    private lateinit var sessionManager: SessionManager
    
    private var govIdUri: Uri? = null
    private var birCertUri: Uri? = null
    
    // City and Barangay data
    private val cityBarangayMap = mapOf(
        "Cebu City" to listOf(
            "Adlaon", "Agus", "Apolo", "Babag", "Bacayan", "Banilad", "Basak",
            "Binaliw", "Bonifacio", "Budla-an", "Buhisan", "Busay", "Calamba",
            "Cambinocot", "Capitol Site", "Carreta", "Central", "Cogon Pardo",
            "Day-as", "Duljo-Fatima", "Ernesto A. Borromeo Sr.", "Guadalupe",
            "Hipodromo", "Inayawan", "Kalunasan", "Kasambagan", "Labangon",
            "Lahug", "Lorega San Miguel", "Luz", "Mabini", "Mabolo", "Malubog",
            "Mambaling", "Pamutan", "Pangdan", "Pardo", "Pasil", "Pit-os",
            "Poblacion", "Pulot", "Punta Princesa", "Sambag 1", "Sambag 2",
            "San Nicolas Norte", "San Nicolas Sur", "Santa Cruz", "Sapangdako",
            "Sirao", "Suba", "Sudlon 1", "Sudlon 2", "Tabunok", "T. Padilla",
            "Tejero", "Tinago", "To-ong", "Zapatera"
        )
    )

    private val govIdPicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            govIdUri = result.data?.data
            binding.govIdStatus.text = "✓ Selected: ${getFileName(govIdUri)}"
            binding.govIdStatus.visibility = View.VISIBLE
        }
    }

    private val birCertPicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            birCertUri = result.data?.data
            binding.birCertStatus.text = "✓ Selected: ${getFileName(birCertUri)}"
            binding.birCertStatus.visibility = View.VISIBLE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySellerRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseManager = FirebaseManager()
        sessionManager = SessionManager(this)
        setupDropdowns()
        setupClickListeners()
        autofillUserData()
    }
    
    private fun autofillUserData() {
        val currentUser = sessionManager.getSession()
        if (currentUser != null) {
            binding.firstNameInput.setText(currentUser.firstName)
            binding.lastNameInput.setText(currentUser.lastName)
            binding.emailInput.setText(currentUser.email)
            binding.phoneInput.setText(currentUser.phoneNumber)
        }
    }

    private fun setupDropdowns() {
        // Setup City Dropdown
        val cities = cityBarangayMap.keys.toList()
        val cityAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, cities)
        binding.cityInput.setAdapter(cityAdapter)
        
        // Set default to Cebu City
        if (cities.contains("Cebu City")) {
            binding.cityInput.setText("Cebu City", false)
            updateBarangayDropdown("Cebu City")
        }
        
        // Listen for city selection changes
        binding.cityInput.setOnItemClickListener { parent, view, position, id ->
            val selectedCity = parent.getItemAtPosition(position).toString()
            updateBarangayDropdown(selectedCity)
        }
    }
    
    private fun updateBarangayDropdown(city: String) {
        val barangays = cityBarangayMap[city] ?: emptyList()
        val barangayAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, barangays)
        binding.barangayInput.setAdapter(barangayAdapter)
        binding.barangayInput.setText("", false) // Clear previous selection
    }

    private fun setupClickListeners() {
        binding.govIdUploadBtn.setOnClickListener {
            pickDocument(govIdPicker, "Select Government ID")
        }

        binding.birCertUploadBtn.setOnClickListener {
            pickDocument(birCertPicker, "Select BIR Certificate")
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
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "application/pdf"))
        }
        launcher.launch(Intent.createChooser(intent, title))
    }

    private fun performRegistration() {
        val firstName = binding.firstNameInput.text.toString().trim()
        val lastName = binding.lastNameInput.text.toString().trim()
        val email = binding.emailInput.text.toString().trim()
        val phone = binding.phoneInput.text.toString().trim()
        val password = binding.passwordInput.text.toString()
        val confirmPassword = binding.confirmPasswordInput.text.toString()
        val storeName = binding.storeNameInput.text.toString().trim()
        val storeType = binding.storeTypeInput.text.toString().trim()
        val storePhone = binding.storePhoneInput.text.toString().trim()
        val storeEmail = binding.storeEmailInput.text.toString().trim()
        val city = binding.cityInput.text.toString().trim()
        val barangay = binding.barangayInput.text.toString().trim()
        val streetName = binding.streetNameInput.text.toString().trim()
        val termsAccepted = binding.termsCheckbox.isChecked

        if (!validateInputs(firstName, lastName, email, phone, password, confirmPassword, 
                storeName, storeType, storePhone, city, barangay, streetName, termsAccepted)) {
            return
        }

        binding.submitBtn.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE
        clearMessages()

        val application = SellerApplication(
            firstName = firstName,
            lastName = lastName,
            email = email,
            phoneNumber = phone,
            storeName = storeName,
            storeType = storeType,
            storePhone = storePhone,
            storeEmail = storeEmail,
            city = city,
            barangay = barangay,
            streetName = streetName
        )

        val files = mutableMapOf<String, Uri>()
        govIdUri?.let { files["gov_id"] = it }
        birCertUri?.let { files["bir_cert"] = it }

        lifecycleScope.launch {
            try {
                val result = firebaseManager.submitSellerApplication(application, files)
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
        password: String, confirmPassword: String, storeName: String,
        storeType: String, storePhone: String, city: String,
        barangay: String, streetName: String, termsAccepted: Boolean
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

        if (storeName.isEmpty() || storeType.isEmpty() || storePhone.isEmpty()) {
            showError("Store information fields are required.")
            return false
        }

        if (city.isEmpty() || barangay.isEmpty() || streetName.isEmpty()) {
            showError("Store address fields are required.")
            return false
        }

        if (govIdUri == null) {
            showError("Please upload a valid Government ID.")
            return false
        }

        if (birCertUri == null) {
            showError("Please upload your BIR Certificate.")
            return false
        }

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
        binding.storeNameInput.text?.clear()
        binding.storeTypeInput.text?.clear()
        binding.storePhoneInput.text?.clear()
        binding.storeEmailInput.text?.clear()
        binding.cityInput.setText("", false)
        binding.barangayInput.setText("", false)
        binding.streetNameInput.text?.clear()
        binding.termsCheckbox.isChecked = false
        binding.govIdStatus.visibility = View.GONE
        binding.birCertStatus.visibility = View.GONE
        govIdUri = null
        birCertUri = null
        
        // Reset dropdowns to default
        if (cityBarangayMap.keys.contains("Cebu City")) {
            binding.cityInput.setText("Cebu City", false)
            updateBarangayDropdown("Cebu City")
        }
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
