package com.example.pickgo.activities.customer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.example.pickgo.R
import com.example.pickgo.databinding.ActivityProfileBinding
import com.example.pickgo.models.Address
import com.example.pickgo.utils.FirebaseManager
import com.example.pickgo.utils.SessionManager
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileBinding
    private lateinit var firebaseManager: FirebaseManager
    private lateinit var sessionManager: SessionManager
    private var savedAddresses: List<Address> = emptyList()

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val imageUri = result.data?.data
            imageUri?.let {
                uploadProfileImage(it)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseManager = FirebaseManager()
        sessionManager = SessionManager(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "My Profile"

        setupClickListeners()
        loadUserData()
        loadAddresses()
    }

    private fun setupClickListeners() {
        binding.saveButton.setOnClickListener {
            saveProfileChanges()
        }

        binding.changePhotoButton.setOnClickListener {
            openImagePicker()
        }

        binding.addAddressButton.setOnClickListener {
            showAddAddressDialog()
        }

        binding.logoutButton.setOnClickListener {
            logout()
        }
    }

    private fun loadUserData() {
        lifecycleScope.launch {
            showLoading(true)
            try {
                val user = firebaseManager.getCurrentUser()
                user?.let {
                    binding.displayNameInput.setText(it.displayName ?: "")
                    binding.emailInput.setText(it.email)
                    binding.phoneInput.setText(it.phoneNumber ?: "")

                    val memberSince = java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault())
                        .format(it.createdAt)
                    binding.memberSinceText.text = "Member since $memberSince"

                    it.profilePhoto?.let { photoUrl ->
                        Glide.with(this@ProfileActivity)
                            .load(photoUrl)
                            .placeholder(R.drawable.placeholder_profile)
                            .circleCrop()
                            .into(binding.profileImage)
                    }
                }
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Error loading profile: ${e.message}", Snackbar.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun loadAddresses() {
        lifecycleScope.launch {
            try {
                val user = firebaseManager.getCurrentUser()
                user?.let {
                    savedAddresses = firebaseManager.getUserAddresses(it.id)
                    displayAddresses()
                }
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Error loading addresses: ${e.message}", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun displayAddresses() {
        val addressesContainer = binding.addressesContainer
        addressesContainer.removeAllViews()

        if (savedAddresses.isEmpty()) {
            binding.noAddressesText.visibility = View.VISIBLE
            return
        }

        binding.noAddressesText.visibility = View.GONE

        savedAddresses.forEach { address ->
            val addressCard = createAddressCard(address)
            addressesContainer.addView(addressCard)
        }
    }

    private fun createAddressCard(address: Address): View {
        val cardView = com.google.android.material.card.MaterialCardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 12
            }
            cardElevation = 2f
            radius = 12f
            setCardBackgroundColor(getColor(R.color.glass_bg))
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(16, 16, 16, 16)
        }

        val textLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val labelText = android.widget.TextView(this).apply {
            text = address.label
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(getColor(R.color.text_primary))
        }
        textLayout.addView(labelText)

        val addressText = android.widget.TextView(this).apply {
            text = "${address.addressLine1}, ${address.city}"
            textSize = 12f
            setTextColor(getColor(R.color.text_secondary))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 4 }
        }
        textLayout.addView(addressText)

        layout.addView(textLayout)

        val deleteButton = android.widget.ImageButton(this).apply {
            setImageResource(R.drawable.ic_delete)
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            setOnClickListener {
                deleteAddress(address)
            }
        }
        layout.addView(deleteButton)

        cardView.addView(layout)
        return cardView
    }

    private fun showAddAddressDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_address, null)
        val labelInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.labelInput)
        val addressInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.addressInput)
        val cityInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.cityInput)

        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Add New Address")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val label = labelInput.text.toString()
                val address = addressInput.text.toString()
                val city = cityInput.text.toString()

                if (label.isNotEmpty() && address.isNotEmpty() && city.isNotEmpty()) {
                    saveNewAddress(label, address, city)
                } else {
                    Snackbar.make(binding.root, "Please fill all fields", Snackbar.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveNewAddress(label: String, address: String, city: String) {
        lifecycleScope.launch {
            try {
                val user = firebaseManager.getCurrentUser()
                user?.let {
                    val newAddress = Address(
                        userId = it.id,
                        label = label,
                        addressLine1 = address,
                        city = city,
                        isDefault = savedAddresses.isEmpty()
                    )
                    firebaseManager.addAddress(newAddress)
                    loadAddresses()
                    Snackbar.make(binding.root, "Address added", Snackbar.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Error adding address: ${e.message}", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteAddress(address: Address) {
        lifecycleScope.launch {
            try {
                firebaseManager.deleteAddress(address.id)
                loadAddresses()
                Snackbar.make(binding.root, "Address deleted", Snackbar.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Error deleting address: ${e.message}", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveProfileChanges() {
        lifecycleScope.launch {
            try {
                val user = firebaseManager.getCurrentUser()
                user?.let {
                    val updates = mutableMapOf<String, Any>()

                    binding.displayNameInput.text.toString().takeIf { it.isNotEmpty() }?.let { displayName ->
                        updates["displayName"] = displayName
                    }

                    binding.phoneInput.text.toString().takeIf { it.isNotEmpty() }?.let { phone ->
                        updates["phoneNumber"] = phone
                    }

                    if (updates.isNotEmpty()) {
                        firebaseManager.updateUserProfile(it.id, updates)
                        Snackbar.make(binding.root, "Profile updated successfully", Snackbar.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Error updating profile: ${e.message}", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }

    private fun uploadProfileImage(imageUri: Uri) {
        lifecycleScope.launch {
            showLoading(true)
            try {
                val user = firebaseManager.getCurrentUser()
                user?.let {
                    val photoUrl = firebaseManager.uploadProfileImage(it.id, imageUri)
                    firebaseManager.updateUserProfile(it.id, mapOf("profilePhoto" to photoUrl))

                    Glide.with(this@ProfileActivity)
                        .load(photoUrl)
                        .placeholder(R.drawable.placeholder_profile)
                        .circleCrop()
                        .into(binding.profileImage)

                    Snackbar.make(binding.root, "Profile photo updated", Snackbar.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Error uploading image: ${e.message}", Snackbar.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun logout() {
        lifecycleScope.launch {
            firebaseManager.logout()
            sessionManager.clearSession()
            finishAffinity()
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