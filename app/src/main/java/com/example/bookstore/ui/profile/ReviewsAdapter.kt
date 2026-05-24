package com.example.bookstore.ui.profile

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.bookstore.databinding.ItemReviewCardBinding
import com.example.bookstore.local.Book

class ReviewsAdapter(
    var items: MutableList<Book>?
) : RecyclerView.Adapter<ReviewViewHolder>() {

    var listener: OnReviewClickListener? = null

    /** Author name shown in every card header. Profile is "my reviews", so this is the current user. */
    var displayName: String? = null

    override fun getItemCount(): Int = items?.size ?: 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val binding = ItemReviewCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ReviewViewHolder(binding, listener)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        items?.get(position)?.let { holder.bind(it, displayName) }
    }

    fun submit(newItems: List<Book>) {
        items = newItems.toMutableList()
        notifyDataSetChanged()
    }
}

interface OnReviewClickListener {
    fun onReviewClick(book: Book)
}
