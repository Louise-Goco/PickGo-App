package com.example.pickgo.adapters.seller

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.pickgo.R
import com.example.pickgo.databinding.ItemSellerReviewBinding
import com.example.pickgo.models.seller.SellerReview
import java.text.SimpleDateFormat
import java.util.*

class ReviewAdapter : RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder>() {

    private var items: List<SellerReview> = emptyList()
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    fun submitList(newItems: List<SellerReview>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val binding = ItemSellerReviewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ReviewViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ReviewViewHolder(private val binding: ItemSellerReviewBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(review: SellerReview) {
            // Customer Name
            binding.customerName.text = review.customerName

            // Review Date
            binding.reviewDate.text = formatDate(review.createdAt)

            // Star Rating
            binding.starRating.text = generateStars(review.rating)

            // Comment
            if (review.comment.isNotEmpty()) {
                binding.reviewComment.text = review.comment
                binding.reviewComment.visibility = android.view.View.VISIBLE
            } else {
                binding.reviewComment.visibility = android.view.View.GONE
            }

            // Product Tag (if product review)
            if (!review.itemName.isNullOrEmpty()) {
                binding.productTag.text = review.itemName
                binding.productTag.visibility = android.view.View.VISIBLE
            } else {
                binding.productTag.visibility = android.view.View.GONE
            }
        }

        private fun generateStars(rating: Int): String {
            val stars = StringBuilder()
            for (i in 1..5) {
                stars.append(if (i <= rating) "★" else "☆")
            }
            return stars.toString()
        }

        private fun formatDate(dateString: String): String {
            return try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val date = inputFormat.parse(dateString)
                dateFormat.format(date ?: Date())
            } catch (e: Exception) {
                dateString
            }
        }
    }
}