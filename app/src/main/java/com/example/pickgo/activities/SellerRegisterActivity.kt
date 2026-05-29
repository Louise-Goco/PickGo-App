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
    
    // City and Barangay data - Cebu Province
    private val cityBarangayMap = mapOf(
        // Cebu City (80 barangays)
        "Cebu City" to listOf(
            "Adlaon", "Agus", "Apolo", "Babag", "Bacayan", "Banilad", "Basak Pardo",
            "Basak", "Binaliw", "Bonifacio", "Budla-an", "Buhisan", "Busay", "Calamba",
            "Cambinocot", "Capitol Site", "Carreta", "Central", "Cogon Pardo",
            "Duljo-Fatima", "Ernesto A. Borromeo Sr.", "Guadalupe",
            "Hipodromo", "Inayawan", "Kalunasan", "Kasambagan", "Labangon",
            "Lahug", "Lorega San Miguel", "Luz", "Mabini", "Mabolo", "Malubog",
            "Mambaling", "Pamutan", "Pangdan", "Pardo", "Pasil", "Pit-os",
            "Poblacion", "Pulot", "Punta Princesa", "Sambag 1", "Sambag 2",
            "San Nicolas Norte", "San Nicolas Sur", "Santa Cruz", "Sapangdako",
            "Sirao", "Suba", "Sudlon 1", "Sudlon 2", "Tabunok", "T. Padilla",
            "Tejero", "Tinago", "To-ong", "Zapatera"
        ),
        
        // Lapu-Lapu City (30 barangays)
        "Lapu-Lapu City" to listOf(
            "Alturas", "Babag 1", "Babag 2", "Bankal", "Basak", "Caunisan",
            "Poblacion", "Pooc", "Pajac", "Maribago", "Mactan", "Looc",
            "Ibabao", "Gun-ob", "Canjulio", "Calawisan", "Buaya", "Subabasbas",
            "Tungasan", "San Vicente", "Santa Rosa", "Sutukil", "Taglime",
            "Tagalog", "Pusok", "Hagnaya", "Cabungahan", "Cansaga",
            "Olango", "Sabang", "Baring"
        ),
        
        // Mandaue City (27 barangays)
        "Mandaue City" to listOf(
            "Alang-alang", "Bakilid", "Bantayan", "Basak", "Cabancalan",
            "Cambaro", "Cansaga", "Carpenter", "Casuntingan", "Dumlog",
            "Ibo", "Jagobiao", "Lahug", "Looc", "Maguikay", "Mandapat",
            "Paknaan", "Pangan-an", "Puntod", "Subangdaku", "Tabok",
            "Tangub", "Tingo", "Tunggo-an", "Umapad", "Opon"
        ),
        
        // Toledo City (44 barangays)
        "Toledo City" to listOf(
            "Antipolo", "Bato", "Biga", "Bubog", "Cabitoonan", "Calongcalong",
            "Cambul-ot", "Camp 8", "Cana-og", "Carmen", "Daanglungsod",
            "Gen. Climaco Sr.", "Ibo", "Ingas", "Laboe", "Luray II",
            "Malubog", "Matab-ang", "Media Once", "Panam-atan", "Poblacion",
            "Punto Diyota", "Puting-bato", "Sagay", "Samak", "Talisay",
            "Tubod", "Vega", "Luray I", "Camp IV", "Don Andres Soriano",
            "Malubog", "Media Onse", "Pancil", "Poblacion",
            "Putong", "Talisay", "Trillana", "Upper Luray"
        ),
        
        // Danao City (38 barangays)
        "Danao City" to listOf(
            "Anonang", "Bakid", "Bancasan", "Bayabas", "Cabatbatan",
            "Cagat", "Calo", "Casilak", "Dalingding", "Danao",
            "Dapitong", "Guinsay", "Lamac", "Langub", "Laya",
            "Lindogon", "Magtagloma", "Malay", "Manduao", "Masaba",
            "Maslog", "North Poblacion", "Obong", "Obo", "Punan",
            "Quisol", "Sabang", "Sacsac", "San Jose", "Santa Rosa",
            "Sibacao", "Tagbobonga", "Taytay", "Tuba", "Tugbong",
            "Tungkop", "Wards Poblacion", "East Poblacion"
        ),
        
        // Naga City (22 barangays)
        "Naga City" to listOf(
            "Central Poblacion", "Colon", "East Poblacion", "Inoburan",
            "Inayagan", "Jagumiao", "Lanas", "Lutacan", "Mainit Poblacion",
            "Mayabong", "Naad", "New Balirong", "Old Balirong", "Patag",
            "Poblacion", "Tangke", "Tapon", "Tayasan", "Upper Suba",
            "West Poblacion", "San Isidro", "Zapatera"
        ),
        
        // Talisay City (35 barangays)
        "Talisay City" to listOf(
            "Bulacao", "Biasong", "Brgy. 1", "Brgy. 2", "Brgy. 3", "Brgy. 4",
            "Brgy. 5", "Brgy. 6", "Brgy. 7", "Brgy. 8", "Brgy. 9", "Brgy. 10",
            "Calindagan", "Camp 7", "Camp IV", "Cansojong", "Dumlog",
            "Jaena", "Lagtang", "Linao", "Maghaway", "Mindanao",
            "Mohon", "Poblacion", "Pooc", "Saan", "Tabunok",
            "Tangke", "Tapul", "Tiga-ub", "Bolobolo", "Cabfloat",
            "Calajo-an", "Canlumampao", "Sulpa"
        ),
        
        // Consolacion
        "Consolacion" to listOf(
            "Cansaga", "Casa", "Curry", "Dungga", "Dunggo-an",
            "Gallego", "Juagdan", "Lamac", "Lanang", "Nangka",
            "Panoypoys", "Poblacion", "Quezon", "Sacsac", "Tayud",
            "Tingub", "Tugpa", "Cabangahan", "Alang-alang"
        ),
        
        // Liloan
        "Liloan" to listOf(
            "Bakid", "Bonbon", "Brgy. 1", "Brgy. 2", "Brgy. 3", "Brgy. 4",
            "Brgy. 5", "Brgy. 6", "Brgy. 7", "Brgy. 8", "Cabundihan",
            "Catarman", "Cotcoton", "Jubay", "San Roque", "Santa Elena",
            "Yati", "Tablan", "Hernandez"
        ),
        
        // Compostela
        "Compostela" to listOf(
            "Compostela Poblacion", "Anapog", "Bagalnga", "Buanoy",
            "Cabadiangan", "Cambayoboan", "Canamucan", "Dapdap",
            "Lupa", "Mangiliol", "Panadtagan", "Pandacan", "Poblacion",
            "Tugas", "Tungaan", "Inoboran"
        ),
        
        // Cordova
        "Cordova" to listOf(
            "Cogon", "Dayhagon", "Gilutongan", "Ibabao",
            "Pier Barracks", "Poblacion", "San Miguel", "Santa Rosa",
            "Sawang", "Balingasag", "Evangelista"
        ),
        
        // Minglanilla
        "Minglanilla" to listOf(
            "Baking", "Cebu Technology Park", "Guindarohan", "Labangon",
            "Linao", "Poblacion", "Tunasan", "Tupaz", "Simala",
            "Calajoan", "Camp 8", "Colony", "Curva", "Jaclupan",
            "Lagtang", "Mambaling", "Poblacion", "Tibolo"
        ),
        
        // San Fernando
        "San Fernando" to listOf(
            "Balumbag", "Bato", "Biang", "Buhing", "Bulacao",
            "Calaboon", "Langaon", "Liburon", "Liguim", "Maglano",
            "Maigang", "Manguit", "Poblacion", "Pongol", "Pulgas",
            "Tabionan", "Tres de Mayo", "Tunga"
        ),
        
        // Carcar City
        "Carcar City" to listOf(
            "Boloon", "Bulacao", "Calidñgan", "Cansojong", "Guadalupe",
            "Jugan", "Liburon", "Napo", "Poblacion 1", "Poblacion 2",
            "Poblacion 3", "Tubod", "Valencia", "Ocana", "Bulak",
            "Masterson", "Taytayan"
        ),
        
        // Argao
        "Argao" to listOf(
            "Actin Poblacion", "Anajao", "Apoloy", "Balogo", "Balon",
            "Bitoon", "Bulasa", "Cagat", "Calaangan", "Canbalili",
            "Candugay", "Can-aga", "Canbanua", "Cansubay", "Colawin",
            "Conde", "Dagatan", "Danao", "Dugyan", "Guiwang",
            "Lagtang", "Lamacan", "Langub", "Luy-a", "Manlapay",
            "Poblacion", "Sabang", "Sakay", "Sulpa", "Tonggo",
            "Trenze", "Upper Actin"
        ),
        
        // Bogo City
        "Bogo City" to listOf(
            "Anonang", "Bantigue", "Binabag", "Bun-ot", "Buntay",
            "Cangmating", "Cansaga", "Dactan", "Gairan", "Guadalupe",
            "La Paz", "La Castellana", "Liburon", "Luyang", "Malingon",
            "Marangog", "Nasunog-an", "Odiong", "Odlotan", "Poblacion",
            "Putat", "San Vicente", "Santo Niño", "Tangub", "Tolotolo"
        )
    )
    
    // Store Type options
    private val storeTypes = listOf(
        "Restaurant",
        "Fast Food",
        "Cafe",
        "Bakery"
    )

    private val govIdPicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                persistUriPermission(uri)
                govIdUri = uri
                binding.govIdStatus.text = "✓ Selected: ${getFileName(govIdUri)}"
                binding.govIdStatus.visibility = View.VISIBLE
            }
        }
    }

    private val birCertPicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                persistUriPermission(uri)
                birCertUri = uri
                binding.birCertStatus.text = "✓ Selected: ${getFileName(birCertUri)}"
                binding.birCertStatus.visibility = View.VISIBLE
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySellerRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseManager = FirebaseManager(this)
        try {
            sessionManager = SessionManager(this)
        } catch (e: Exception) {
            // Session manager initialization failed - this is okay for registration
            android.util.Log.w("SellerRegister", "SessionManager init failed: ${e.message}")
        }
        setupDropdowns()
        setupClickListeners()
    }


    private fun setupDropdowns() {
        // Setup Store Type Dropdown
        val storeTypeAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, storeTypes)
        binding.storeTypeInput.setAdapter(storeTypeAdapter)
        
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
                val result = firebaseManager.submitSellerApplication(application, files, password)
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

        // Document uploads are now optional (Firebase Storage requires Blaze plan)
        // if (govIdUri == null) {
        //     showError("Please upload a valid Government ID.")
        //     return false
        // }

        // if (birCertUri == null) {
        //     showError("Please upload your BIR Certificate.")
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
        binding.storeNameInput.text?.clear()
        binding.storeTypeInput.setText("", false)
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
