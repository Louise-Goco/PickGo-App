package com.example.pickgo.activities.seller

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.example.pickgo.R
import com.example.pickgo.adapters.seller.ReviewAdapter
import com.example.pickgo.databinding.ActivitySellerReviewsBinding
import com.example.pickgo.models.seller.SellerReview
import com.example.pickgo.utils.FirebaseManager
import com.example.pickgo.utils.SessionManager
import kotlinx.coroutines.launch

class SellerReviewsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySellerReviewsBinding
    private lateinit var firebaseManager: FirebaseManager
    private lateinit var sessionManager: SessionManager
    private lateinit var storeReviewAdapter: ReviewAdapter
    private lateinit var productReviewAdapter: ReviewAdapter
    private var sellerId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySellerReviewsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseManager = FirebaseManager(this)
        sessionManager = SessionManager(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Customer Reviews"

        setupRecyclerViews()
        loadSellerData()
    }

    private fun setupRecyclerViews() {
        storeReviewAdapter = ReviewAdapter()
        productReviewAdapter = ReviewAdapter()

        binding.storeReviewsRecycler.layoutManager = LinearLayoutManager(this)
        binding.storeReviewsRecycler.adapter = storeReviewAdapter

        binding.productReviewsRecycler.layoutManager = LinearLayoutManager(this)
        binding.productReviewsRecycler.adapter = productReviewAdapter
    }

    private fun loadSellerData() {
        lifecycleScope.launch {
            showLoading(true)
            try {
                val user = firebaseManager.getCurrentUser()
                val seller = user?.let { firebaseManager.getSellerByEmail(it.email) }
                sellerId = seller?.id ?: return@launch

                loadReviews()
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Error loading data: ${e.message}", Snackbar.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun loadReviews() {
        lifecycleScope.launch {
            try {
                val reviews = firebaseManager.getSellerReviews(sellerId)

                // Enrich reviews with customer names and item names
                val enrichedReviews = reviews.map { review ->
                    val customer = firebaseManager.getUserById(review.customerId)
                    val itemName = if (review.itemId != null) {
                        firebaseManager.getItemById(review.itemId)?.itemName
                    } else null

                    review.copy(
                        customerName = "${customer?.firstName ?: ""} ${customer?.lastName ?: ""}".trim(),
                        itemName = itemName
                    )
                }

                val storeReviews = enrichedReviews.filter { it.itemId == null }
                val productReviews = enrichedReviews.filter { it.itemId != null }

                binding.storeReviewsTitle.text = "Store Experience (${storeReviews.size})"
                binding.productReviewsTitle.text = "Product Feedback (${productReviews.size})"

                storeReviewAdapter.submitList(storeReviews)
                productReviewAdapter.submitList(productReviews)

                // Update rating banner
                updateRatingBanner(reviews)
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Error loading reviews: ${e.message}", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateRatingBanner(reviews: List<SellerReview>) {
        if (reviews.isEmpty()) {
            binding.avgRating.text = "0.0"
            binding.totalReviewsText.text = "Based on 0 total reviews"
            return
        }

        val avgRating = reviews.map { it.rating }.average()
        binding.avgRating.text = String.format("%.1f", avgRating)
        binding.totalReviewsText.text = "Based on ${reviews.size} total reviews"

        // Update star display
        binding.starRating.removeAllViews()
        val fullStars = avgRating.toInt()
        val hasHalfStar = avgRating - fullStars >= 0.5

        for (i in 1..5) {
            val star = android.widget.ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(32, 32)
                setImageResource(
                    when {
                        i <= fullStars -> R.drawable.ic_star_filled
                        i == fullStars + 1 && hasHalfStar -> R.drawable.ic_star_half
                        else -> R.drawable.ic_star_empty
                    }
                )
            }
            binding.starRating.addView(star)
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