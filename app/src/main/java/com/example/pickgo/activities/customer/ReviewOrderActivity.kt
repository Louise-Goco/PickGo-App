package com.example.pickgo.activities.customer

import android.os.Bundle
import android.view.View
import android.widget.RatingBar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.example.pickgo.R
import com.example.pickgo.databinding.ActivityReviewOrderBinding
import com.example.pickgo.models.Order
import com.example.pickgo.models.Review
import com.example.pickgo.utils.FirebaseManager
import com.example.pickgo.utils.SessionManager
import kotlinx.coroutines.launch
import java.util.Date

class ReviewOrderActivity : AppCompatActivity() {
    private lateinit var binding: ActivityReviewOrderBinding
    private lateinit var firebaseManager: FirebaseManager
    private lateinit var sessionManager: SessionManager
    private var order: Order? = null
    private var orderId: String = ""
    private var selectedTip: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReviewOrderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseManager = FirebaseManager(this)
        sessionManager = SessionManager(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Rate Your Order"

        orderId = intent.getStringExtra("order_id") ?: ""

        setupTipButtons()
        loadOrder()

        binding.submitButton.setOnClickListener {
            submitReview()
        }
    }

    private fun setupTipButtons() {
        val tipButtons = listOf(
            binding.tip0 to 0,
            binding.tip20 to 20,
            binding.tip50 to 50,
            binding.tip100 to 100
        )

        tipButtons.forEach { (button, amount) ->
            button.setOnClickListener {
                selectedTip = amount
                tipButtons.forEach { (btn, _) ->
                    btn.isSelected = btn == button
                }
            }
        }
    }

    private fun loadOrder() {
        lifecycleScope.launch {
            showLoading(true)
            try {
                order = firebaseManager.getOrderById(orderId)
                order?.let {
                    displayStoreInfo(it)
                    displayRiderInfo()
                }
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Error loading order: ${e.message}", Snackbar.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun displayStoreInfo(order: Order) {
        binding.storeName.text = order.merchantName
        binding.storeRating.rating = 5f

        // Load store image
        lifecycleScope.launch {
            val merchant = firebaseManager.getMerchantById(order.merchantId)
            merchant?.let { m ->
                m.merchLogo?.let { logoUrl ->
                    Glide.with(this@ReviewOrderActivity)
                        .load(logoUrl)
                        .placeholder(R.drawable.placeholder_store)
                        .circleCrop()
                        .into(binding.storeImage)
                }
            }
        }
    }

    private fun displayRiderInfo() {
        lifecycleScope.launch {
            order?.riderId?.let { riderId ->
                val rider = firebaseManager.getRiderById(riderId)
                rider?.let { r ->
                    binding.riderName.text = "${r.firstName} ${r.lastName}"
                    binding.riderRating.rating = 5f
                    binding.riderSection.visibility = View.VISIBLE

                    // AdminRider doesn't have profile photo, use placeholder
                    Glide.with(this@ReviewOrderActivity)
                        .load(R.drawable.placeholder_profile)
                        .circleCrop()
                        .into(binding.riderImage)
                }
            }
        }
    }

    private fun submitReview() {
        lifecycleScope.launch {
            try {
                val user = firebaseManager.getCurrentUser()
                order?.let { order ->
                    user?.let { user ->
                        // Submit store review
                        val storeRating = binding.storeRating.rating.toInt()
                        val storeComment = binding.storeComment.text.toString()

                        val storeReview = Review(
                            orderId = orderId,
                            customerId = user.id,
                            sellerId = order.merchantId,
                            rating = storeRating,
                            comment = storeComment,
                            createdAt = Date()
                        )
                        firebaseManager.addReview(storeReview)

                        // Submit rider review if exists
                        if (order.riderId != null) {
                            val riderRating = binding.riderRating.rating.toInt()
                            val riderComment = binding.riderComment.text.toString()

                            val riderReview = Review(
                                orderId = orderId,
                                customerId = user.id,
                                riderId = order.riderId,
                                rating = riderRating,
                                comment = riderComment,
                                createdAt = Date()
                            )
                            firebaseManager.addReview(riderReview)

                            // Add tip if selected
                            if (selectedTip > 0) {
                                firebaseManager.addRiderTip(order.riderId!!, selectedTip, orderId)
                            }
                        }

                        Snackbar.make(binding.root, "Thank you for your feedback!", Snackbar.LENGTH_LONG).show()

                        val intent = android.content.Intent(this@ReviewOrderActivity, MyOrdersActivity::class.java)
                        intent.putExtra("review_success", "success")
                        intent.flags = android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                        startActivity(intent)
                        finish()
                    }
                }
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Error submitting review: ${e.message}", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.submitButton.isEnabled = !show
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}