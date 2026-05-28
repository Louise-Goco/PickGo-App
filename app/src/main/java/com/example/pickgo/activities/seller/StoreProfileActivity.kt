package com.example.pickgo.activities.seller

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.example.pickgo.R
import com.example.pickgo.databinding.ActivityStoreProfileBinding
import com.example.pickgo.utils.FirebaseManager
import com.example.pickgo.utils.SessionManager
import kotlinx.coroutines.launch

class StoreProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStoreProfileBinding
    private lateinit var firebaseManager: FirebaseManager
    private lateinit var sessionManager: SessionManager
    private var merchantId: String = ""
    private var selectedLogoUri: Uri? = null
    private var selectedBannerUri: Uri? = null
    private var existingLogoUrl: String? = null
    private var existingBannerUrl: String? = null

    private val pickLogoLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedLogoUri = result.data?.data
            selectedLogoUri?.let { uri ->
                Glide.with(this).load(uri).into(binding.logoPreview)
            }
        }
    }

    private val pickBannerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedBannerUri = result.data?.data
            selectedBannerUri?.let { uri ->
                Glide.with(this).load(uri).into(binding.bannerPreview)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStoreProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseManager = FirebaseManager()
        sessionManager = SessionManager(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Store Profile"

        setupClickListeners()
        loadStoreData()
    }

    private fun setupClickListeners() {
        binding.changeLogoBtn.setOnClickListener {
            openLogoPicker()
        }

        binding.changeBannerBtn.setOnClickListener {
            openBannerPicker()
        }

        binding.updateProfileBtn.setOnClickListener {
            updateProfile()
        }
    }

    private fun openLogoPicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickLogoLauncher.launch(intent)
    }

    private fun openBannerPicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickBannerLauncher.launch(intent)
    }

    private fun loadStoreData() {
        lifecycleScope.launch {
            showLoading(true)
            try {
                val user = firebaseManager.getCurrentUser()
                val seller = user?.let { firebaseManager.getSellerByEmail(it.email) }
                val merchant = seller?.let { firebaseManager.getMerchantById(it.merchantId) }

                merchant?.let { m ->
                    merchantId = m.id
                    existingLogoUrl = m.merchLogo
                    existingBannerUrl = m.merchBanner

                    binding.storeNameInput.setText(m.merchName)
                    binding.descriptionInput.setText(m.merchDescription)
                    binding.openingTimeInput.setText(m.merchOpeningTime)
                    binding.closingTimeInput.setText(m.merchClosingTime)
                    // Delivery range field doesn't exist in Merchant model
                    // binding.deliveryRangeInput.setText(m.merchDeliveryRange.toString())

                    m.merchLogo?.let { logoUrl ->
                        Glide.with(this@StoreProfileActivity)
                            .load(logoUrl)
                            .placeholder(R.drawable.placeholder_store)
                            .into(binding.logoPreview)
                    }

                    m.merchBanner?.let { bannerUrl ->
                        Glide.with(this@StoreProfileActivity)
                            .load(bannerUrl)
                            .placeholder(R.drawable.placeholder_store)
                            .into(binding.bannerPreview)
                    }
                }
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Error loading store data: ${e.message}", Snackbar.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun updateProfile() {
        val storeName = binding.storeNameInput.text.toString().trim()
        val description = binding.descriptionInput.text.toString().trim()
        val openingTime = binding.openingTimeInput.text.toString().trim()
        val closingTime = binding.closingTimeInput.text.toString().trim()
        // Delivery range field doesn't exist in layout
        // val deliveryRangeStr = binding.deliveryRangeInput.text.toString().trim()

        if (storeName.isEmpty()) {
            Snackbar.make(binding.root, "Please enter store name", Snackbar.LENGTH_SHORT).show()
            return
        }

        // val deliveryRange = deliveryRangeStr.toIntOrNull() ?: 5

        lifecycleScope.launch {
            showLoading(true)
            try {
                var logoUrl = existingLogoUrl
                var bannerUrl = existingBannerUrl

                if (selectedLogoUri != null) {
                    logoUrl = firebaseManager.uploadMerchantLogo(merchantId, selectedLogoUri!!)
                }

                if (selectedBannerUri != null) {
                    bannerUrl = firebaseManager.uploadMerchantBanner(merchantId, selectedBannerUri!!)
                }

                firebaseManager.updateMerchantProfile(
                    merchantId,
                    mapOf<String, Any>(
                        "merchName" to storeName,
                        "merchDescription" to description,
                        "merchOpeningTime" to openingTime,
                        "merchClosingTime" to closingTime,
                        "merchLogo" to (logoUrl ?: ""),
                        "merchBanner" to (bannerUrl ?: "")
                    )
                )

                Snackbar.make(binding.root, "Store profile updated successfully!", Snackbar.LENGTH_SHORT).show()

                // Reset selected images
                selectedLogoUri = null
                selectedBannerUri = null
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Error updating profile: ${e.message}", Snackbar.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.updateProfileBtn.isEnabled = !show
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}