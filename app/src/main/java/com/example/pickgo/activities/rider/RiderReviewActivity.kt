package com.example.pickgo.activities.rider

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.example.pickgo.R
import com.example.pickgo.adapters.rider.ReviewAdapter
import com.example.pickgo.databinding.ActivityRiderReviewsBinding
import com.example.pickgo.utils.FirebaseManager
import com.example.pickgo.utils.SessionManager
import kotlinx.coroutines.launch

class RiderReviewsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRiderReviewsBinding
    private lateinit var firebaseManager: FirebaseManager
    private lateinit var sessionManager: SessionManager
    private lateinit var reviewAdapter: ReviewAdapter
    private var riderId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRiderReviewsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseManager = FirebaseManager(this)
        sessionManager = SessionManager(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "My Ratings & Reviews"

        setupRecyclerView()
        loadReviews()
    }

    private fun setupRecyclerView() {
        reviewAdapter = ReviewAdapter()
        binding.reviewsRecycler.layoutManager = LinearLayoutManager(this)
        binding.reviewsRecycler.adapter = reviewAdapter
    }

    private fun loadReviews() {
        lifecycleScope.launch {
            showLoading(true)
            try {
                val user = firebaseManager.getCurrentUser()
                user?.let {
                    val rider = firebaseManager.getRiderByEmail(it.email)
                    rider?.let { r ->
                        riderId = r.id
                        val reviews = firebaseManager.getRiderReviews(riderId)

                        if (reviews.isEmpty()) {
                            binding.emptyState.visibility = View.VISIBLE
                            binding.reviewsRecycler.visibility = View.GONE
                        } else {
                            binding.emptyState.visibility = View.GONE
                            binding.reviewsRecycler.visibility = View.VISIBLE
                            reviewAdapter.submitList(reviews)
                        }

                        updateRatingOverview(reviews)
                    }
                }
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Error loading reviews: ${e.message}", Snackbar.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun updateRatingOverview(reviews: List<com.example.pickgo.models.rider.RiderReview>) {
        if (reviews.isEmpty()) {
            binding.avgRating.text = "0.0"
            binding.starRating.text = "☆☆☆☆☆"
            binding.totalReviews.text = "0 total reviews"
            return
        }

        val avgRating = reviews.map { it.rating }.average()
        binding.avgRating.text = String.format("%.1f", avgRating)
        binding.totalReviews.text = "${reviews.size} total reviews"

        // Update star display
        val fullStars = avgRating.toInt()
        val starText = StringBuilder()
        for (i in 1..5) {
            starText.append(if (i <= fullStars) "★" else "☆")
        }
        binding.starRating.text = starText.toString()

        // Update rating bars
        binding.ratingBars.removeAllViews()
        val ratingCounts = IntArray(5) { star ->
            reviews.count { it.rating == star + 1 }
        }

        for (star in 5 downTo 1) {
            val count = ratingCounts[star - 1]
            val percentage = (count.toDouble() / reviews.size) * 100

            val barRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 8 }
            }

            val label = TextView(this).apply {
                text = "$star ★"
                textSize = 12f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(getColor(R.color.text_secondary))
                layoutParams = LinearLayout.LayoutParams(50, LinearLayout.LayoutParams.WRAP_CONTENT)
            }
            barRow.addView(label)

            val barContainer = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    30,
                    1f
                ).apply { leftMargin = 8; rightMargin = 8 }
                background = getDrawable(R.drawable.bg_rating_bar_empty)
            }
            barRow.addView(barContainer)

            val fillBar = View(this).apply {
                layoutParams = android.widget.FrameLayout.LayoutParams(
                    (percentage * barContainer.width / 100).toInt(),
                    30
                )
                background = getDrawable(R.drawable.bg_rating_bar_fill)
            }
            (barContainer as? android.widget.FrameLayout)?.addView(fillBar)

            val countText = TextView(this).apply {
                text = count.toString()
                textSize = 12f
                setTextColor(getColor(R.color.text_secondary))
                layoutParams = LinearLayout.LayoutParams(40, LinearLayout.LayoutParams.WRAP_CONTENT)
            }
            barRow.addView(countText)

            binding.ratingBars.addView(barRow)
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