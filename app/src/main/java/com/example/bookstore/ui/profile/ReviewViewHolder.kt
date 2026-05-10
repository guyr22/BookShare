package com.example.bookstore.ui.profile

import androidx.recyclerview.widget.RecyclerView
import com.example.bookstore.databinding.ItemReviewCardBinding
import com.example.bookstore.local.Book
import com.squareup.picasso.Picasso

class ReviewViewHolder(
    private val binding: ItemReviewCardBinding,
    private val listener: OnReviewClickListener?
) : RecyclerView.ViewHolder(binding.root) {

    private var current: Book? = null

    init {
        binding.root.setOnClickListener {
            current?.let { listener?.onReviewClick(it) }
        }
    }

    fun bind(book: Book, displayName: String?) {
        current = book
        binding.authorName.text = displayName ?: "You"
        binding.bookTitle.text = book.title
        binding.bookDescription.text =
            if (book.description.isNotBlank()) book.description else book.author

        if (book.coverUrl.isNotBlank()) {
            Picasso.get()
                .load(book.coverUrl)
                .fit()
                .centerCrop()
                .into(binding.bookCover)
        } else {
            binding.bookCover.setImageDrawable(null)
        }
    }
}
