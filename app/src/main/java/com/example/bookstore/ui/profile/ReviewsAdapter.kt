package com.example.bookstore.ui.profile

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.bookstore.databinding.ItemReviewCardBinding

class ReviewsAdapter(
    var reviews: MutableList<Review>?
) : RecyclerView.Adapter<ReviewViewHolder>() {

    override fun getItemCount(): Int = reviews?.size ?: 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemReviewCardBinding.inflate(inflater, parent, false)
        return ReviewViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        reviews?.let { holder.bind(it[position]) }
    }
}
