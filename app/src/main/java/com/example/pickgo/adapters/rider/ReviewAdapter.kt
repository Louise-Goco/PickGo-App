package com.example.pickgo.adapters.rider

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.pickgo.R
import com.example.pickgo.databinding.ItemReviewBinding
import com.example.pickgo.models.rider.RiderReview
import java.text.SimpleDateFormat
import java.util.*

class ReviewAdapter : RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder>() {

    private var items: List<RiderReview> = emptyList()
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())

    fun submitList(newItems: List<RiderReview>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val binding = ItemReviewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ReviewViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ReviewViewHolder(private val binding: ItemReviewBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(review: RiderReview) {
            // Customer Initials
            binding.customerInitials.text = review.customerInitials.ifEmpty { "C" }

            // Customer Name
            binding.customerName.text = review.customerName

            // Review Date
            binding.reviewDate.text = formatDate(review.createdAt)

            // Star Rating
            binding.starRating.text = generateStars(review.rating)

            // Review Comment
            if (review.comment.isNotEmpty()) {
                binding.commentText.text = review.comment
                binding.commentText.visibility = android.view.View.VISIBLE
            } else {
                binding.commentText.visibility = android.view.View.GONE
            }

            // Order Badge
            binding.orderBadge.text = "Order #${review.orderId.take(8)}"
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