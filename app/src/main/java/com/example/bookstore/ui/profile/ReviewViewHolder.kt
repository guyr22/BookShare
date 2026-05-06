package com.example.bookstore.ui.profile

import androidx.recyclerview.widget.RecyclerView
import com.example.bookstore.databinding.ItemReviewCardBinding

class ReviewViewHolder(
    private val binding: ItemReviewCardBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(review: Review) {
        binding.authorName.text = review.authorName
        binding.bookTitle.text = review.bookTitle
        binding.bookDescription.text = review.description
        binding.bookCover.setBackgroundResource(review.coverBackgroundRes)
    }
}
